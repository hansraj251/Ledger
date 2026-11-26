-- Prevent an owner from registering their own Ledger Online account as attendance staff.
-- This replaces the existing RPC in-place and preserves the invitation workflow.

create or replace function public.register_attendance_staff(
    p_local_staff_id bigint,
    p_display_name text,
    p_email text
)
returns uuid
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_owner_id uuid := auth.uid();
    v_owner_email text;
    v_workplace_id uuid;
    v_staff_id uuid;
    v_match_count integer;
    v_email text := lower(btrim(coalesce(p_email, '')));
begin
    if v_owner_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    if p_local_staff_id is null or p_local_staff_id <= 0 then
        raise exception 'A valid local staff ID is required' using errcode = '22023';
    end if;

    if coalesce(length(btrim(p_display_name)), 0) = 0 then
        raise exception 'Staff name is required' using errcode = '22023';
    end if;

    if v_email = '' or position('@' in v_email) <= 1 then
        raise exception 'A valid staff email is required' using errcode = '22023';
    end if;

    -- Use the authenticated Supabase account as the source of truth. This
    -- prevents an owner from registering their own Ledger Online Gmail as staff,
    -- even if a client-side validation is bypassed.
    select lower(btrim(u.email))
      into v_owner_email
      from auth.users u
     where u.id = v_owner_id;

    if v_owner_email is not null and v_email = v_owner_email then
        raise exception 'You cannot add your own Ledger Online Gmail as staff. Enter the staff member''s Gmail.'
            using errcode = '22023';
    end if;

    insert into public.attendance_workplaces (owner_id)
    values (v_owner_id)
    on conflict (owner_id) do update
        set updated_at = now()
    returning id into v_workplace_id;

    select count(*)
      into v_match_count
      from public.attendance_staff s
     where s.workplace_id = v_workplace_id
       and (
           s.local_staff_id = p_local_staff_id
           or s.email_normalized = v_email
       );

    if v_match_count > 1 then
        raise exception 'This email is already assigned to another staff record'
            using errcode = '23505';
    elsif v_match_count = 1 then
        select s.id
          into v_staff_id
          from public.attendance_staff s
         where s.workplace_id = v_workplace_id
           and (
               s.local_staff_id = p_local_staff_id
               or s.email_normalized = v_email
           )
         limit 1
         for update;

        update public.attendance_staff s
           set local_staff_id = p_local_staff_id,
               display_name = btrim(p_display_name),
               email = btrim(p_email),
               email_normalized = v_email,
               auth_user_id = case
                   when s.email_normalized <> v_email
                     or s.link_status = 'disabled'
                     or (s.auth_user_id is null and s.approval_status = 'accepted')
                     or s.approval_status = 'declined'
                       then null
                   else s.auth_user_id
               end,
               link_status = case
                   when s.email_normalized <> v_email
                     or s.link_status = 'disabled'
                     or (s.auth_user_id is null and s.approval_status = 'accepted')
                     or s.approval_status = 'declined'
                       then 'pending'
                   else s.link_status
               end,
               approval_status = case
                   when s.email_normalized <> v_email
                     or s.link_status = 'disabled'
                     or (s.auth_user_id is null and s.approval_status = 'accepted')
                     or s.approval_status = 'declined'
                       then 'pending'
                   else s.approval_status
               end,
               linked_at = case
                   when s.email_normalized <> v_email
                     or s.link_status = 'disabled'
                     or (s.auth_user_id is null and s.approval_status = 'accepted')
                     or s.approval_status = 'declined'
                       then null
                   else s.linked_at
               end,
               is_active = true,
               updated_at = now()
         where s.id = v_staff_id;
    else
        insert into public.attendance_staff (
            workplace_id,
            local_staff_id,
            display_name,
            email,
            email_normalized,
            approval_status
        )
        values (
            v_workplace_id,
            p_local_staff_id,
            btrim(p_display_name),
            btrim(p_email),
            v_email,
            'pending'
        )
        returning id into v_staff_id;
    end if;

    return v_staff_id;
end;
$$;

revoke all on function public.register_attendance_staff(bigint, text, text)
    from public, anon, authenticated;
grant execute on function public.register_attendance_staff(bigint, text, text)
    to authenticated;
