-- Owner can permanently remove an online attendance registration and all its
-- attendance history. Local Room staff data is not touched by this RPC.
create or replace function public.delete_owner_attendance_staff(
    p_staff_id uuid
)
returns integer
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

    if p_staff_id is null then
        raise exception 'Staff ID is required' using errcode = '22023';
    end if;

    -- Lock and verify ownership before deleting any cloud data.
    select s.id
      into v_staff_id
      from public.attendance_staff s
      join public.attendance_workplaces w
        on w.id = s.workplace_id
     where s.id = p_staff_id
       and w.owner_id = v_owner_id
     for update of s;

    if not found then
        raise exception 'Online attendance staff record not found or access denied'
            using errcode = '42501';
    end if;

    -- Explicitly remove attendance history, then the cloud staff/invitation row.
    delete from public.attendance_records
     where staff_id = v_staff_id;

    delete from public.attendance_staff
     where id = v_staff_id;

    return 1;
end;
$$;

revoke all on function public.delete_owner_attendance_staff(uuid)
    from public, anon, authenticated;
grant execute on function public.delete_owner_attendance_staff(uuid)
    to authenticated;
