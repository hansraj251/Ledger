-- Align Ledger: approval workflow for edits and deletes of finalized group expenses.
-- Apply only after the existing 20261125_group_expense_approval_workflow.sql migration.
-- Existing Add Expense approval records remain request_type='add'. Google Drive sync is untouched.

BEGIN;

ALTER TABLE public.cloud_group_expense_approval_requests
    ADD COLUMN IF NOT EXISTS request_type text NOT NULL DEFAULT 'add',
    ADD COLUMN IF NOT EXISTS target_expense_id uuid;

ALTER TABLE public.cloud_group_expense_approval_requests
    DROP CONSTRAINT IF EXISTS cloud_group_expense_approval_requests_status_check;
ALTER TABLE public.cloud_group_expense_approval_requests
    ADD CONSTRAINT cloud_group_expense_approval_requests_status_check
    CHECK (status IN ('pending', 'changes_required', 'approved', 'cancelled'));

ALTER TABLE public.cloud_group_expense_approval_requests
    DROP CONSTRAINT IF EXISTS cloud_group_expense_approval_requests_request_type_check;
ALTER TABLE public.cloud_group_expense_approval_requests
    ADD CONSTRAINT cloud_group_expense_approval_requests_request_type_check
    CHECK (request_type IN ('add', 'edit', 'delete'));

-- The old unique constraint is suitable only for Add requests. Edit/Delete history may
-- legitimately refer to the same target expense over time.
ALTER TABLE public.cloud_group_expense_approval_requests
    DROP CONSTRAINT IF EXISTS cloud_group_expense_approval_final_id_unique;
CREATE UNIQUE INDEX IF NOT EXISTS uq_group_expense_add_final_id
    ON public.cloud_group_expense_approval_requests(final_expense_id)
    WHERE request_type = 'add' AND final_expense_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_group_expense_active_change_target
    ON public.cloud_group_expense_approval_requests(group_id, target_expense_id)
    WHERE request_type IN ('edit', 'delete')
      AND target_expense_id IS NOT NULL
      AND status IN ('pending', 'changes_required');

-- One finalizer handles all three request types. It is called only inside the
-- submit/respond SECURITY DEFINER transactions; the function itself is not executable by clients.
CREATE OR REPLACE FUNCTION public.finalize_cloud_group_expense_approval(p_request_id uuid)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
DECLARE
    v_request public.cloud_group_expense_approval_requests%ROWTYPE;
    v_result_expense_id uuid;
