-- GPS geofence attendance with Owner Approval fallback.
-- Adds a separate gps_auto mode; existing Owner Approval and BLE modes remain intact.

alter table public.attendance_workplaces
    add column if not exists gps_latitude double precision,
    add column if not exists gps_longitude double precision,
    add column if not exists attendance_radius_m integer not null default 200;

do $$
begin
    if not exists (
        select 1 from pg_constraint
         where conname = 'attendance_workplaces_gps_coordinates_pair'
           and conrelid = 'public.attendance_workplaces'::regclass
    ) then
        alter table public.attendance_workplaces
            add constraint attendance_workplaces_gps_coordinates_pair
            check (
                (gps_latitude is null and gps_longitude is null)
                or (
                    gps_latitude is not null and gps_longitude is not null
                    and gps_latitude between -90 and 90
                    and gps_longitude between -180 and 180
                )
            );
    end if;

    if not exists (
        select 1 from pg_constraint
         where conname = 'attendance_workplaces_radius_range'
           and conrelid = 'public.attendance_workplaces'::regclass
    ) then
        alter table public.attendance_workplaces
            add constraint attendance_workplaces_radius_range
            check (attendance_radius_m between 50 and 300);
    end if;
end;
$$;

alter table public.attendance_workplaces
    drop constraint if exists attendance_workplaces_verification_mode_check;
alter table public.attendance_workplaces
    add constraint attendance_workplaces_verification_mode_check
    check (attendance_verification_mode in ('owner_approval', 'bluetooth_auto', 'gps_auto'));

alter table public.attendance_mark_requests
    add column if not exists location_latitude double precision,
    add column if not exists location_longitude double precision,
    add column if not exists location_accuracy_m double precision,
    add column if not exists location_distance_m double precision,
    add column if not exists location_radius_m integer,
    add column if not exists location_reason text;

do $$
begin
    if not exists (
        select 1 from pg_constraint
         where conname = 'attendance_mark_requests_location_latitude_range'
           and conrelid = 'public.attendance_mark_requests'::regclass
    ) then
        alter table public.attendance_mark_requests
            add constraint attendance_mark_requests_location_latitude_range
            check (location_latitude is null or location_latitude between -90 and 90);
    end if;
    if not exists (
        select 1 from pg_constraint
         where conname = 'attendance_mark_requests_location_longitude_range'
           and conrelid = 'public.attendance_mark_requests'::regclass
    ) then
        alter table public.attendance_mark_requests
            add constraint attendance_mark_requests_location_longitude_range
            check (location_longitude is null or location_longitude between -180 and 180);
    end if;
end;
$$;

create or replace function public.set_owner_gps_settings(
    p_latitude double precision,
    p_longitude double precision,
    p_radius_m integer
)
returns integer
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_owner_id uuid := auth.uid();
    v_updated integer := 0;
begin
    if v_owner_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;
    if p_latitude is null or p_longitude is null
       or p_latitude not between -90 and 90
       or p_longitude not between -180 and 180 then
        raise exception 'Valid workplace GPS coordinates are required' using errcode = '22023';
    end if;
    if p_radius_m is null or p_radius_m not between 50 and 300 then
        raise exception 'Attendance radius must be between 50 and 300 metres' using errcode = '22023';
    end if;

    update public.attendance_workplaces
       set gps_latitude = p_latitude,
           gps_longitude = p_longitude,
           attendance_radius_m = p_radius_m,
           updated_at = now()
     where owner_id = v_owner_id;
    get diagnostics v_updated = row_count;
    if v_updated = 0 then
        raise exception 'Attendance workplace not found' using errcode = 'P0002';
    end if;
    return v_updated;
end;
$$;

create or replace function public.set_owner_attendance_verification_mode(p_mode text)
returns integer
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_owner_id uuid := auth.uid();
    v_updated integer := 0;
begin
    if v_owner_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;
    if p_mode not in ('owner_approval', 'bluetooth_auto', 'gps_auto') then
        raise exception 'Unsupported attendance verification mode' using errcode = '22023';
    end if;
    if p_mode = 'bluetooth_auto' and not exists (
        select 1 from public.attendance_workplaces
         where owner_id = v_owner_id
           and owner_ble_public_key is not null
    ) then
        raise exception 'Register this device for Bluetooth verification first' using errcode = '22023';
    end if;
    if p_mode = 'gps_auto' and not exists (
        select 1 from public.attendance_workplaces
         where owner_id = v_owner_id
           and gps_latitude is not null
           and gps_longitude is not null
           and attendance_radius_m between 50 and 300
    ) then
        raise exception 'Set the workplace GPS location and radius first' using errcode = '22023';
    end if;

    update public.attendance_workplaces
       set attendance_verification_mode = p_mode,
           updated_at = now()
     where owner_id = v_owner_id;
    get diagnostics v_updated = row_count;
    if v_updated = 0 then
        raise exception 'Attendance workplace not found' using errcode = 'P0002';
    end if;
    return v_updated;
end;
$$;

create or replace function public.submit_my_gps_attendance(
    p_staff_id uuid,
    p_latitude double precision,
    p_longitude double precision,
    p_accuracy_m double precision,
    p_location_status text,
    p_is_mock boolean
)
returns table (
    result text,
    request_id uuid,
    attendance_date date,
    distance_m double precision,
    radius_m integer,
    accuracy_m double precision,
    reason text
)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user_id uuid := auth.uid();
    v_staff public.attendance_staff%rowtype;
    v_workplace public.attendance_workplaces%rowtype;
    v_today date;
    v_distance double precision;
    v_a double precision;
    v_reason text;
    v_request_id uuid;
    v_now timestamptz := now();
