-- Owner-approval attendance workflow. Existing and new workplaces default to owner_approval.
alter table public.attendance_workplaces
    add column if not exists attendance_verification_mode text not null default 'owner_approval';
alter table public.attendance_workplaces
    alter column attendance_verification_mode set default 'owner_approval';
alter table public.attendance_workplaces
    drop constraint if exists attendance_workplaces_verification_mode_check;
alter table public.attendance_workplaces
    add constraint attendance_workplaces_verification_mode_check
    check (attendance_verification_mode in ('owner_approval'));
update public.attendance_workplaces
   set attendance_verification_mode = 'owner_approval'
 where attendance_verification_mode is distinct from 'owner_approval';

create table if not exists public.attendance_mark_requests (
    id uuid primary key default gen_random_uuid(),
    staff_id uuid not null references public.attendance_staff(id) on delete cascade,
    attendance_date date not null,
    requested_by_user_id uuid not null references auth.users(id),
    requested_at timestamptz not null default now(),
    status text not null default 'pending'
        check (status in ('pending', 'approved', 'rejected')),
    reviewed_by_user_id uuid references auth.users(id),
    reviewed_at timestamptz
);
create unique index if not exists attendance_mark_requests_one_pending_per_day_uidx
    on public.attendance_mark_requests(staff_id, attendance_date)
    where status = 'pending';
create index if not exists attendance_mark_requests_staff_date_idx
    on public.attendance_mark_requests(staff_id, attendance_date desc);

alter table public.attendance_mark_requests enable row level security;
drop policy if exists attendance_mark_requests_owner_or_self_select
    on public.attendance_mark_requests;
create policy attendance_mark_requests_owner_or_self_select
    on public.attendance_mark_requests
    for select to authenticated
    using (
        requested_by_user_id = auth.uid()
        or exists (
            select 1 from public.attendance_staff s
            join public.attendance_workplaces w on w.id = s.workplace_id
            where s.id = attendance_mark_requests.staff_id
              and w.owner_id = auth.uid()
        )
    );
revoke all on table public.attendance_mark_requests from public, anon, authenticated;
grant select on table public.attendance_mark_requests to authenticated;

create or replace function public.request_my_attendance(p_staff_id uuid)
returns setof public.attendance_mark_requests
language plpgsql security definer
set search_path = public, pg_temp
as $$
declare
    v_user_id uuid := auth.uid();
    v_staff public.attendance_staff%rowtype;
    v_timezone text;
    v_mode text;
    v_date date;
begin
    if v_user_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    select s.* into v_staff
      from public.attendance_staff s
     where s.id = p_staff_id
       and s.auth_user_id = v_user_id
       and s.link_status = 'linked'
       and s.approval_status = 'accepted'
       and s.is_active = true;
    if not found then
        raise exception 'No accepted active staff registration found for this account'
            using errcode = '42501';
    end if;

    select w.timezone, w.attendance_verification_mode
      into v_timezone, v_mode
      from public.attendance_workplaces w
     where w.id = v_staff.workplace_id;

    if v_mode is distinct from 'owner_approval' then
        raise exception 'This attendance verification mode is not available'
            using errcode = '42501';
    end if;
    if v_timezone is null or not exists (
        select 1 from pg_timezone_names where name = v_timezone
    ) then
        raise exception 'Invalid workplace timezone configuration'
            using errcode = '22023';
    end if;

    v_date := (now() at time zone v_timezone)::date;
    if exists (
        select 1 from public.attendance_records r
        where r.staff_id = p_staff_id and r.attendance_date = v_date
    ) then
        raise exception 'Attendance is already recorded for today'
            using errcode = '23505';
    end if;

    return query
    select r.* from public.attendance_mark_requests r
    where r.staff_id = p_staff_id
      and r.attendance_date = v_date
      and r.status = 'pending';
    if found then
        return;
    end if;

    insert into public.attendance_mark_requests
        (staff_id, attendance_date, requested_by_user_id, status)
    values (p_staff_id, v_date, v_user_id, 'pending')
    on conflict (staff_id, attendance_date) where status = 'pending'
    do nothing;

    return query
    select r.* from public.attendance_mark_requests r
    where r.staff_id = p_staff_id
      and r.attendance_date = v_date
      and r.status = 'pending';