BEGIN
    SELECT * INTO v_request
      FROM public.cloud_group_expense_approval_requests
     WHERE id = p_request_id
     FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Expense approval request not found.';
    END IF;
    IF v_request.status = 'approved' AND v_request.final_expense_id IS NOT NULL THEN
        RETURN v_request.final_expense_id;
    END IF;
    IF v_request.status <> 'pending' THEN
        RAISE EXCEPTION 'This request is not awaiting approval.';
    END IF;
    IF EXISTS (
        SELECT 1 FROM public.cloud_group_expense_approvals a
         WHERE a.request_id = v_request.id
           AND a.revision = v_request.revision
           AND a.approval_status <> 'approved'
    ) THEN
        RAISE EXCEPTION 'All required members must approve this request first.';
    END IF;

    IF v_request.request_type = 'add' THEN
        v_result_expense_id := v_request.id;
        UPDATE public.cloud_group_expense_approval_requests
           SET status = 'approved', final_expense_id = v_result_expense_id,
               approved_at = now(), updated_at = now()
         WHERE id = v_request.id;

        INSERT INTO public.cloud_group_expenses (
            id, group_id, title, amount, category, note, expense_date_ms,
            split_mode, created_by, created_at_ms, updated_at
        ) VALUES (
            v_request.id, v_request.group_id, trim(v_request.title), v_request.amount,
            v_request.category, COALESCE(v_request.note, ''), v_request.expense_date_ms,
            v_request.split_mode, v_request.created_by, v_request.created_at_ms, now()
        );

    ELSIF v_request.request_type = 'edit' THEN
        v_result_expense_id := v_request.target_expense_id;
        IF v_result_expense_id IS NULL OR NOT EXISTS (
            SELECT 1 FROM public.cloud_group_expenses e
             WHERE e.id = v_result_expense_id AND e.group_id = v_request.group_id
             FOR UPDATE
        ) THEN
            RAISE EXCEPTION 'The expense being edited no longer exists.';
        END IF;

        UPDATE public.cloud_group_expense_approval_requests
           SET status = 'approved', final_expense_id = v_result_expense_id,
               approved_at = now(), updated_at = now()
         WHERE id = v_request.id;

        UPDATE public.cloud_group_expenses
           SET title = trim(v_request.title), amount = v_request.amount,
               category = v_request.category, note = COALESCE(v_request.note, ''),
               expense_date_ms = v_request.expense_date_ms,
               split_mode = v_request.split_mode, updated_at = now()
         WHERE id = v_result_expense_id AND group_id = v_request.group_id;

        DELETE FROM public.cloud_group_expense_splits
         WHERE expense_id = v_result_expense_id AND group_id = v_request.group_id;
        DELETE FROM public.cloud_group_expense_payments
         WHERE expense_id = v_result_expense_id AND group_id = v_request.group_id;

    ELSIF v_request.request_type = 'delete' THEN
        v_result_expense_id := v_request.target_expense_id;
        IF v_result_expense_id IS NULL OR NOT EXISTS (
            SELECT 1 FROM public.cloud_group_expenses e
             WHERE e.id = v_result_expense_id AND e.group_id = v_request.group_id
             FOR UPDATE
        ) THEN
            RAISE EXCEPTION 'The expense being deleted no longer exists.';
        END IF;

        UPDATE public.cloud_group_expense_approval_requests
           SET status = 'approved', final_expense_id = v_result_expense_id,
               approved_at = now(), updated_at = now()
         WHERE id = v_request.id;

        DELETE FROM public.cloud_group_expenses
         WHERE id = v_result_expense_id AND group_id = v_request.group_id;
    ELSE
        RAISE EXCEPTION 'Unsupported expense approval request type.';
    END IF;

    IF v_request.request_type <> 'delete' THEN
        INSERT INTO public.cloud_group_expense_splits (
            group_id, expense_id, member_id, member_name_snapshot, amount, share_units
        )
        SELECT v_request.group_id, v_result_expense_id, x.member_id,
               m.member_name, x.amount, x.share_units
          FROM jsonb_to_recordset(v_request.splits)
               AS x(member_id uuid, amount numeric, share_units numeric)
          JOIN public.cloud_group_members m
            ON m.id = x.member_id AND m.group_id = v_request.group_id AND m.status = 'joined';

        INSERT INTO public.cloud_group_expense_payments (
            group_id, expense_id, member_id, member_name_snapshot,
            amount, paid_at_ms, payment_type
        )
        SELECT v_request.group_id, v_result_expense_id, x.member_id,
               m.member_name, x.amount,
               (extract(epoch FROM clock_timestamp()) * 1000)::bigint, x.payment_type
          FROM jsonb_to_recordset(v_request.payments)
               AS x(member_id uuid, amount numeric, payment_type text)
          JOIN public.cloud_group_members m
            ON m.id = x.member_id AND m.group_id = v_request.group_id AND m.status = 'joined';
    END IF;

    RETURN v_result_expense_id;
END;
$function$;

