
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

    IF v_split_count <> v_split_distinct OR v_split_total <> p_amount THEN
        RAISE EXCEPTION 'Invalid or unbalanced expense splits.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM jsonb_to_recordset(p_splits) AS x(member_id uuid, amount numeric, share_units numeric)
        LEFT JOIN public.cloud_group_members m
          ON m.id=x.member_id AND m.group_id=p_group_id AND m.status='joined'
        WHERE m.id IS NULL OR x.amount IS NULL OR x.amount < 0
           OR x.amount <> round(x.amount,2)
           OR x.share_units IS NULL OR x.share_units < 0
    ) THEN
        RAISE EXCEPTION 'Invalid split amount or unjoined member.';
    END IF;

    SELECT count(*), count(DISTINCT x.member_id), COALESCE(sum(x.amount), 0)
      INTO v_payment_count, v_payment_distinct, v_payment_total
      FROM jsonb_to_recordset(p_payments) AS x(member_id uuid, amount numeric, payment_type text);

    IF v_payment_count <> v_payment_distinct THEN
        RAISE EXCEPTION 'Duplicate members in expense payments.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM jsonb_to_recordset(p_payments) AS x(member_id uuid, amount numeric, payment_type text)
        LEFT JOIN public.cloud_group_members m
          ON m.id=x.member_id AND m.group_id=p_group_id AND m.status='joined'
        WHERE m.id IS NULL
           OR x.amount IS NULL
           OR x.amount <= 0
           OR x.amount <> round(x.amount,2)
           OR x.payment_type IS NULL
           OR x.payment_type NOT IN ('Cash','Online')
    ) THEN
        RAISE EXCEPTION 'Invalid payment or unjoined member.';
    END IF;

    IF v_payment_count > 0 AND v_payment_total <> p_amount THEN
        RAISE EXCEPTION 'Payment amounts must equal the expense amount.';
    END IF;

    IF lower(btrim(p_title))='settled' AND (v_split_count <> 1 OR v_payment_count <> 1) THEN
        RAISE EXCEPTION 'A settled expense must have exactly one receiver and one payer.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM jsonb_to_recordset(p_splits) AS x(member_id uuid, amount numeric, share_units numeric)
        JOIN public.cloud_group_members m
          ON m.id=x.member_id AND m.group_id=p_group_id AND m.status='joined'
        WHERE m.user_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Every member required to approve must first link their account and join the group.';
    END IF;

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
         WHERE id=p_request_id
         RETURNING id, revision INTO v_request_id, v_revision;
    END IF;

    INSERT INTO public.cloud_group_expense_approvals(
        request_id,group_id,revision,member_id,member_name_snapshot
    )
    SELECT v_request_id,p_group_id,v_revision,m.id,m.member_name
      FROM jsonb_to_recordset(p_splits) AS x(member_id uuid, amount numeric, share_units numeric)
      JOIN public.cloud_group_members m
        ON m.id=x.member_id AND m.group_id=p_group_id AND m.status='joined'
     WHERE m.user_id IS DISTINCT FROM v_user_id;

    IF NOT EXISTS (
        SELECT 1 FROM public.cloud_group_expense_approvals a
        WHERE a.request_id=v_request_id AND a.revision=v_revision
    ) THEN
        PERFORM public.finalize_cloud_group_expense_approval(v_request_id);
    END IF;

    RETURN v_request_id;
END;
$function$;