end;
$$;

create or replace function public.approve_owner_attendance_request(p_request_id uuid)
returns setof public.attendance_records
language plpgsql security definer
set search_path = public, pg_temp
as $$
declare
    v_owner_id uuid := auth.uid();
    v_request public.attendance_mark_requests%rowtype;
begin
    if v_owner_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    select r.* into v_request
      from public.attendance_mark_requests r
      join public.attendance_staff s on s.id = r.staff_id
      join public.attendance_workplaces w on w.id = s.workplace_id
     where r.id = p_request_id and w.owner_id = v_owner_id
     for update of r;
    if not found then
        raise exception 'Attendance request not found or access denied'
            using errcode = '42501';
    end if;
    if v_request.status <> 'pending' then
        raise exception 'Attendance request has already been reviewed'
            using errcode = '23505';
    end if;
    if not exists (
        select 1 from public.attendance_staff s
        where s.id = v_request.staff_id
          and s.is_active = true
          and s.link_status = 'linked'
          and s.approval_status = 'accepted'
    ) then
        raise exception 'Staff registration is no longer active'
            using errcode = '42501';
    end if;

    insert into public.attendance_records
        (staff_id, attendance_date, marked_by_user_id, marked_at, status)
    values
        (v_request.staff_id, v_request.attendance_date, v_owner_id, now(), 'present')
    on conflict (staff_id, attendance_date) do nothing;

    if not found then
        raise exception 'Attendance has already been recorded for this date'
            using errcode = '23505';
    end if;

    update public.attendance_mark_requests
       set status = 'approved',
           reviewed_by_user_id = v_owner_id,
           reviewed_at = now()
     where id = v_request.id;

    return query
    select r.* from public.attendance_records r
    where r.staff_id = v_request.staff_id
      and r.attendance_date = v_request.attendance_date;
end;
$$;

create or replace function public.reject_owner_attendance_request(p_request_id uuid)
returns integer
language plpgsql security definer
set search_path = public, pg_temp
as $$
declare
    v_owner_id uuid := auth.uid();
    v_updated integer := 0;
begin
    if v_owner_id is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    update public.attendance_mark_requests r
       set status = 'rejected',
           reviewed_by_user_id = v_owner_id,
           reviewed_at = now()
      from public.attendance_staff s
      join public.attendance_workplaces w on w.id = s.workplace_id
     where r.id = p_request_id
       and r.staff_id = s.id
       and w.owner_id = v_owner_id
       and r.status = 'pending';
    get diagnostics v_updated = row_count;
    if v_updated = 0 then
        raise exception 'Pending attendance request not found or access denied'
            using errcode = '42501';
    end if;
    return v_updated;
end;
$$;

-- Disable the old direct-mark RPC for authenticated clients.
revoke all on function public.mark_my_attendance(uuid) from public, anon, authenticated;
revoke all on function public.request_my_attendance(uuid) from public, anon, authenticated;
revoke all on function public.approve_owner_attendance_request(uuid) from public, anon, authenticated;
revoke all on function public.reject_owner_attendance_request(uuid) from public, anon, authenticated;
grant execute on function public.request_my_attendance(uuid) to authenticated;
grant execute on function public.approve_owner_attendance_request(uuid) to authenticated;
grant execute on function public.reject_owner_attendance_request(uuid) to authenticated;

comment on table public.attendance_mark_requests is
    'Staff attendance requests. Staff submit requests; only the workplace owner may approve or reject.';
comment on column public.attendance_workplaces.attendance_verification_mode is
    'Attendance verification policy. Existing and new workplaces default to owner_approval.';
