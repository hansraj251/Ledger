-- Align Ledger: group expense approval workflow
-- Apply in Supabase SQL Editor after reviewing the full migration.
-- Existing final expense rows and Google Drive sync tables are not altered.

BEGIN;

CREATE TABLE IF NOT EXISTS public.cloud_group_expense_approval_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id uuid NOT NULL REFERENCES public.cloud_groups(id) ON DELETE CASCADE,
    created_by uuid NOT NULL,
    title text NOT NULL,
    amount numeric NOT NULL CHECK (amount > 0),
    category text NOT NULL CHECK (category IN ('Cash', 'Online')),
    note text NOT NULL DEFAULT '',
    expense_date_ms bigint NOT NULL,
    split_mode text NOT NULL CHECK (split_mode IN ('Equal', 'Custom', 'Share')),
    splits jsonb NOT NULL CHECK (jsonb_typeof(splits) = 'array'),
    payments jsonb NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(payments) = 'array'),
    status text NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'changes_required', 'approved')),
    revision integer NOT NULL DEFAULT 1 CHECK (revision > 0),
    rejection_reason text,
    final_expense_id uuid,
    created_at_ms bigint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    approved_at timestamptz,
    CONSTRAINT cloud_group_expense_approval_final_id_unique UNIQUE (final_expense_id)
);

CREATE TABLE IF NOT EXISTS public.cloud_group_expense_approvals (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id uuid NOT NULL REFERENCES public.cloud_group_expense_approval_requests(id) ON DELETE CASCADE,
    group_id uuid NOT NULL REFERENCES public.cloud_groups(id) ON DELETE CASCADE,
    revision integer NOT NULL CHECK (revision > 0),
    member_id uuid NOT NULL,
    member_name_snapshot text NOT NULL,
    approval_status text NOT NULL DEFAULT 'pending'
        CHECK (approval_status IN ('pending', 'approved', 'rejected')),
    responded_by uuid,
    responded_at timestamptz,
    rejection_note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT cloud_group_expense_approvals_revision_member_unique
        UNIQUE (request_id, revision, member_id)
);

CREATE INDEX IF NOT EXISTS idx_cloud_group_expense_approval_requests_group_status
    ON public.cloud_group_expense_approval_requests (group_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cloud_group_expense_approvals_request_revision
    ON public.cloud_group_expense_approvals (request_id, revision, approval_status);
CREATE INDEX IF NOT EXISTS idx_cloud_group_expense_approvals_group_member
    ON public.cloud_group_expense_approvals (group_id, member_id, approval_status);

ALTER TABLE public.cloud_group_expense_approval_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cloud_group_expense_approvals ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS cloud_group_expense_approval_requests_select_member
    ON public.cloud_group_expense_approval_requests;
CREATE POLICY cloud_group_expense_approval_requests_select_member
    ON public.cloud_group_expense_approval_requests
    FOR SELECT TO authenticated
    USING (public.is_cloud_group_member(group_id));

DROP POLICY IF EXISTS cloud_group_expense_approvals_select_member
    ON public.cloud_group_expense_approvals;
CREATE POLICY cloud_group_expense_approvals_select_member
    ON public.cloud_group_expense_approvals
    FOR SELECT TO authenticated
    USING (public.is_cloud_group_member(group_id));

REVOKE ALL ON TABLE public.cloud_group_expense_approval_requests FROM anon, authenticated;
REVOKE ALL ON TABLE public.cloud_group_expense_approvals FROM anon, authenticated;
GRANT SELECT ON TABLE public.cloud_group_expense_approval_requests TO authenticated;
GRANT SELECT ON TABLE public.cloud_group_expense_approvals TO authenticated;

-- Pending requests and approval rows use the same group write lock as final expenses.
DROP TRIGGER IF EXISTS trg_lock_expense_approval_requests ON public.cloud_group_expense_approval_requests;
CREATE TRIGGER trg_lock_expense_approval_requests
BEFORE INSERT OR UPDATE OR DELETE ON public.cloud_group_expense_approval_requests
FOR EACH ROW EXECUTE FUNCTION public.lock_cloud_group_for_write();

DROP TRIGGER IF EXISTS trg_lock_expense_approvals ON public.cloud_group_expense_approvals;
CREATE TRIGGER trg_lock_expense_approvals
BEFORE INSERT OR UPDATE OR DELETE ON public.cloud_group_expense_approvals
FOR EACH ROW EXECUTE FUNCTION public.lock_cloud_group_for_write();

DROP TRIGGER IF EXISTS trg_validate_expense_approval_member ON public.cloud_group_expense_approvals;
CREATE TRIGGER trg_validate_expense_approval_member
BEFORE INSERT OR UPDATE ON public.cloud_group_expense_approvals
FOR EACH ROW EXECUTE FUNCTION public.cloud_group_validate_joined_member_reference();

-- Final expense rows can only be inserted when an approved request with the same ID exists.
-- The request is marked approved and the final expense is inserted in one transaction.
CREATE OR REPLACE FUNCTION public.require_approved_group_expense_request()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM public.cloud_group_expense_approval_requests r
        WHERE r.id = NEW.id
          AND r.group_id = NEW.group_id
          AND r.created_by = NEW.created_by
          AND r.status = 'approved'
          AND r.final_expense_id = NEW.id
    ) THEN
        RAISE EXCEPTION 'New group expenses must pass member approval before finalization.';
    END IF;
    RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_require_approved_group_expense_request
    ON public.cloud_group_expenses;
