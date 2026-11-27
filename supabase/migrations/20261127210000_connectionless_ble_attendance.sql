-- Connectionless BLE beacon attendance.
-- Session secrets are readable only by the Supabase service role.
create table if not exists public.attendance_ble_beacon_sessions (
    workplace_id uuid primary key
        references public.attendance_workplaces(id) on delete cascade,
    secret_base64 text not null,
    expires_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint attendance_ble_beacon_secret_length
        check (length(secret_base64) between 40 and 64)
);

alter table public.attendance_ble_beacon_sessions enable row level security;
revoke all on table public.attendance_ble_beacon_sessions from public, anon, authenticated;
grant select, insert, update, delete on table public.attendance_ble_beacon_sessions to service_role;

create or replace function public.record_ble_beacon_attendance(p_staff_id uuid)
returns setof public.attendance_records
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_staff public.attendance_staff%rowtype;
    v_workplace public.attendance_workplaces%rowtype;
    v_today date;
    v_record public.attendance_records%rowtype;
    v_now timestamptz := now();
begin
    select * into v_staff
      from public.attendance_staff
     where id = p_staff_id
     for update;

    if not found or not v_staff.is_active
       or v_staff.link_status <> 'linked'
       or v_staff.approval_status <> 'accepted'
       or v_staff.auth_user_id is null then
        raise exception 'Staff registration is not active' using errcode = '42501';
    end if;

    select * into v_workplace
      from public.attendance_workplaces
     where id = v_staff.workplace_id;

    if not found or v_workplace.attendance_verification_mode <> 'bluetooth_auto' then
        raise exception 'Bluetooth attendance is not enabled' using errcode = '42501';
    end if;

    if v_workplace.timezone is null or not exists (
        select 1 from pg_timezone_names where name = v_workplace.timezone
    ) then
        raise exception 'Invalid workplace timezone configuration' using errcode = '22023';
    end if;

    v_today := (v_now at time zone v_workplace.timezone)::date;

    if exists (
        select 1 from public.attendance_records
         where staff_id = v_staff.id and attendance_date = v_today
    ) then
        raise exception 'Attendance is already recorded for today' using errcode = '23505';
    end if;

    insert into public.attendance_records
        (staff_id, attendance_date, marked_by_user_id, marked_at, status)
    values
        (v_staff.id, v_today, v_staff.auth_user_id, v_now, 'present')
    returning * into v_record;

    update public.attendance_mark_requests
       set status = 'cancelled', reviewed_at = v_now
     where staff_id = v_staff.id
       and attendance_date = v_today
       and status = 'pending';

    return next v_record;
exception
    when unique_violation then
        raise exception 'Attendance is already recorded for today' using errcode = '23505';
end;
$$;

revoke all on function public.record_ble_beacon_attendance(uuid) from public, anon, authenticated;
grant execute on function public.record_ble_beacon_attendance(uuid) to service_role;
