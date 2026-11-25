-- Align Ledger: validate settlement requests against the live pairwise balance
-- at final approval time. Existing Google Drive backup/sync is untouched.
BEGIN;

CREATE OR REPLACE FUNCTION public.cloud_group_pairwise_outstanding_paise(
    p_group_id uuid,
    p_from_member_id uuid,
    p_to_member_id uuid,
    p_exclude_expense_id uuid DEFAULT NULL
)
RETURNS bigint
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
DECLARE
    v_remaining_paise bigint;
BEGIN
    IF p_from_member_id IS NULL OR p_to_member_id IS NULL
       OR p_from_member_id = p_to_member_id THEN
        RAISE EXCEPTION 'Settlement payer and receiver must be different valid members.';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM public.cloud_group_members m
        WHERE m.id = p_from_member_id AND m.group_id = p_group_id AND m.status = 'joined'
    ) OR NOT EXISTS (
        SELECT 1 FROM public.cloud_group_members m
        WHERE m.id = p_to_member_id AND m.group_id = p_group_id AND m.status = 'joined'
    ) THEN
        RAISE EXCEPTION 'Settlement references an unknown or unjoined member.';
    END IF;

    -- Match the Kotlin calculator's joined-member and positive-payment checks.
    IF EXISTS (
        SELECT 1
        FROM public.cloud_group_expenses e
        JOIN public.cloud_group_expense_splits s
          ON s.expense_id = e.id AND s.group_id = p_group_id
        LEFT JOIN public.cloud_group_members m
          ON m.id = s.member_id AND m.group_id = p_group_id
        WHERE e.group_id = p_group_id AND e.id IS DISTINCT FROM p_exclude_expense_id
          AND (m.id IS NULL OR m.status <> 'joined' OR s.amount < 0)
    ) OR EXISTS (
        SELECT 1
        FROM public.cloud_group_expenses e
        JOIN public.cloud_group_expense_payments p
          ON p.expense_id = e.id AND p.group_id = p_group_id
        LEFT JOIN public.cloud_group_members m
          ON m.id = p.member_id AND m.group_id = p_group_id
        WHERE e.group_id = p_group_id AND e.id IS DISTINCT FROM p_exclude_expense_id
          AND (m.id IS NULL OR m.status <> 'joined' OR round(p.amount * 100) <= 0)
    ) THEN
        RAISE EXCEPTION 'An expense references an invalid amount or an unknown/unjoined member.';
    END IF;

    WITH
    scoped_expenses AS (
        SELECT e.id, e.title, e.amount
        FROM public.cloud_group_expenses e
        WHERE e.group_id = p_group_id AND e.id IS DISTINCT FROM p_exclude_expense_id
    ),
    settlement_candidates AS (
        SELECT e.id AS expense_id,
               p.member_id AS from_member_id,
               s.member_id AS to_member_id,
               round(e.amount * 100)::bigint AS amount_paise
        FROM scoped_expenses e
        JOIN LATERAL (
            SELECT count(*) AS row_count,
                   (array_agg(x.member_id))[1] AS member_id,
                   (array_agg(x.amount))[1] AS amount
            FROM public.cloud_group_expense_splits x
            WHERE x.group_id = p_group_id AND x.expense_id = e.id
        ) s ON s.row_count = 1
        JOIN LATERAL (
            SELECT count(*) AS row_count,
                   (array_agg(x.member_id))[1] AS member_id,
                   (array_agg(x.amount))[1] AS amount
            FROM public.cloud_group_expense_payments x
            WHERE x.group_id = p_group_id AND x.expense_id = e.id
        ) p ON p.row_count = 1
        WHERE lower(btrim(e.title)) = 'settled'
          AND e.amount > 0
          AND round(s.amount * 100) = round(e.amount * 100)
          AND round(p.amount * 100) = round(e.amount * 100)
          AND s.member_id <> p.member_id
    ),
    ordinary_expenses AS (
        SELECT e.id FROM scoped_expenses e
        WHERE NOT EXISTS (
            SELECT 1 FROM settlement_candidates sc WHERE sc.expense_id = e.id
        )
    ),
    expense_totals AS (
        SELECT e.id AS expense_id,
               COALESCE(s.total_paise, 0)::bigint AS total_shares_paise,
               COALESCE(p.total_paise, 0)::bigint AS total_paid_paise,
               COALESCE(s.row_count, 0)::bigint AS split_count,
               COALESCE(p.row_count, 0)::bigint AS payment_count
        FROM ordinary_expenses e
        LEFT JOIN LATERAL (
            SELECT count(*) AS row_count, sum(round(x.amount * 100)) AS total_paise
            FROM public.cloud_group_expense_splits x
            WHERE x.group_id = p_group_id AND x.expense_id = e.id
        ) s ON true
        LEFT JOIN LATERAL (
            SELECT count(*) AS row_count, sum(round(x.amount * 100)) AS total_paise
            FROM public.cloud_group_expense_payments x
            WHERE x.group_id = p_group_id AND x.expense_id = e.id
        ) p ON true
    ),
    eligible_expenses AS (
        SELECT expense_id FROM expense_totals
        WHERE split_count > 0 AND payment_count > 0
          AND total_shares_paise = total_paid_paise
    ),
    split_member_totals AS (
        SELECT s.expense_id, s.member_id,
               sum(round(s.amount * 100))::bigint AS share_paise
        FROM public.cloud_group_expense_splits s
        JOIN eligible_expenses e ON e.expense_id = s.expense_id
        WHERE s.group_id = p_group_id
        GROUP BY s.expense_id, s.member_id
    ),
    payment_member_totals AS (
        SELECT p.expense_id, p.member_id,
               sum(round(p.amount * 100))::bigint AS paid_paise
        FROM public.cloud_group_expense_payments p
        JOIN eligible_expenses e ON e.expense_id = p.expense_id
        WHERE p.group_id = p_group_id
        GROUP BY p.expense_id, p.member_id
    ),
    member_net AS (
        SELECT COALESCE(s.expense_id, p.expense_id) AS expense_id,
               COALESCE(s.member_id, p.member_id) AS member_id,
               COALESCE(p.paid_paise, 0) - COALESCE(s.share_paise, 0) AS net_paise
        FROM split_member_totals s
        FULL JOIN payment_member_totals p
          ON p.expense_id = s.expense_id AND p.member_id = s.member_id
    ),
    creditors AS (
        SELECT expense_id, member_id, net_paise AS credit_paise
        FROM member_net WHERE net_paise > 0
    ),
    debtors AS (
        SELECT expense_id, member_id, -net_paise AS debt_paise
        FROM member_net WHERE net_paise < 0
    ),
    total_credit AS (
        SELECT expense_id, sum(credit_paise)::numeric AS total_credit_paise
        FROM creditors GROUP BY expense_id
    ),
    allocation_raw AS (
        SELECT d.expense_id, d.member_id AS debtor_id, d.debt_paise,
               c.member_id AS creditor_id,
               floor((d.debt_paise::numeric * c.credit_paise::numeric)
                     / tc.total_credit_paise)::bigint AS base_paise,
               mod(d.debt_paise::numeric * c.credit_paise::numeric,
                   tc.total_credit_paise) AS fractional_remainder
        FROM debtors d
        JOIN creditors c ON c.expense_id = d.expense_id
        JOIN total_credit tc ON tc.expense_id = d.expense_id
    ),
    allocation_ranked AS (
        SELECT a.*,
               row_number() OVER (
                   PARTITION BY a.expense_id, a.debtor_id
                   ORDER BY a.fractional_remainder DESC, a.creditor_id::text ASC
               ) AS remainder_rank,
               sum(a.base_paise) OVER (
                   PARTITION BY a.expense_id, a.debtor_id
               ) AS base_total_paise
        FROM allocation_raw a
    ),
    allocations AS (
        SELECT expense_id, debtor_id, creditor_id,
               base_paise + CASE
                   WHEN remainder_rank <= debt_paise - base_total_paise THEN 1
                   ELSE 0
               END AS allocated_paise
        FROM allocation_ranked
    ),
    base_pairwise AS (
        SELECT COALESCE(sum(CASE
            WHEN debtor_id = p_from_member_id AND creditor_id = p_to_member_id
                THEN allocated_paise
            WHEN debtor_id = p_to_member_id AND creditor_id = p_from_member_id
                THEN -allocated_paise
            ELSE 0 END), 0)::bigint AS signed_base_paise
        FROM allocations
    ),
    settlement_pairwise AS (
        SELECT COALESCE(sum(CASE
            WHEN from_member_id = p_from_member_id AND to_member_id = p_to_member_id
                THEN amount_paise
            WHEN from_member_id = p_to_member_id AND to_member_id = p_from_member_id
                THEN -amount_paise
            ELSE 0 END), 0)::bigint AS signed_settlement_paise
        FROM settlement_candidates
    )
    SELECT greatest(b.signed_base_paise - s.signed_settlement_paise, 0)::bigint
    INTO v_remaining_paise
    FROM base_pairwise b CROSS JOIN settlement_pairwise s;

    RETURN COALESCE(v_remaining_paise, 0);
