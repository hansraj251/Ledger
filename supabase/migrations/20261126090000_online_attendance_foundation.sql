-- Online Attendance foundation.
-- Dedicated schema objects; does not modify Room, Online Groups, or Drive backup.
-- Review this migration before applying it to the linked Supabase project.

create table if not exists public.attendance_workplaces (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null unique references auth.users(id) on delete cascade,
    timezone text not null default 'Asia/Kolkata',
    weekly_off_days smallint[] not null default array[0]::smallint[],
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint attendance_workplaces_timezone_not_blank
        check (length(btrim(timezone)) > 0),
    constraint attendance_workplaces_weekly_off_days_valid
        check (
            cardinality(weekly_off_days) <= 7
            and weekly_off_days <@ array[0,1,2,3,4,5,6]::smallint[]
        )
);

create table if not exists public.attendance_staff (
    id uuid primary key default gen_random_uuid(),
    workplace_id uuid not null references public.attendance_workplaces(id) on delete cascade,
    local_staff_id bigint not null check (local_staff_id > 0),
    display_name text not null check (length(btrim(display_name)) > 0),
    email text not null,
    email_normalized text not null,
    auth_user_id uuid references auth.users(id) on delete set null,
    link_status text not null default 'pending'
        check (link_status in ('pending', 'linked', 'disabled')),
    is_active boolean not null default true,
    linked_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint attendance_staff_email_normalized
        check (email_normalized = lower(btrim(email)) and position('@' in email_normalized) > 1)
);

create unique index if not exists attendance_staff_workplace_email_uidx
    on public.attendance_staff(workplace_id, email_normalized);

create index if not exists attendance_staff_auth_user_idx
    on public.attendance_staff(auth_user_id)
    where auth_user_id is not null;

create unique index if not exists attendance_staff_workplace_local_id_uidx
    on public.attendance_staff(workplace_id, local_staff_id);

create table if not exists public.attendance_records (
    id uuid primary key default gen_random_uuid(),
    staff_id uuid not null references public.attendance_staff(id) on delete cascade,
    attendance_date date not null,
    marked_by_user_id uuid not null references auth.users(id),
    marked_at timestamptz not null default now(),
    constraint attendance_records_one_per_day unique (staff_id, attendance_date)
);

create index if not exists attendance_records_staff_date_idx
    on public.attendance_records(staff_id, attendance_date desc);

create table if not exists public.attendance_holidays (
    id uuid primary key default gen_random_uuid(),
    workplace_id uuid not null references public.attendance_workplaces(id) on delete cascade,
    holiday_date date not null,
    title text not null check (length(btrim(title)) > 0),
    created_at timestamptz not null default now(),
    constraint attendance_holidays_one_per_day unique (workplace_id, holiday_date)
);

alter table public.attendance_workplaces enable row level security;
alter table public.attendance_staff enable row level security;
alter table public.attendance_records enable row level security;
alter table public.attendance_holidays enable row level security;

drop policy if exists attendance_workplaces_owner_select
    on public.attendance_workplaces;
create policy attendance_workplaces_owner_select
    on public.attendance_workplaces
    for select to authenticated
    using (owner_id = auth.uid());

drop policy if exists attendance_staff_owner_or_self_select
    on public.attendance_staff;
create policy attendance_staff_owner_or_self_select
    on public.attendance_staff
    for select to authenticated
    using (
        auth_user_id = auth.uid()
        or exists (
            select 1
            from public.attendance_workplaces w
            where w.id = attendance_staff.workplace_id
              and w.owner_id = auth.uid()
        )
    );

drop policy if exists attendance_records_owner_or_self_select
    on public.attendance_records;
create policy attendance_records_owner_or_self_select
    on public.attendance_records
    for select to authenticated
    using (
        exists (
            select 1
            from public.attendance_staff s
            where s.id = attendance_records.staff_id
              and s.auth_user_id = auth.uid()
        )
        or exists (
            select 1
            from public.attendance_staff s
            join public.attendance_workplaces w on w.id = s.workplace_id
            where s.id = attendance_records.staff_id
              and w.owner_id = auth.uid()
        )
    );

drop policy if exists attendance_holidays_owner_select
    on public.attendance_holidays;
create policy attendance_holidays_owner_select
    on public.attendance_holidays
    for select to authenticated
    using (
        exists (
            select 1
            from public.attendance_workplaces w
            where w.id = attendance_holidays.workplace_id
              and w.owner_id = auth.uid()
        )
    );

drop policy if exists attendance_holidays_owner_insert
    on public.attendance_holidays;
create policy attendance_holidays_owner_insert
    on public.attendance_holidays
    for insert to authenticated
    with check (
        exists (
            select 1
            from public.attendance_workplaces w
            where w.id = attendance_holidays.workplace_id
              and w.owner_id = auth.uid()
        )
    );

drop policy if exists attendance_holidays_owner_update
    on public.attendance_holidays;