CREATE TRIGGER trg_require_approved_group_expense_request
BEFORE INSERT ON public.cloud_group_expenses
FOR EACH ROW EXECUTE FUNCTION public.require_approved_group_expense_request();

-- Remove direct INSERT routes. Existing SELECT/UPDATE/DELETE policies remain unchanged.
ALTER TABLE public.cloud_group_expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cloud_group_expense_splits ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cloud_group_expense_payments ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS cloud_expenses_insert_member ON public.cloud_group_expenses;
DROP POLICY IF EXISTS cloud_splits_insert_member ON public.cloud_group_expense_splits;
DROP POLICY IF EXISTS cloud_payments_insert_member ON public.cloud_group_expense_payments;
REVOKE INSERT ON TABLE
    public.cloud_group_expenses,
    public.cloud_group_expense_splits,
    public.cloud_group_expense_payments
FROM anon, authenticated;

CREATE OR REPLACE FUNCTION public.finalize_cloud_group_expense_approval(p_request_id uuid)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
DECLARE
    v_request public.cloud_group_expense_approval_requests%ROWTYPE;
BEGIN
    SELECT *
      INTO v_request
      FROM public.cloud_group_expense_approval_requests
     WHERE id = p_request_id
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Pending expense request not found.';
    END IF;

    IF v_request.status = 'approved' AND v_request.final_expense_id IS NOT NULL THEN
        RETURN v_request.final_expense_id;
    END IF;

    IF v_request.status <> 'pending' THEN
        RAISE EXCEPTION 'This expense is not awaiting approval.';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.cloud_group_expense_approvals a
         WHERE a.request_id = v_request.id
           AND a.revision = v_request.revision
           AND a.approval_status <> 'approved'
    ) THEN
        RAISE EXCEPTION 'All required members must approve this expense first.';
    END IF;

    -- The pending request ID is reused as the final expense ID.
    -- This also provides the authorization marker checked by the INSERT trigger above.
    UPDATE public.cloud_group_expense_approval_requests
       SET status = 'approved',
           final_expense_id = v_request.id,
           approved_at = now(),
           updated_at = now()
     WHERE id = v_request.id;

    INSERT INTO public.cloud_group_expenses (
        id, group_id, title, amount, category, note,
        expense_date_ms, split_mode, created_by, created_at_ms, updated_at
    ) VALUES (
        v_request.id, v_request.group_id, trim(v_request.title), v_request.amount,
        v_request.category, COALESCE(v_request.note, ''), v_request.expense_date_ms,
        v_request.split_mode, v_request.created_by, v_request.created_at_ms, now()
    );

    INSERT INTO public.cloud_group_expense_splits (
        group_id, expense_id, member_id, member_name_snapshot, amount, share_units
    )
    SELECT
        v_request.group_id,
        v_request.id,
        x.member_id,
        m.member_name,
        x.amount,
        x.share_units
    FROM jsonb_to_recordset(v_request.splits)
        AS x(member_id uuid, amount numeric, share_units numeric)
    JOIN public.cloud_group_members m
      ON m.id = x.member_id
     AND m.group_id = v_request.group_id
     AND m.status = 'joined';

    INSERT INTO public.cloud_group_expense_payments (
        group_id, expense_id, member_id, member_name_snapshot,
        amount, paid_at_ms, payment_type
    )
    SELECT
        v_request.group_id,
        v_request.id,
        x.member_id,
        m.member_name,
        x.amount,
        (extract(epoch FROM clock_timestamp()) * 1000)::bigint,
        x.payment_type
    FROM jsonb_to_recordset(v_request.payments)
        AS x(member_id uuid, amount numeric, payment_type text)
    JOIN public.cloud_group_members m
      ON m.id = x.member_id
     AND m.group_id = v_request.group_id
     AND m.status = 'joined';

    RETURN v_request.id;
