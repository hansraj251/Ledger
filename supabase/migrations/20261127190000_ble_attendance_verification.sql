-- BLE auto-attendance verification. Existing owner approval remains the default.
-- The owner device registers an Android Keystore public key. Staff receive a
-- short-lived, one-use challenge and the owner device signs its canonical payload
-- over BLE. Signature verification is performed by the Edge Function.

alter table public.attendance_workplaces
    add column if not exists owner_ble_public_key text;

alter table public.attendance_workplaces
    drop constraint if exists attendance_workplaces_verification_mode_check;
alter table public.attendance_workplaces
    add constraint attendance_workplaces_verification_mode_check
    check (attendance_verification_mode in ('owner_approval', 'bluetooth_auto'));

create table if not exists public.attendance_ble_challenges (
    id uuid primary key,
    staff_id uuid not null references public.attendance_staff(id) on delete cascade,
    attendance_date date not null,
    nonce text not null,
    canonical_payload text not null,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    consumed_at timestamptz,
    constraint attendance_ble_challenges_expiry_valid check (expires_at > created_at)
);
create index if not exists attendance_ble_challenges_staff_date_idx
    on public.attendance_ble_challenges(staff_id, attendance_date desc);
alter table public.attendance_ble_challenges enable row level security;
revoke all on table public.attendance_ble_challenges from public, anon, authenticated;

create or replace function public.register_owner_ble_public_key(p_public_key text)
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
    if p_public_key is null or length(p_public_key) < 80 or length(p_public_key) > 2048 then
        raise exception 'Invalid BLE public key' using errcode = '22023';
    end if;
    update public.attendance_workplaces
       set owner_ble_public_key = p_public_key,
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
    if p_mode not in ('owner_approval', 'bluetooth_auto') then
        raise exception 'Unsupported attendance verification mode' using errcode = '22023';
    end if;
    if p_mode = 'bluetooth_auto' and not exists (
        select 1 from public.attendance_workplaces
         where owner_id = v_owner_id
           and owner_ble_public_key is not null
    ) then
        raise exception 'Register this device for Bluetooth verification first' using errcode = '22023';
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

create or replace function public.create_my_ble_attendance_challenge(p_staff_id uuid)
returns table (
    challenge_id uuid,
    canonical_payload text,
    expires_at timestamptz
)
language plpgsql security definer
set search_path = public, pg_temp
as $$
declare
    v_user_id uuid := auth.uid();
    v_staff public.attendance_staff%rowtype;
    v_workplace public.attendance_workplaces%rowtype;
    v_date date;
    v_id uuid := gen_random_uuid();
    v_nonce text := replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', '');
    v_expires timestamptz := now() + interval '75 seconds';
    v_payload text;
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
        raise exception 'No accepted active staff registration found for this account' using errcode = '42501';
    end if;
    select w.* into v_workplace
      from public.attendance_workplaces w
     where w.id = v_staff.workplace_id;
    if not found or v_workplace.attendance_verification_mode <> 'bluetooth_auto'
       or v_workplace.owner_ble_public_key is null then
        raise exception 'Bluetooth auto attendance is not enabled' using errcode = '42501';
    end if;
    if v_workplace.timezone is null or not exists (
        select 1 from pg_timezone_names where name = v_workplace.timezone
    ) then
        raise exception 'Invalid workplace timezone configuration' using errcode = '22023';
    end if;
    v_date := (now() at time zone v_workplace.timezone)::date;
    if exists (
        select 1 from public.attendance_records r
         where r.staff_id = p_staff_id and r.attendance_date = v_date
    ) then
        raise exception 'Attendance is already recorded for today' using errcode = '23505';
    end if;
    update public.attendance_ble_challenges
       set consumed_at = now()
     where staff_id = p_staff_id
       and attendance_date = v_date
       and consumed_at is null;
    v_payload := 'align-ledger-ble-v1|' || v_id::text || '|' || p_staff_id::text || '|' ||
                 v_staff.workplace_id::text || '|' || v_date::text || '|' || v_nonce || '|' || extract(epoch from v_expires)::bigint::text;
    insert into public.attendance_ble_challenges
        (id, staff_id, attendance_date, nonce, canonical_payload, expires_at)
    values (v_id, p_staff_id, v_date, v_nonce, v_payload, v_expires);
    return query select v_id, v_payload, v_expires;
end;
$$;

-- Called only by the BLE verification Edge Function using the service role.
-- The Edge Function verifies the ECDSA signature before calling this one-use
-- finalizer. The database still rechecks expiry, mode, staff identity and date.
create or replace function public.consume_ble_attendance_challenge(p_challenge_id uuid)
returns setof public.attendance_records
language plpgsql security definer
set search_path = public, pg_temp
as $$
declare
    v_challenge public.attendance_ble_challenges%rowtype;
    v_staff public.attendance_staff%rowtype;
    v_workplace public.attendance_workplaces%rowtype;
    v_record public.attendance_records%rowtype;
    v_now timestamptz := now();
    v_today date;
