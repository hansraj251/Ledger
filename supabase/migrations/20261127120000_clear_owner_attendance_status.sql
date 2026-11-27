-- Allow a workplace owner to clear an attendance mark for a selected date.
-- Clearing removes the row; it does not introduce a third status value.

create or replace function public.clear_owner_attendance(
    p_staff_id uuid,
    p_attendance_date date
)
returns integer
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_owner_id uuid := auth.uid();
    v_staff_id uuid;
    v_deleted integer := 0;
begin
    if v_owner_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    if p_staff_id is null or p_attendance_date is null then
        raise exception 'Staff and attendance date are required'
            using errcode = '22023';
    end if;

    -- Verify ownership before deleting any attendance row.
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

    delete from public.attendance_records r
     where r.staff_id = v_staff_id
       and r.attendance_date = p_attendance_date;

    get diagnostics v_deleted = row_count;
    return v_deleted;
end;
$$;

revoke all on function public.clear_owner_attendance(uuid, date)
    from public, anon, authenticated;
grant execute on function public.clear_owner_attendance(uuid, date)
    to authenticated;

comment on function public.clear_owner_attendance(uuid, date) is
    'Allows a workplace owner to remove the attendance mark for one staff member and date.';