-- Submit a proposed edit. The current expense remains untouched until final approval.
CREATE OR REPLACE FUNCTION public.submit_cloud_group_expense_edit_for_approval(
    p_group_id uuid,
    p_expense_id uuid,
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
    v_split_count integer;
    v_split_distinct integer;
    v_split_total numeric;
    v_payment_count integer;
    v_payment_distinct integer;
    v_payment_total numeric;
    v_creator uuid;
    v_status text;
BEGIN
    IF v_user_id IS NULL THEN RAISE EXCEPTION 'Authentication required.'; END IF;
    IF NOT public.is_cloud_group_member(p_group_id) THEN RAISE EXCEPTION 'You are not a joined member of this group.'; END IF;
    IF p_title IS NULL OR length(trim(p_title)) NOT BETWEEN 1 AND 200 THEN RAISE EXCEPTION 'Expense title must contain 1 to 200 characters.'; END IF;
    IF p_amount IS NULL OR p_amount <= 0 OR p_amount <> round(p_amount, 2) THEN RAISE EXCEPTION 'Invalid expense amount.'; END IF;
    IF p_category IS NULL OR p_category NOT IN ('Cash', 'Online') THEN RAISE EXCEPTION 'Invalid payment type.'; END IF;
    IF p_split_mode IS NULL OR p_split_mode NOT IN ('Equal', 'Custom', 'Share') THEN RAISE EXCEPTION 'Invalid split mode.'; END IF;
    IF p_expense_date_ms IS NULL OR p_expense_date_ms <= 0 THEN RAISE EXCEPTION 'Expense date is required.'; END IF;
    IF jsonb_typeof(p_splits) IS DISTINCT FROM 'array' OR jsonb_array_length(p_splits) = 0 THEN RAISE EXCEPTION 'At least one expense split is required.'; END IF;
    IF jsonb_typeof(p_payments) IS DISTINCT FROM 'array' THEN RAISE EXCEPTION 'Invalid payment details.'; END IF;
    PERFORM 1 FROM public.cloud_group_expenses e
     WHERE e.id = p_expense_id AND e.group_id = p_group_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Expense not found in this group.'; END IF;

    SELECT count(*), count(DISTINCT x.member_id), COALESCE(sum(x.amount), 0)
      INTO v_split_count, v_split_distinct, v_split_total
      FROM jsonb_to_recordset(p_splits) AS x(member_id uuid, amount numeric, share_units numeric);
    IF v_split_count <> v_split_distinct OR v_split_total <> p_amount THEN RAISE EXCEPTION 'Invalid or unbalanced expense splits.'; END IF;
    IF EXISTS (
        SELECT 1 FROM jsonb_to_recordset(p_splits) AS x(member_id uuid, amount numeric, share_units numeric)
        LEFT JOIN public.cloud_group_members m ON m.id=x.member_id AND m.group_id=p_group_id AND m.status='joined'
        WHERE m.id IS NULL OR x.amount IS NULL OR x.amount < 0 OR x.amount <> round(x.amount,2) OR x.share_units IS NULL OR x.share_units < 0
    ) THEN RAISE EXCEPTION 'Invalid split amount or unjoined member.'; END IF;
    SELECT count(*), count(DISTINCT x.member_id), COALESCE(sum(x.amount), 0)
      INTO v_payment_count, v_payment_distinct, v_payment_total
      FROM jsonb_to_recordset(p_payments) AS x(member_id uuid, amount numeric, payment_type text);
    IF v_payment_count <> v_payment_distinct THEN RAISE EXCEPTION 'Duplicate members in expense payments.'; END IF;
    IF EXISTS (
        SELECT 1 FROM jsonb_to_recordset(p_payments) AS x(member_id uuid, amount numeric, payment_type text)
        LEFT JOIN public.cloud_group_members m ON m.id=x.member_id AND m.group_id=p_group_id AND m.status='joined'
        WHERE m.id IS NULL OR x.amount IS NULL OR x.amount <= 0 OR x.amount <> round(x.amount,2) OR x.payment_type NOT IN ('Cash','Online')
    ) THEN RAISE EXCEPTION 'Invalid payment or unjoined member.'; END IF;
    IF v_payment_count > 0 AND v_payment_total <> p_amount THEN RAISE EXCEPTION 'Payment amounts must equal the expense amount.'; END IF;
    IF lower(btrim(p_title))='settled' AND (v_split_count <> 1 OR v_payment_count <> 1) THEN RAISE EXCEPTION 'A settled expense must have exactly one receiver and one payer.'; END IF;
    IF EXISTS (
        SELECT 1 FROM jsonb_to_recordset(p_splits) AS x(member_id uuid, amount numeric, share_units numeric)
        JOIN public.cloud_group_members m ON m.id=x.member_id AND m.group_id=p_group_id AND m.status='joined'
        WHERE m.user_id IS NULL
    ) THEN RAISE EXCEPTION 'Every member required to approve must first link their account and join the group.'; END IF;

    IF p_request_id IS NULL THEN
        INSERT INTO public.cloud_group_expense_approval_requests (
            group_id, created_by, request_type, target_expense_id, title, amount,
            category, note, expense_date_ms, split_mode, splits, payments,
            status, revision, created_at_ms
        ) VALUES (
            p_group_id, v_user_id, 'edit', p_expense_id, trim(p_title), p_amount,
            p_category, COALESCE(p_note,''), p_expense_date_ms, p_split_mode,
            p_splits, p_payments, 'pending', 1,
            (extract(epoch FROM clock_timestamp())*1000)::bigint
        ) RETURNING id, revision INTO v_request_id, v_revision;
    ELSE
        SELECT created_by, status INTO v_creator, v_status
          FROM public.cloud_group_expense_approval_requests
         WHERE id=p_request_id AND group_id=p_group_id AND request_type='edit'
           AND target_expense_id=p_expense_id FOR UPDATE;
        IF NOT FOUND OR v_creator <> v_user_id OR v_status <> 'changes_required' THEN
            RAISE EXCEPTION 'Only the requester can revise an edit request returned for changes.';
        END IF;
        UPDATE public.cloud_group_expense_approval_requests
           SET title=trim(p_title), amount=p_amount, category=p_category,
               note=COALESCE(p_note,''), expense_date_ms=p_expense_date_ms,
               split_mode=p_split_mode, splits=p_splits, payments=p_payments,
               status='pending', revision=revision+1, rejection_reason=NULL,
               final_expense_id=NULL, approved_at=NULL, updated_at=now()
         WHERE id=p_request_id RETURNING id, revision INTO v_request_id, v_revision;
    END IF;

    INSERT INTO public.cloud_group_expense_approvals(request_id,group_id,revision,member_id,member_name_snapshot)
    SELECT v_request_id,p_group_id,v_revision,m.id,m.member_name
      FROM jsonb_to_recordset(p_splits) AS x(member_id uuid, amount numeric, share_units numeric)
      JOIN public.cloud_group_members m ON m.id=x.member_id AND m.group_id=p_group_id AND m.status='joined'
     WHERE m.user_id IS DISTINCT FROM v_user_id;

    IF NOT EXISTS (SELECT 1 FROM public.cloud_group_expense_approvals a WHERE a.request_id=v_request_id AND a.revision=v_revision) THEN
        PERFORM public.finalize_cloud_group_expense_approval(v_request_id);
    END IF;
    RETURN v_request_id;
END;
$function$;

-- Submit a delete request using the original expense's current shares as approvers.
CREATE OR REPLACE FUNCTION public.submit_cloud_group_expense_delete_for_approval(
    p_group_id uuid,
    p_expense_id uuid,
    p_request_id uuid DEFAULT NULL
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
DECLARE
    v_user_id uuid := auth.uid();
    v_expense public.cloud_group_expenses%ROWTYPE;
    v_splits jsonb;
    v_payments jsonb;
    v_request_id uuid;
    v_revision integer;
    v_creator uuid;
    v_status text;
BEGIN
    IF v_user_id IS NULL THEN RAISE EXCEPTION 'Authentication required.'; END IF;
    IF NOT public.is_cloud_group_member(p_group_id) THEN RAISE EXCEPTION 'You are not a joined member of this group.'; END IF;
    SELECT * INTO v_expense FROM public.cloud_group_expenses
     WHERE id=p_expense_id AND group_id=p_group_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Expense not found in this group.'; END IF;

    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'member_id',s.member_id,'amount',s.amount,'share_units',s.share_units
    )), '[]'::jsonb) INTO v_splits
      FROM public.cloud_group_expense_splits s WHERE s.expense_id=p_expense_id AND s.group_id=p_group_id;
    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'member_id',p.member_id,'amount',p.amount,'payment_type',p.payment_type
    )), '[]'::jsonb) INTO v_payments
      FROM public.cloud_group_expense_payments p WHERE p.expense_id=p_expense_id AND p.group_id=p_group_id;

    IF EXISTS (
        SELECT 1 FROM jsonb_to_recordset(v_splits) AS x(member_id uuid, amount numeric, share_units numeric)
        JOIN public.cloud_group_members m ON m.id=x.member_id AND m.group_id=p_group_id AND m.status='joined'
        WHERE m.user_id IS NULL
    ) THEN RAISE EXCEPTION 'Every member required to approve must first link their account and join the group.'; END IF;

    IF p_request_id IS NULL THEN
        INSERT INTO public.cloud_group_expense_approval_requests (
            group_id,created_by,request_type,target_expense_id,title,amount,category,note,
            expense_date_ms,split_mode,splits,payments,status,revision,created_at_ms
        ) VALUES (
            p_group_id,v_user_id,'delete',p_expense_id,v_expense.title,v_expense.amount,
            v_expense.category,v_expense.note,v_expense.expense_date_ms,v_expense.split_mode,
            v_splits,v_payments,'pending',1,(extract(epoch FROM clock_timestamp())*1000)::bigint
        ) RETURNING id,revision INTO v_request_id,v_revision;
    ELSE
        SELECT created_by,status INTO v_creator,v_status
          FROM public.cloud_group_expense_approval_requests
         WHERE id=p_request_id AND group_id=p_group_id AND request_type='delete'
           AND target_expense_id=p_expense_id FOR UPDATE;
        IF NOT FOUND OR v_creator <> v_user_id OR v_status <> 'changes_required' THEN
            RAISE EXCEPTION 'Only the requester can resubmit a delete request returned for changes.';
        END IF;
        UPDATE public.cloud_group_expense_approval_requests
           SET title=v_expense.title, amount=v_expense.amount, category=v_expense.category,
               note=v_expense.note, expense_date_ms=v_expense.expense_date_ms,
               split_mode=v_expense.split_mode, splits=v_splits, payments=v_payments,
               status='pending', revision=revision+1, rejection_reason=NULL,
               final_expense_id=NULL, approved_at=NULL, updated_at=now()
         WHERE id=p_request_id RETURNING id,revision INTO v_request_id,v_revision;
    END IF;

    INSERT INTO public.cloud_group_expense_approvals(request_id,group_id,revision,member_id,member_name_snapshot)
    SELECT v_request_id,p_group_id,v_revision,m.id,m.member_name
      FROM jsonb_to_recordset(v_splits) AS x(member_id uuid, amount numeric, share_units numeric)
      JOIN public.cloud_group_members m ON m.id=x.member_id AND m.group_id=p_group_id AND m.status='joined'
     WHERE m.user_id IS DISTINCT FROM v_user_id;

    IF NOT EXISTS (SELECT 1 FROM public.cloud_group_expense_approvals a WHERE a.request_id=v_request_id AND a.revision=v_revision) THEN
        PERFORM public.finalize_cloud_group_expense_approval(v_request_id);
    END IF;
    RETURN v_request_id;