begin
    select * into v_challenge
      from public.attendance_ble_challenges
     where id = p_challenge_id
     for update;
    if not found or v_challenge.consumed_at is not null or v_challenge.expires_at <= v_now then
        raise exception 'BLE challenge is invalid, expired, or already used' using errcode = '42501';
    end if;
    select * into v_staff from public.attendance_staff where id = v_challenge.staff_id;
    if not found or not v_staff.is_active or v_staff.link_status <> 'linked'
       or v_staff.approval_status <> 'accepted' or v_staff.auth_user_id is null then
        raise exception 'Staff registration is not active' using errcode = '42501';
    end if;
    select * into v_workplace from public.attendance_workplaces where id = v_staff.workplace_id;
    if not found or v_workplace.attendance_verification_mode <> 'bluetooth_auto'
       or v_workplace.owner_ble_public_key is null then
        raise exception 'Bluetooth verification is no longer enabled' using errcode = '42501';
    end if;
    v_today := (v_now at time zone v_workplace.timezone)::date;
    if v_today <> v_challenge.attendance_date then
        raise exception 'Attendance challenge date is no longer current' using errcode = '22023';
    end if;
    if exists (
        select 1 from public.attendance_records r
         where r.staff_id = v_staff.id and r.attendance_date = v_today
    ) then
        raise exception 'Attendance is already recorded for today' using errcode = '23505';
    end if;
    update public.attendance_ble_challenges
       set consumed_at = v_now
     where id = p_challenge_id and consumed_at is null;
    if not found then
        raise exception 'BLE challenge was already consumed' using errcode = '23505';
    end if;
    insert into public.attendance_records
        (staff_id, attendance_date, marked_by_user_id, marked_at, status)
    values (v_staff.id, v_today, v_staff.auth_user_id, v_now, 'present');
    update public.attendance_mark_requests
       set status = 'cancelled', reviewed_at = v_now
     where staff_id = v_staff.id
       and attendance_date = v_today
       and status = 'pending';
    select * into v_record from public.attendance_records
     where staff_id = v_staff.id and attendance_date = v_today;
    return next v_record;
end;
$$;

-- Allow the existing owner-approval fallback to work while Bluetooth mode is selected.
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
    select s.* into v_staff from public.attendance_staff s
     where s.id = p_staff_id and s.auth_user_id = v_user_id
       and s.link_status = 'linked' and s.approval_status = 'accepted' and s.is_active = true;
    if not found then
        raise exception 'No accepted active staff registration found for this account' using errcode = '42501';
    end if;
    select w.timezone, w.attendance_verification_mode into v_timezone, v_mode
      from public.attendance_workplaces w where w.id = v_staff.workplace_id;
    if v_mode not in ('owner_approval', 'bluetooth_auto') then
        raise exception 'This attendance verification mode is not available' using errcode = '42501';
    end if;
    if v_timezone is null or not exists (select 1 from pg_timezone_names where name = v_timezone) then
        raise exception 'Invalid workplace timezone configuration' using errcode = '22023';
    end if;
    v_date := (now() at time zone v_timezone)::date;
    if exists (select 1 from public.attendance_records r where r.staff_id = p_staff_id and r.attendance_date = v_date) then
        raise exception 'Attendance is already recorded for today' using errcode = '23505';
    end if;
    return query select r.* from public.attendance_mark_requests r
     where r.staff_id = p_staff_id and r.attendance_date = v_date and r.status = 'pending';
    if found then return; end if;
    insert into public.attendance_mark_requests
        (staff_id, attendance_date, requested_by_user_id, status)
    values (p_staff_id, v_date, v_user_id, 'pending')
    on conflict (staff_id, attendance_date) where status = 'pending' do nothing;
    return query select r.* from public.attendance_mark_requests r
     where r.staff_id = p_staff_id and r.attendance_date = v_date and r.status = 'pending';
end;
$$;

revoke all on function public.register_owner_ble_public_key(text) from public, anon, authenticated;
revoke all on function public.set_owner_attendance_verification_mode(text) from public, anon, authenticated;
revoke all on function public.create_my_ble_attendance_challenge(uuid) from public, anon, authenticated;
revoke all on function public.consume_ble_attendance_challenge(uuid) from public, anon, authenticated;
grant execute on function public.register_owner_ble_public_key(text) to authenticated;
grant execute on function public.set_owner_attendance_verification_mode(text) to authenticated;
grant execute on function public.create_my_ble_attendance_challenge(uuid) to authenticated;
grant execute on function public.consume_ble_attendance_challenge(uuid) to service_role;
grant execute on function public.request_my_attendance(uuid) to authenticated;

alter table public.attendance_mark_requests
    drop constraint if exists attendance_mark_requests_status_check;
alter table public.attendance_mark_requests
    add constraint attendance_mark_requests_status_check
    check (status in ('pending', 'approved', 'rejected', 'cancelled'));

comment on column public.attendance_workplaces.owner_ble_public_key is
    'Owner Android Keystore ECDSA P-256 public key used by the BLE attendance verifier.';
comment on column public.attendance_workplaces.attendance_verification_mode is
    'owner_approval (default) or bluetooth_auto with signed BLE challenge verification.';

create or replace function public.get_my_attendance_verification_mode(p_staff_id uuid)
returns text
language plpgsql security definer
set search_path = public, pg_temp
as $$
declare
    v_mode text;
begin
    if auth.uid() is null then
        raise exception 'Authentication required' using errcode = '28000';
    end if;
    select w.attendance_verification_mode into v_mode
      from public.attendance_staff s
      join public.attendance_workplaces w on w.id = s.workplace_id
     where s.id = p_staff_id
       and s.auth_user_id = auth.uid()
       and s.link_status = 'linked'
       and s.approval_status = 'accepted'
       and s.is_active = true;
    if not found then
        raise exception 'No accepted active staff registration found for this account' using errcode = '42501';
    end if;
    return coalesce(v_mode, 'owner_approval');
end;
$$;

revoke all on function public.get_my_attendance_verification_mode(uuid) from public, anon, authenticated;
grant execute on function public.get_my_attendance_verification_mode(uuid) to authenticated;

grant select on public.attendance_ble_challenges to service_role;
grant select on public.attendance_staff to service_role;
grant select on public.attendance_workplaces to service_role;
