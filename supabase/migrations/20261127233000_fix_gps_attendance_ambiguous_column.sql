-- Fix PL/pgSQL ambiguity between RETURNS TABLE output variable
-- attendance_date and table columns used in GPS attendance statements.
-- Also retain the strict NULL mock-location validation.

CREATE OR REPLACE FUNCTION public.submit_my_gps_attendance(
    p_staff_id uuid,
    p_latitude double precision,
    p_longitude double precision,
    p_accuracy_m double precision,
    p_location_status text,
    p_is_mock boolean
)
RETURNS TABLE (
    result text,
    request_id uuid,
    attendance_date date,
    distance_m double precision,
    radius_m integer,
    accuracy_m double precision,
    reason text
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
#variable_conflict use_column

DECLARE
    v_user_id uuid := auth.uid();
    v_staff public.attendance_staff%rowtype;
    v_workplace public.attendance_workplaces%rowtype;
    v_today date;
    v_distance double precision;
    v_a double precision;
    v_reason text;
    v_request_id uuid;
    v_now timestamptz := now();
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required'
            USING errcode = '28000';
    END IF;

    IF p_location_status IS NULL
       OR p_location_status NOT IN (
           'captured',
           'permission_denied',
           'location_unavailable'
       ) THEN
        RAISE EXCEPTION 'Invalid GPS location status'
            USING errcode = '22023';
    END IF;

    -- Fail closed if the client omits mock-location verification.
    IF p_is_mock IS NULL THEN
        RAISE EXCEPTION 'Mock location verification is required'
            USING errcode = '22023';
    END IF;

    SELECT s.*
      INTO v_staff
      FROM public.attendance_staff s
     WHERE s.id = p_staff_id
       AND s.auth_user_id = v_user_id
       AND s.link_status = 'linked'
       AND s.approval_status = 'accepted'
       AND s.is_active = true
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'No accepted active staff registration found for this account'
            USING errcode = '42501';
    END IF;

    SELECT w.*
      INTO v_workplace
      FROM public.attendance_workplaces w
     WHERE w.id = v_staff.workplace_id;

    IF NOT FOUND
       OR v_workplace.attendance_verification_mode <> 'gps_auto'
       OR v_workplace.gps_latitude IS NULL
       OR v_workplace.gps_longitude IS NULL THEN
        RAISE EXCEPTION
            'GPS attendance is not configured for this workplace'
            USING errcode = '42501';
    END IF;

    IF v_workplace.timezone IS NULL
       OR NOT EXISTS (
           SELECT 1
             FROM pg_timezone_names
            WHERE name = v_workplace.timezone
       ) THEN
        RAISE EXCEPTION 'Invalid workplace timezone configuration'
            USING errcode = '22023';
    END IF;

    v_today := (v_now AT TIME ZONE v_workplace.timezone)::date;

    IF EXISTS (
        SELECT 1
          FROM public.attendance_records r
         WHERE r.staff_id = v_staff.id
           AND r.attendance_date = v_today
    ) THEN
        RAISE EXCEPTION 'Attendance is already recorded for today'
            USING errcode = '23505';
    END IF;

    IF p_location_status = 'captured' THEN
        IF p_latitude IS NULL
           OR p_longitude IS NULL
           OR p_accuracy_m IS NULL
           OR p_latitude NOT BETWEEN -90 AND 90
           OR p_longitude NOT BETWEEN -180 AND 180
           OR p_accuracy_m <= 0
           OR p_accuracy_m > 10000 THEN
            RAISE EXCEPTION 'Invalid GPS coordinates or accuracy'
                USING errcode = '22023';
        END IF;

        v_a :=
            power(sin(radians(
                p_latitude - v_workplace.gps_latitude
            ) / 2), 2)
            + cos(radians(v_workplace.gps_latitude))
            * cos(radians(p_latitude))
            * power(sin(radians(
                p_longitude - v_workplace.gps_longitude
            ) / 2), 2);

        v_distance :=
            6371000 * 2 * asin(
                least(1, sqrt(greatest(0, v_a)))
            );
    ELSE
        IF p_latitude IS NOT NULL
           OR p_longitude IS NOT NULL
           OR p_accuracy_m IS NOT NULL THEN
            RAISE EXCEPTION
                'Coordinates must be omitted when GPS is unavailable'
                USING errcode = '22023';
        END IF;

        v_distance := NULL;
    END IF;

    -- Automatic Present only when the complete accuracy circle
    -- is inside the geofence and mock verification is explicitly false.
    IF p_location_status = 'captured'
       AND p_is_mock IS FALSE
       AND p_accuracy_m <= 50
       AND (v_distance + p_accuracy_m)
           <= v_workplace.attendance_radius_m THEN

        INSERT INTO public.attendance_records
            (staff_id, attendance_date, marked_by_user_id, marked_at, status)
        VALUES
            (v_staff.id, v_today, v_user_id, v_now, 'present');

        UPDATE public.attendance_mark_requests
           SET status = 'cancelled',
               reviewed_at = v_now
         WHERE staff_id = v_staff.id
           AND attendance_date = v_today
           AND status = 'pending';

        RETURN QUERY
        SELECT
            'present'::text,
            NULL::uuid,
            v_today,
            v_distance,
            v_workplace.attendance_radius_m,
            p_accuracy_m,
            NULL::text;

        RETURN;
    END IF;

    v_reason := CASE
        WHEN p_is_mock IS TRUE
            THEN 'mock_location_detected'
        WHEN p_location_status = 'permission_denied'
            THEN 'location_permission_denied'
        WHEN p_location_status = 'location_unavailable'
            THEN 'location_unavailable'
        WHEN p_accuracy_m > 50
            THEN 'low_accuracy'
        WHEN v_distance > v_workplace.attendance_radius_m
            THEN 'outside_geofence'
        ELSE 'near_boundary'
    END;

    INSERT INTO public.attendance_mark_requests (
        staff_id,
        attendance_date,
        requested_by_user_id,
        requested_at,
        status,
        location_latitude,
        location_longitude,
        location_accuracy_m,
        location_distance_m,
        location_radius_m,
        location_reason
    )
    VALUES (
        v_staff.id,
        v_today,
        v_user_id,
        v_now,
        'pending',
        p_latitude,
        p_longitude,
        p_accuracy_m,
        v_distance,
        v_workplace.attendance_radius_m,
        v_reason
    )
    ON CONFLICT (staff_id, attendance_date)
        WHERE status = 'pending'
    DO UPDATE SET
        requested_at = EXCLUDED.requested_at,
        location_latitude = EXCLUDED.location_latitude,
        location_longitude = EXCLUDED.location_longitude,
        location_accuracy_m = EXCLUDED.location_accuracy_m,
        location_distance_m = EXCLUDED.location_distance_m,
        location_radius_m = EXCLUDED.location_radius_m,
        location_reason = EXCLUDED.location_reason
    RETURNING id INTO v_request_id;

    RETURN QUERY
    SELECT
        'approval_pending'::text,
        v_request_id,
        v_today,
        v_distance,
        v_workplace.attendance_radius_m,
        p_accuracy_m,
        v_reason;
END;
$$;

-- Preserve authenticated app access; do not expose this RPC to anon/PUBLIC.
REVOKE ALL ON FUNCTION public.submit_my_gps_attendance(
    uuid, double precision, double precision, double precision, text, boolean
) FROM PUBLIC, anon;

GRANT EXECUTE ON FUNCTION public.submit_my_gps_attendance(
    uuid, double precision, double precision, double precision, text, boolean
) TO authenticated;

-- Keep service-role execution available for trusted backend operations.
GRANT EXECUTE ON FUNCTION public.submit_my_gps_attendance(
    uuid, double precision, double precision, double precision, text, boolean
) TO service_role;