create policy attendance_holidays_owner_update
    on public.attendance_holidays
    for update to authenticated
    using (
        exists (
            select 1
            from public.attendance_workplaces w
            where w.id = attendance_holidays.workplace_id
              and w.owner_id = auth.uid()
        )
    )
    with check (
        exists (
            select 1
            from public.attendance_workplaces w
            where w.id = attendance_holidays.workplace_id
              and w.owner_id = auth.uid()
        )
    );

drop policy if exists attendance_holidays_owner_delete
    on public.attendance_holidays;
create policy attendance_holidays_owner_delete
    on public.attendance_holidays
    for delete to authenticated
    using (
        exists (
            select 1
            from public.attendance_workplaces w
            where w.id = attendance_holidays.workplace_id
              and w.owner_id = auth.uid()
        )
    );

-- Explicitly reset table privileges. Do not rely on project-wide default
-- privileges, which may have granted broader access in this Supabase project.
revoke all on table public.attendance_workplaces from public, anon, authenticated;
revoke all on table public.attendance_staff from public, anon, authenticated;
revoke all on table public.attendance_records from public, anon, authenticated;
revoke all on table public.attendance_holidays from public, anon, authenticated;

grant select on public.attendance_workplaces to authenticated;
grant select on public.attendance_staff to authenticated;
grant select on public.attendance_records to authenticated;
grant select, insert, update, delete on public.attendance_holidays to authenticated;

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
                   when s.email_normalized <> v_email then null
                   else s.auth_user_id
               end,
               link_status = case
                   when s.email_normalized <> v_email or s.auth_user_id is null
                       then 'pending'
                   else 'linked'
               end,
               linked_at = case
                   when s.email_normalized <> v_email then null
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
            email_normalized
        )
        values (
            v_workplace_id,
            p_local_staff_id,
            btrim(p_display_name),
            btrim(p_email),
            v_email
        )
        returning id into v_staff_id;
    end if;

    return v_staff_id;
end;
$$;

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
       and u.email_confirmed_at is not null
       and (
           u.raw_app_meta_data ->> 'provider' = 'google'
           or coalesce(u.raw_app_meta_data -> 'providers', '[]'::jsonb) ? 'google'
       );

    if v_email is null or v_email = '' then
        raise exception 'A verified Google account email is required'
            using errcode = '42501';
    end if;

    update public.attendance_staff s
       set auth_user_id = v_user_id,
           link_status = 'linked',
           linked_at = coalesce(s.linked_at, now()),
           updated_at = now()
     where s.email_normalized = v_email
       and s.is_active = true
       and s.link_status <> 'disabled'
       and (s.auth_user_id is null or s.auth_user_id = v_user_id);

    get diagnostics v_linked_count = row_count;
    return v_linked_count;
end;
$$;

create or replace function public.set_attendance_staff_active(
    p_staff_id uuid,
    p_is_active boolean
)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
    if auth.uid() is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;

    update public.attendance_staff s
       set is_active = p_is_active,
           link_status = case when p_is_active then
               case when s.auth_user_id is null then 'pending' else 'linked' end
               else 'disabled'
           end,
           updated_at = now()
      from public.attendance_workplaces w
     where s.id = p_staff_id
       and w.id = s.workplace_id
       and w.owner_id = auth.uid();

    if not found then
        raise exception 'Staff record not found or access denied'
            using errcode = '42501';
    end if;
end;
$$;

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
       and s.is_active = true;

    if not found then
        raise exception 'No active linked staff record found for this account'
            using errcode = '42501';
    end if;

    select w.timezone
      into v_timezone
      from public.attendance_workplaces w
     where w.id = v_staff.workplace_id;

    if v_timezone is null or not exists (
        select 1
          from pg_timezone_names
         where name = v_timezone
    ) then
        raise exception 'Invalid workplace timezone configuration'
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

-- SECURITY DEFINER RPCs must never be callable by anonymous users or PUBLIC.
revoke all on function public.register_attendance_staff(bigint, text, text) from public, anon, authenticated;
revoke all on function public.claim_my_attendance_staff() from public, anon, authenticated;
revoke all on function public.set_attendance_staff_active(uuid, boolean) from public, anon, authenticated;
revoke all on function public.mark_my_attendance(uuid) from public, anon, authenticated;

grant execute on function public.register_attendance_staff(bigint, text, text) to authenticated;
grant execute on function public.claim_my_attendance_staff() to authenticated;
grant execute on function public.set_attendance_staff_active(uuid, boolean) to authenticated;
grant execute on function public.mark_my_attendance(uuid) to authenticated;

comment on table public.attendance_workplaces is
    'Attendance workplace ownership and calendar configuration.';
comment on table public.attendance_staff is
    'Owner-managed staff identity mapping for verified Supabase Auth users.';
comment on table public.attendance_records is
    'Immutable self-marked attendance events; one record per staff member per workplace-local date.';
comment on table public.attendance_holidays is
    'Owner-managed non-working dates excluded from attendance absence calculations.';
