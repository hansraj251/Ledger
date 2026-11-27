-- Remove Bluetooth attendance while retaining GPS Geofence and Owner Approval.
-- Legacy Bluetooth workplaces are safely moved to Owner Approval first.

BEGIN;

UPDATE public.attendance_workplaces
   SET attendance_verification_mode = 'owner_approval',
       owner_ble_public_key = NULL,
       updated_at = now()
 WHERE attendance_verification_mode = 'bluetooth_auto';

ALTER TABLE public.attendance_workplaces
    DROP CONSTRAINT IF EXISTS attendance_workplaces_verification_mode_check;
ALTER TABLE public.attendance_workplaces
    ADD CONSTRAINT attendance_workplaces_verification_mode_check
    CHECK (attendance_verification_mode IN ('owner_approval', 'gps_auto'));

CREATE OR REPLACE FUNCTION public.set_owner_attendance_verification_mode(p_mode text)
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_owner_id uuid := auth.uid();
    v_updated integer := 0;
BEGIN
    IF v_owner_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required' USING errcode = '28000';
    END IF;
    IF p_mode IS NULL OR p_mode NOT IN ('owner_approval', 'gps_auto') THEN
        RAISE EXCEPTION 'Unsupported attendance verification mode' USING errcode = '22023';
    END IF;
    IF p_mode = 'gps_auto' AND NOT EXISTS (
        SELECT 1 FROM public.attendance_workplaces
         WHERE owner_id = v_owner_id
           AND gps_latitude IS NOT NULL
           AND gps_longitude IS NOT NULL
           AND attendance_radius_m BETWEEN 50 AND 300
    ) THEN
        RAISE EXCEPTION 'Set the workplace GPS location and radius first' USING errcode = '22023';
    END IF;
    UPDATE public.attendance_workplaces
       SET attendance_verification_mode = p_mode,
           updated_at = now()
     WHERE owner_id = v_owner_id;
    GET DIAGNOSTICS v_updated = ROW_COUNT;
    IF v_updated = 0 THEN
        RAISE EXCEPTION 'Attendance workplace not found' USING errcode = 'P0002';
    END IF;
    RETURN v_updated;
END;
$$;

CREATE OR REPLACE FUNCTION public.request_my_attendance(p_staff_id uuid)
RETURNS SETOF public.attendance_mark_requests
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_user_id uuid := auth.uid();
    v_staff public.attendance_staff%ROWTYPE;
    v_timezone text;
    v_mode text;
    v_date date;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required' USING errcode = '28000';
    END IF;
    SELECT s.* INTO v_staff
      FROM public.attendance_staff s
     WHERE s.id = p_staff_id
       AND s.auth_user_id = v_user_id
       AND s.link_status = 'linked'
       AND s.approval_status = 'accepted'
       AND s.is_active = true;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No accepted active staff registration found for this account' USING errcode = '42501';
    END IF;
    SELECT w.timezone, w.attendance_verification_mode
      INTO v_timezone, v_mode
      FROM public.attendance_workplaces w
     WHERE w.id = v_staff.workplace_id;
    IF v_mode <> 'owner_approval' THEN
        RAISE EXCEPTION 'Attendance approval requests are available only in Owner Approval mode' USING errcode = '42501';
    END IF;
    IF v_timezone IS NULL OR NOT EXISTS (
        SELECT 1 FROM pg_timezone_names WHERE name = v_timezone
    ) THEN
        RAISE EXCEPTION 'Invalid workplace timezone configuration' USING errcode = '22023';
    END IF;
    v_date := (now() AT TIME ZONE v_timezone)::date;
    IF EXISTS (
        SELECT 1 FROM public.attendance_records r
         WHERE r.staff_id = p_staff_id AND r.attendance_date = v_date
    ) THEN
        RAISE EXCEPTION 'Attendance is already recorded for today' USING errcode = '23505';
    END IF;
    RETURN QUERY
        SELECT r.* FROM public.attendance_mark_requests r
         WHERE r.staff_id = p_staff_id
           AND r.attendance_date = v_date
           AND r.status = 'pending';
    IF FOUND THEN RETURN; END IF;
    INSERT INTO public.attendance_mark_requests
        (staff_id, attendance_date, requested_by_user_id, status)
    VALUES (p_staff_id, v_date, v_user_id, 'pending')
    ON CONFLICT (staff_id, attendance_date) WHERE status = 'pending' DO NOTHING;
    RETURN QUERY
        SELECT r.* FROM public.attendance_mark_requests r
         WHERE r.staff_id = p_staff_id
           AND r.attendance_date = v_date
           AND r.status = 'pending';
END;
$$;

REVOKE ALL ON FUNCTION public.set_owner_attendance_verification_mode(text) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.set_owner_attendance_verification_mode(text) TO authenticated;
REVOKE ALL ON FUNCTION public.request_my_attendance(uuid) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.request_my_attendance(uuid) TO authenticated;

DROP FUNCTION IF EXISTS public.register_owner_ble_public_key(text);
DROP FUNCTION IF EXISTS public.create_my_ble_attendance_challenge(uuid);
DROP FUNCTION IF EXISTS public.consume_ble_attendance_challenge(uuid);
DROP FUNCTION IF EXISTS public.record_ble_beacon_attendance(uuid);

DROP TABLE IF EXISTS public.attendance_ble_challenges;
DROP TABLE IF EXISTS public.attendance_ble_beacon_sessions;

ALTER TABLE public.attendance_workplaces
    DROP COLUMN IF EXISTS owner_ble_public_key;

COMMIT;