END;
$function$;

CREATE OR REPLACE FUNCTION public.submit_cloud_group_expense_for_approval(
    p_group_id uuid,
    p_request_id uuid,
    p_title text,
    p_amount numeric,
    p_category text,
    p_note text,
    p_expense_date_ms bigint,
    p_split_mode text,
    p_splits jsonb,
    p_payments jsonb
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
DECLARE
    v_user_id uuid := auth.uid();
    v_request_id uuid;
    v_revision integer;
    v_status text;
    v_creator uuid;
    v_split_rows integer;
    v_split_distinct integer;
    v_split_total numeric;
    v_payment_rows integer;
    v_payment_distinct integer;
    v_payment_total numeric;
    v_is_settled boolean;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;
    IF NOT public.is_cloud_group_member(p_group_id) THEN
        RAISE EXCEPTION 'You are not a joined member of this group.';
    END IF;
    IF p_title IS NULL OR length(trim(p_title)) = 0 OR length(trim(p_title)) > 200 THEN
        RAISE EXCEPTION 'Expense title must contain 1 to 200 characters.';
    END IF;
    IF p_amount IS NULL OR p_amount <= 0 OR p_amount <> round(p_amount, 2) THEN
        RAISE EXCEPTION 'Invalid expense amount.';
    END IF;
    IF p_category IS NULL OR p_category NOT IN ('Cash', 'Online') THEN
        RAISE EXCEPTION 'Invalid payment type.';
    END IF;
    IF p_split_mode IS NULL OR p_split_mode NOT IN ('Equal', 'Custom', 'Share') THEN
        RAISE EXCEPTION 'Invalid split mode.';
    END IF;
    IF p_expense_date_ms IS NULL OR p_expense_date_ms <= 0 THEN
        RAISE EXCEPTION 'Expense date is required.';
    END IF;
    IF jsonb_typeof(p_splits) IS DISTINCT FROM 'array' THEN
        RAISE EXCEPTION 'Invalid expense splits.';
    END IF;
    IF jsonb_array_length(p_splits) = 0 THEN
        RAISE EXCEPTION 'At least one expense split is required.';
    END IF;
    IF jsonb_typeof(p_payments) IS DISTINCT FROM 'array' THEN
        RAISE EXCEPTION 'Invalid payment details.';
    END IF;

    SELECT count(*), count(DISTINCT x.member_id), COALESCE(sum(x.amount), 0)
      INTO v_split_rows, v_split_distinct, v_split_total
      FROM jsonb_to_recordset(p_splits)
        AS x(member_id uuid, amount numeric, share_units numeric);

    IF v_split_rows <> v_split_distinct THEN
        RAISE EXCEPTION 'Duplicate members in expense splits.';
    END IF;
    IF EXISTS (
        SELECT 1
          FROM jsonb_to_recordset(p_splits)
            AS x(member_id uuid, amount numeric, share_units numeric)
          LEFT JOIN public.cloud_group_members m
            ON m.id = x.member_id
           AND m.group_id = p_group_id
           AND m.status = 'joined'
         WHERE m.id IS NULL
            OR x.amount IS NULL OR x.amount < 0 OR x.amount <> round(x.amount, 2)
            OR x.share_units IS NULL OR x.share_units < 0
    ) THEN
        RAISE EXCEPTION 'Invalid split amount or unjoined member.';
    END IF;
    IF v_split_total <> p_amount THEN
        RAISE EXCEPTION 'Split amounts must equal the expense amount.';
    END IF;

    SELECT count(*), count(DISTINCT x.member_id), COALESCE(sum(x.amount), 0)
      INTO v_payment_rows, v_payment_distinct, v_payment_total
      FROM jsonb_to_recordset(p_payments)
        AS x(member_id uuid, amount numeric, payment_type text);

    IF v_payment_rows <> v_payment_distinct THEN
        RAISE EXCEPTION 'Duplicate members in expense payments.';
    END IF;
    IF EXISTS (
        SELECT 1
          FROM jsonb_to_recordset(p_payments)
            AS x(member_id uuid, amount numeric, payment_type text)
          LEFT JOIN public.cloud_group_members m
            ON m.id = x.member_id
           AND m.group_id = p_group_id
           AND m.status = 'joined'
         WHERE m.id IS NULL
            OR x.amount IS NULL OR x.amount <= 0 OR x.amount <> round(x.amount, 2)
            OR x.payment_type IS NULL OR x.payment_type NOT IN ('Cash', 'Online')
    ) THEN
        RAISE EXCEPTION 'Invalid payment or unjoined member.';
    END IF;
    IF v_payment_rows > 0 AND v_payment_total <> p_amount THEN
        RAISE EXCEPTION 'Payment amounts must equal the expense amount.';
    END IF;

    v_is_settled := lower(btrim(p_title)) = 'settled';
    IF v_is_settled AND (v_split_rows <> 1 OR v_payment_rows <> 1) THEN
        RAISE EXCEPTION 'A settled expense must have exactly one receiver and one payer.';
    END IF;

    -- Every non-creator member who must approve must have a linked authenticated account.
    -- For a settled expense the split contains only the receiver; payer is not an approver.
    IF EXISTS (
        SELECT 1
          FROM jsonb_to_recordset(p_splits)
            AS x(member_id uuid, amount numeric, share_units numeric)
          JOIN public.cloud_group_members m
            ON m.id = x.member_id AND m.group_id = p_group_id AND m.status = 'joined'
         WHERE m.user_id IS NULL
           AND m.user_id IS DISTINCT FROM v_user_id
    ) THEN
        RAISE EXCEPTION 'Every member required to approve must first link their account and join the group.';
    END IF;

    IF p_request_id IS NULL THEN
        INSERT INTO public.cloud_group_expense_approval_requests (
            group_id, created_by, title, amount, category, note,
            expense_date_ms, split_mode, splits, payments,
            status, revision, created_at_ms
        ) VALUES (
            p_group_id, v_user_id, trim(p_title), p_amount, p_category,
            COALESCE(p_note, ''), p_expense_date_ms, p_split_mode,
            p_splits, p_payments, 'pending', 1,
            (extract(epoch FROM clock_timestamp()) * 1000)::bigint
        ) RETURNING id, revision INTO v_request_id, v_revision;
    ELSE
        SELECT status, created_by
          INTO v_status, v_creator
          FROM public.cloud_group_expense_approval_requests
         WHERE id = p_request_id AND group_id = p_group_id
         FOR UPDATE;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Expense approval request not found.';
        END IF;
        IF v_creator <> v_user_id THEN
            RAISE EXCEPTION 'Only the expense creator can edit and resubmit it.';
        END IF;
        IF v_status <> 'changes_required' THEN
            RAISE EXCEPTION 'Only an expense returned for changes can be resubmitted.';
        END IF;

        UPDATE public.cloud_group_expense_approval_requests
           SET title = trim(p_title), amount = p_amount, category = p_category,
               note = COALESCE(p_note, ''), expense_date_ms = p_expense_date_ms,
               split_mode = p_split_mode, splits = p_splits, payments = p_payments,
               status = 'pending', revision = revision + 1,
               rejection_reason = NULL, final_expense_id = NULL,
               approved_at = NULL, updated_at = now()
         WHERE id = p_request_id
         RETURNING id, revision INTO v_request_id, v_revision;
    END IF;

    INSERT INTO public.cloud_group_expense_approvals (
        request_id, group_id, revision, member_id, member_name_snapshot
    )
    SELECT
        v_request_id, p_group_id, v_revision, m.id, m.member_name
    FROM jsonb_to_recordset(p_splits)
        AS x(member_id uuid, amount numeric, share_units numeric)
    JOIN public.cloud_group_members m
      ON m.id = x.member_id
     AND m.group_id = p_group_id
     AND m.status = 'joined'
    WHERE m.user_id IS DISTINCT FROM v_user_id;

    -- Creator approval is implicit. If the creator is the only required member
    -- (or is the settled receiver), finalize immediately in this same transaction.
    IF NOT EXISTS (
        SELECT 1
          FROM public.cloud_group_expense_approvals a
         WHERE a.request_id = v_request_id
           AND a.revision = v_revision
    ) THEN
        PERFORM public.finalize_cloud_group_expense_approval(v_request_id);
    END IF;

    RETURN v_request_id;
END;
$function$;

CREATE OR REPLACE FUNCTION public.respond_cloud_group_expense_approval(
    p_group_id uuid,
    p_request_id uuid,
    p_approve boolean,
    p_rejection_note text DEFAULT NULL
)
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
DECLARE
    v_user_id uuid := auth.uid();
    v_revision integer;
    v_status text;
    v_member_id uuid;
    v_pending_count integer;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;
    IF p_approve IS NULL THEN
        RAISE EXCEPTION 'Approval decision is required.';
    END IF;

    SELECT revision, status
      INTO v_revision, v_status
      FROM public.cloud_group_expense_approval_requests
     WHERE id = p_request_id AND group_id = p_group_id
     FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Expense approval request not found.';
    END IF;
    IF v_status <> 'pending' THEN
        RAISE EXCEPTION 'This expense is no longer awaiting approval.';
    END IF;

    UPDATE public.cloud_group_expense_approvals a
       SET approval_status = CASE WHEN p_approve THEN 'approved' ELSE 'rejected' END,
           responded_by = v_user_id,
           responded_at = now(),
           rejection_note = CASE WHEN p_approve THEN NULL ELSE NULLIF(left(trim(COALESCE(p_rejection_note, '')), 500), '') END
      FROM public.cloud_group_members m
     WHERE a.request_id = p_request_id
       AND a.group_id = p_group_id
       AND a.revision = v_revision
       AND a.approval_status = 'pending'
       AND m.id = a.member_id
       AND m.group_id = p_group_id
       AND m.user_id = v_user_id
       AND m.status = 'joined'
    RETURNING a.member_id INTO v_member_id;

    IF v_member_id IS NULL THEN
        RAISE EXCEPTION 'You are not an eligible pending approver for this expense.';
    END IF;

    IF NOT p_approve THEN
        UPDATE public.cloud_group_expense_approval_requests
           SET status = 'changes_required',
               rejection_reason = COALESCE(NULLIF(left(trim(COALESCE(p_rejection_note, '')), 500), ''), 'A member requested changes.'),
               updated_at = now()
         WHERE id = p_request_id;
        RETURN 'changes_required';
    END IF;

    SELECT count(*)
      INTO v_pending_count
      FROM public.cloud_group_expense_approvals a
     WHERE a.request_id = p_request_id
       AND a.revision = v_revision
       AND a.approval_status <> 'approved';

    IF v_pending_count = 0 THEN
        PERFORM public.finalize_cloud_group_expense_approval(p_request_id);
        RETURN 'approved';
    END IF;

    RETURN 'pending';
END;
$function$;

REVOKE ALL ON FUNCTION public.finalize_cloud_group_expense_approval(uuid) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.submit_cloud_group_expense_for_approval(uuid, uuid, text, numeric, text, text, bigint, text, jsonb, jsonb) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.respond_cloud_group_expense_approval(uuid, uuid, boolean, text) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.submit_cloud_group_expense_for_approval(uuid, uuid, text, numeric, text, text, bigint, text, jsonb, jsonb) TO authenticated;
GRANT EXECUTE ON FUNCTION public.respond_cloud_group_expense_approval(uuid, uuid, boolean, text) TO authenticated;

COMMIT;

-- Post-migration checks (run separately after applying):
-- SELECT tablename, policyname, cmd FROM pg_policies
-- WHERE schemaname='public' AND tablename LIKE 'cloud_group_expense%';
-- SELECT proname, pg_get_function_identity_arguments(oid)
-- FROM pg_proc WHERE pronamespace='public'::regnamespace
--   AND proname IN ('submit_cloud_group_expense_for_approval', 'respond_cloud_group_expense_approval');