END;
$function$;

CREATE OR REPLACE FUNCTION public.cancel_cloud_group_expense_approval_request(
    p_group_id uuid,
    p_request_id uuid
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
DECLARE
    v_user_id uuid := auth.uid();
    v_request_id uuid;
BEGIN
    IF v_user_id IS NULL THEN RAISE EXCEPTION 'Authentication required.'; END IF;
    IF NOT public.is_cloud_group_member(p_group_id) THEN
        RAISE EXCEPTION 'You are not a joined member of this group.';
    END IF;
    UPDATE public.cloud_group_expense_approval_requests
       SET status='cancelled', updated_at=now()
     WHERE id=p_request_id AND group_id=p_group_id
       AND created_by=v_user_id AND status IN ('pending','changes_required')
    RETURNING id INTO v_request_id;
    IF v_request_id IS NULL THEN RAISE EXCEPTION 'Request not found or cannot be cancelled.'; END IF;
    RETURN v_request_id;
END;
$function$;

-- Close the old direct mutation RPC routes. All future edits/deletes must use approval RPCs.
REVOKE ALL ON FUNCTION public.save_cloud_group_expense(uuid,uuid,text,numeric,text,text,bigint,text,jsonb,jsonb) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.delete_cloud_group_expense(uuid,uuid) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.finalize_cloud_group_expense_approval(uuid) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.submit_cloud_group_expense_for_approval(uuid,uuid,text,numeric,text,text,bigint,text,jsonb,jsonb) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.respond_cloud_group_expense_approval(uuid,uuid,boolean,text) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.submit_cloud_group_expense_edit_for_approval(uuid,uuid,uuid,text,numeric,text,text,bigint,text,jsonb,jsonb) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.submit_cloud_group_expense_delete_for_approval(uuid,uuid,uuid) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.cancel_cloud_group_expense_approval_request(uuid,uuid) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.submit_cloud_group_expense_for_approval(uuid,uuid,text,numeric,text,text,bigint,text,jsonb,jsonb) TO authenticated;
GRANT EXECUTE ON FUNCTION public.respond_cloud_group_expense_approval(uuid,uuid,boolean,text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_cloud_group_expense_edit_for_approval(uuid,uuid,uuid,text,numeric,text,text,bigint,text,jsonb,jsonb) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_cloud_group_expense_delete_for_approval(uuid,uuid,uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.cancel_cloud_group_expense_approval_request(uuid,uuid) TO authenticated;

-- Prevent direct table writes from bypassing the approval RPCs. SECURITY DEFINER RPCs continue to work.
REVOKE UPDATE, DELETE ON TABLE
    public.cloud_group_expenses,
    public.cloud_group_expense_splits,
    public.cloud_group_expense_payments
FROM anon, authenticated;

COMMIT;