begin
    if v_user_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    if p_location_status is null
       or p_location_status not in ('captured', 'permission_denied', 'location_unavailable') then
        raise exception 'Invalid GPS location status' using errcode = '22023';
    end if;

    select s.* into v_staff
      from public.attendance_staff s
     where s.id = p_staff_id
       and s.auth_user_id = v_user_id
       and s.link_status = 'linked'
       and s.approval_status = 'accepted'
       and s.is_active = true
     for update;
    if not found then
        raise exception 'No accepted active staff registration found for this account'
            using errcode = '42501';
    end if;

    select w.* into v_workplace
      from public.attendance_workplaces w
     where w.id = v_staff.workplace_id;
    if not found or v_workplace.attendance_verification_mode <> 'gps_auto'
       or v_workplace.gps_latitude is null or v_workplace.gps_longitude is null then
        raise exception 'GPS attendance is not configured for this workplace'
            using errcode = '42501';
    end if;

    if v_workplace.timezone is null or not exists (
        select 1 from pg_timezone_names where name = v_workplace.timezone
    ) then
        raise exception 'Invalid workplace timezone configuration' using errcode = '22023';
    end if;
    v_today := (v_now at time zone v_workplace.timezone)::date;

    if exists (
        select 1 from public.attendance_records r
         where r.staff_id = v_staff.id and r.attendance_date = v_today
    ) then
        raise exception 'Attendance is already recorded for today' using errcode = '23505';
    end if;

    if p_location_status = 'captured' then
        if p_latitude is null or p_longitude is null or p_accuracy_m is null
           or p_latitude not between -90 and 90
           or p_longitude not between -180 and 180
           or p_accuracy_m <= 0 or p_accuracy_m > 10000 then
            raise exception 'Invalid GPS coordinates or accuracy' using errcode = '22023';
        end if;

        v_a :=
            power(sin(radians(p_latitude - v_workplace.gps_latitude) / 2), 2)
            + cos(radians(v_workplace.gps_latitude))
            * cos(radians(p_latitude))
            * power(sin(radians(p_longitude - v_workplace.gps_longitude) / 2), 2);
        v_distance := 6371000 * 2 * asin(least(1, sqrt(greatest(0, v_a))));
    else
        if p_latitude is not null or p_longitude is not null or p_accuracy_m is not null then
            raise exception 'Coordinates must be omitted when GPS is unavailable' using errcode = '22023';
        end if;
        v_distance := null;
    end if;

    -- Automatic Present only when the complete accuracy circle is inside the
    -- geofence. Uncertain, outside, denied, unavailable or mock locations go
    -- to the existing Owner Approval workflow.
    if p_location_status = 'captured'
       and not coalesce(p_is_mock, false)
       and p_accuracy_m <= 50
       and (v_distance + p_accuracy_m) <= v_workplace.attendance_radius_m then

        insert into public.attendance_records
            (staff_id, attendance_date, marked_by_user_id, marked_at, status)
        values
            (v_staff.id, v_today, v_user_id, v_now, 'present');

        update public.attendance_mark_requests
           set status = 'cancelled', reviewed_at = v_now
         where staff_id = v_staff.id
           and attendance_date = v_today
           and status = 'pending';

        return query select
            'present'::text, null::uuid, v_today, v_distance,
            v_workplace.attendance_radius_m, p_accuracy_m, null::text;
        return;
    end if;

    v_reason := case
        when coalesce(p_is_mock, false) then 'mock_location_detected'
        when p_location_status = 'permission_denied' then 'location_permission_denied'
        when p_location_status = 'location_unavailable' then 'location_unavailable'
        when p_accuracy_m > 50 then 'low_accuracy'
        when v_distance > v_workplace.attendance_radius_m then 'outside_geofence'
        else 'near_boundary'
    end;

    insert into public.attendance_mark_requests (
        staff_id, attendance_date, requested_by_user_id, requested_at, status,
        location_latitude, location_longitude, location_accuracy_m,
        location_distance_m, location_radius_m, location_reason
    )
    values (
        v_staff.id, v_today, v_user_id, v_now, 'pending',
        p_latitude, p_longitude, p_accuracy_m,
        v_distance, v_workplace.attendance_radius_m, v_reason
    )
    on conflict (staff_id, attendance_date) where status = 'pending'
    do update set
        requested_at = excluded.requested_at,
        location_latitude = excluded.location_latitude,
        location_longitude = excluded.location_longitude,
        location_accuracy_m = excluded.location_accuracy_m,
        location_distance_m = excluded.location_distance_m,
        location_radius_m = excluded.location_radius_m,
        location_reason = excluded.location_reason
    returning id into v_request_id;

    return query select
        'approval_pending'::text, v_request_id, v_today, v_distance,
        v_workplace.attendance_radius_m, p_accuracy_m, v_reason;
end;
$$;

revoke all on function public.set_owner_gps_settings(double precision, double precision, integer)
    from public, anon, authenticated;
revoke all on function public.submit_my_gps_attendance(uuid, double precision, double precision, double precision, text, boolean)
    from public, anon, authenticated;
grant execute on function public.set_owner_gps_settings(double precision, double precision, integer)
    to authenticated;
grant execute on function public.submit_my_gps_attendance(uuid, double precision, double precision, double precision, text, boolean)
    to authenticated;

comment on column public.attendance_workplaces.gps_latitude is
    'Owner-configured workplace latitude for GPS geofence attendance.';
comment on column public.attendance_workplaces.gps_longitude is
    'Owner-configured workplace longitude for GPS geofence attendance.';
comment on column public.attendance_workplaces.attendance_radius_m is
    'Owner-configured GPS attendance radius, constrained to 50–300 metres.';
comment on column public.attendance_mark_requests.location_reason is
    'Why a GPS attendance attempt was routed to Owner Approval.';