END;
$function$;

REVOKE ALL ON FUNCTION public.cloud_group_pairwise_outstanding_paise(uuid, uuid, uuid, uuid)
    FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION public.finalize_cloud_group_expense_approval(p_request_id uuid)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
DECLARE
    v_request public.cloud_group_expense_approval_requests%ROWTYPE;
    v_result_expense_id uuid;
    v_settlement_receiver_id uuid;
    v_settlement_payer_id uuid;
    v_split_amount numeric;
    v_payment_amount numeric;
    v_requested_paise bigint;
    v_remaining_paise bigint;
BEGIN
    SELECT * INTO v_request
    FROM public.cloud_group_expense_approval_requests
    WHERE id = p_request_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Expense approval request not found.';
    END IF;

    -- Serialize finalization and balance checks within this group.
    PERFORM 1 FROM public.cloud_groups
    WHERE id = v_request.group_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Group not found.';
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

    -- Validate only at final approval, after all approvals are complete.
    IF v_request.request_type IN ('add', 'edit')
       AND lower(btrim(v_request.title)) = 'settled' THEN
        IF jsonb_typeof(v_request.splits) IS DISTINCT FROM 'array'
           OR jsonb_typeof(v_request.payments) IS DISTINCT FROM 'array' THEN
            RAISE EXCEPTION 'A settled expense must have exactly one receiver and one payer.';
        END IF;
        IF jsonb_array_length(v_request.splits) <> 1
           OR jsonb_array_length(v_request.payments) <> 1 THEN
            RAISE EXCEPTION 'A settled expense must have exactly one receiver and one payer.';
        END IF;

        SELECT x.member_id, x.amount INTO v_settlement_receiver_id, v_split_amount
        FROM jsonb_to_recordset(v_request.splits) AS x(member_id uuid, amount numeric);
        SELECT x.member_id, x.amount INTO v_settlement_payer_id, v_payment_amount
        FROM jsonb_to_recordset(v_request.payments) AS x(member_id uuid, amount numeric);

        IF v_settlement_receiver_id IS NULL OR v_settlement_payer_id IS NULL
           OR v_settlement_receiver_id = v_settlement_payer_id
           OR v_request.amount IS NULL OR v_request.amount <= 0
           OR v_split_amount IS NULL OR v_payment_amount IS NULL
           OR round(v_request.amount * 100) <= 0
           OR round(v_split_amount * 100) <> round(v_request.amount * 100)
           OR round(v_payment_amount * 100) <> round(v_request.amount * 100) THEN
            RAISE EXCEPTION 'Invalid settlement request. Payer, receiver and amounts must match.';
        END IF;

        v_requested_paise := round(v_request.amount * 100)::bigint;
        v_remaining_paise := public.cloud_group_pairwise_outstanding_paise(
            v_request.group_id,
            v_settlement_payer_id,
            v_settlement_receiver_id,
            CASE WHEN v_request.request_type = 'edit'
                 THEN v_request.target_expense_id ELSE NULL END
        );

        IF v_requested_paise > v_remaining_paise THEN
            RAISE EXCEPTION
                'Settlement exceeds actual remaining balance. Remaining balance: %',
                to_char(v_remaining_paise::numeric / 100,
                        'FM999999999999990.00');
        END IF;
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

REVOKE ALL ON FUNCTION public.finalize_cloud_group_expense_approval(uuid)
    FROM PUBLIC, anon, authenticated;

COMMIT;
