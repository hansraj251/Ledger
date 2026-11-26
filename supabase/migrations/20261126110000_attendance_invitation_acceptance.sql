-- Staff invitation and explicit acceptance workflow for Online Attendance.
-- Existing already-linked staff are preserved as accepted for backward compatibility.

alter table public.attendance_staff
    add column if not exists approval_status text not null default 'pending';

do $$
begin
    if not exists (
        select 1
          from pg_constraint
         where conname = 'attendance_staff_approval_status_valid'
           and conrelid = 'public.attendance_staff'::regclass
    ) then
        alter table public.attendance_staff
            add constraint attendance_staff_approval_status_valid
            check (approval_status in ('pending', 'accepted', 'declined'));
    end if;
end;
$$;

update public.attendance_staff
   set approval_status = 'accepted'
 where link_status = 'linked'
   and auth_user_id is not null
   and approval_status = 'pending';

create index if not exists attendance_staff_pending_invite_email_idx
    on public.attendance_staff(email_normalized, created_at desc)
    where is_active = true
      and link_status = 'pending'
      and approval_status = 'pending';

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

-- Login alone never accepts or links a pending invitation.
create or replace function public.claim_my_attendance_staff()
returns integer
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user_id uuid := auth.uid();
    v_email text;
    v_linked_count integer;
begin
    if v_user_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    select lower(btrim(u.email))
      into v_email
      from auth.users u
     where u.id = v_user_id
       and u.email_confirmed_at is not null;

    if v_email is null or v_email = '' then
        raise exception 'A verified Google account email is required'
            using errcode = '42501';
    end if;

    select count(*)
      into v_linked_count
      from public.attendance_staff s
     where s.email_normalized = v_email
       and s.auth_user_id = v_user_id
       and s.is_active = true
       and s.link_status = 'linked'
       and s.approval_status = 'accepted';

    return v_linked_count;
end;
$$;

create or replace function public.get_my_attendance_invites()
returns table (
    staff_id uuid,
    display_name text,
    owner_name text,
    owner_email text,
    created_at timestamptz
)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user_id uuid := auth.uid();
    v_email text;
begin
    if v_user_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    select lower(btrim(u.email))
      into v_email
      from auth.users u
     where u.id = v_user_id
       and u.email_confirmed_at is not null;

    if v_email is null or v_email = '' then
        raise exception 'A verified Google account email is required'
            using errcode = '42501';
    end if;

    return query
    select
        s.id,
        s.display_name,
        coalesce(
            nullif(btrim(owner_user.raw_user_meta_data ->> 'full_name'), ''),
            nullif(btrim(owner_user.raw_user_meta_data ->> 'name'), ''),
            owner_user.email,
            'Staff owner'
        )::text,
        coalesce(owner_user.email, '')::text,
        s.created_at
    from public.attendance_staff s
    join public.attendance_workplaces w
      on w.id = s.workplace_id
    join auth.users owner_user
      on owner_user.id = w.owner_id
    where s.email_normalized = v_email
      and s.is_active = true
      and s.link_status = 'pending'
      and s.approval_status = 'pending'
      and (s.auth_user_id is null or s.auth_user_id = v_user_id)
    order by s.created_at desc;
end;
$$;

create or replace function public.accept_my_attendance_invite(p_staff_id uuid)
returns integer
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user_id uuid := auth.uid();
    v_email text;
    v_accepted_count integer;
begin
    if v_user_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    select lower(btrim(u.email))
      into v_email
      from auth.users u
     where u.id = v_user_id
       and u.email_confirmed_at is not null;

    if v_email is null or v_email = '' then
        raise exception 'A verified Google account email is required'
            using errcode = '42501';
    end if;

    update public.attendance_staff s
       set auth_user_id = v_user_id,
           link_status = 'linked',
           approval_status = 'accepted',
           linked_at = coalesce(s.linked_at, now()),
           updated_at = now()
     where s.id = p_staff_id
       and s.email_normalized = v_email
       and s.is_active = true
       and s.link_status = 'pending'
       and s.approval_status = 'pending'
       and (s.auth_user_id is null or s.auth_user_id = v_user_id);

    get diagnostics v_accepted_count = row_count;

    if v_accepted_count = 0 then
        -- Make acceptance idempotent for the same verified account.
        select count(*)
          into v_accepted_count
          from public.attendance_staff s
         where s.id = p_staff_id
           and s.email_normalized = v_email
           and s.auth_user_id = v_user_id
           and s.link_status = 'linked'
           and s.approval_status = 'accepted'
           and s.is_active = true;
    end if;

    if v_accepted_count = 0 then
        raise exception 'No pending attendance invitation found for this Google account'
            using errcode = '42501';
    end if;

    return 1;
end;
$$;

-- Attendance can only be marked after explicit staff acceptance.
create or replace function public.mark_my_attendance(p_staff_id uuid)
returns setof public.attendance_records
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_staff public.attendance_staff%rowtype;
    v_timezone text;
    v_attendance_date date;
begin
    if auth.uid() is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    select s.*
      into v_staff
      from public.attendance_staff s
     where s.id = p_staff_id
       and s.auth_user_id = auth.uid()
       and s.link_status = 'linked'
       and s.approval_status = 'accepted'
       and s.is_active = true;

    if not found then
        raise exception 'No accepted active staff invitation found for this account'
            using errcode = '42501';
    end if;

    select w.timezone
      into v_timezone
      from public.attendance_workplaces w
     where w.id = v_staff.workplace_id;

    if v_timezone is null then
        raise exception 'Attendance workplace timezone is not configured'
            using errcode = '22023';
    end if;

    v_attendance_date := (now() at time zone v_timezone)::date;

    insert into public.attendance_records (
        staff_id,
        attendance_date,
        marked_by_user_id
    )
    values (
        p_staff_id,
        v_attendance_date,
        auth.uid()
    )
    on conflict (staff_id, attendance_date) do nothing;

    return query
    select r.*
      from public.attendance_records r
     where r.staff_id = p_staff_id
       and r.attendance_date = v_attendance_date;
end;
$$;

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

    select s.id
      into v_staff_id
      from public.attendance_staff s
      join public.attendance_workplaces w
        on w.id = s.workplace_id
     where s.id = p_staff_id
       and w.owner_id = v_owner_id
       and s.is_active = true
       and s.link_status = 'linked'
       and s.approval_status = 'accepted'
     for update of s;

    if not found then
        raise exception 'Accepted staff record not found or access denied'
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

-- Reapply the hardened privilege boundary to every replaced/new RPC.
revoke all on function public.register_attendance_staff(bigint, text, text)
    from public, anon, authenticated;
revoke all on function public.claim_my_attendance_staff()
    from public, anon, authenticated;
revoke all on function public.get_my_attendance_invites()
    from public, anon, authenticated;
revoke all on function public.accept_my_attendance_invite(uuid)
    from public, anon, authenticated;
revoke all on function public.mark_my_attendance(uuid)
    from public, anon, authenticated;
revoke all on function public.set_owner_attendance(uuid, date, text)
    from public, anon, authenticated;

grant execute on function public.register_attendance_staff(bigint, text, text)
    to authenticated;
grant execute on function public.claim_my_attendance_staff()
    to authenticated;
grant execute on function public.get_my_attendance_invites()
    to authenticated;
grant execute on function public.accept_my_attendance_invite(uuid)
    to authenticated;
grant execute on function public.mark_my_attendance(uuid)
    to authenticated;
grant execute on function public.set_owner_attendance(uuid, date, text)
    to authenticated;
