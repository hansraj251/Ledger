-- Owner-managed Present/Absent status for any selected calendar date.
-- Additive migration: preserves existing attendance rows and staff self-marking RPC.

alter table public.attendance_records
    add column if not exists status text not null default 'present';

alter table public.attendance_records
    drop constraint if exists attendance_records_status_check;

alter table public.attendance_records
    add constraint attendance_records_status_check
    check (status in ('present', 'absent'));

create or replace function public.set_owner_attendance(
    p_staff_id uuid,
    p_attendance_date date,
    p_status text
)
returns setof public.attendance_records
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_owner_id uuid := auth.uid();
    v_staff_id uuid;
begin
    if v_owner_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    if p_staff_id is null or p_attendance_date is null then
        raise exception 'Staff and attendance date are required'
            using errcode = '22023';
    end if;

    if p_status is null or lower(btrim(p_status)) not in ('present', 'absent') then
        raise exception 'Status must be Present or Absent'
            using errcode = '22023';
    end if;

    -- Verify that the signed-in user owns this staff member's workplace.
    select s.id
      into v_staff_id
      from public.attendance_staff s
      join public.attendance_workplaces w
        on w.id = s.workplace_id
     where s.id = p_staff_id
       and w.owner_id = v_owner_id
     for update of s;

    if not found then
        raise exception 'Staff record not found or access denied'
            using errcode = '42501';
    end if;

    insert into public.attendance_records (
        staff_id,
        attendance_date,
        marked_by_user_id,
        marked_at,
        status
    )
    values (
        v_staff_id,
        p_attendance_date,
        v_owner_id,
        now(),
        lower(btrim(p_status))
    )
    on conflict (staff_id, attendance_date)
    do update set
        status = excluded.status,
        marked_by_user_id = excluded.marked_by_user_id,
        marked_at = excluded.marked_at;

    return query
    select r.*
      from public.attendance_records r
     where r.staff_id = v_staff_id
       and r.attendance_date = p_attendance_date;
end;
$$;

revoke all on function public.set_owner_attendance(uuid, date, text)
    from public, anon, authenticated;
grant execute on function public.set_owner_attendance(uuid, date, text)
    to authenticated;

comment on table public.attendance_records is
    'Attendance records with Present/Absent status; staff can self-mark today and owners can manage any workplace date.';

comment on column public.attendance_records.status is
    'Attendance status set by the staff self-mark RPC (Present) or owner-managed Present/Absent action.';
