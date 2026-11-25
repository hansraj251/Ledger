BEGIN;

CREATE OR REPLACE FUNCTION public.delete_cloud_group(p_group_id uuid)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
DECLARE
    v_owner_id uuid;
    v_pending text;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;

    -- Lock and verify the group before any deletion.
    SELECT owner_id
      INTO v_owner_id
      FROM public.cloud_groups
     WHERE id = p_group_id
     FOR UPDATE;

    IF NOT FOUND OR v_owner_id IS DISTINCT FROM auth.uid() THEN
        RAISE EXCEPTION 'Only the group owner can delete this group.';
    END IF;

    -- Verify that every joined member has exactly zero outstanding balance.
    SELECT string_agg(
        format(
            '%s: Rs. %s',
            m.member_name,
            to_char(
                b.remaining_paise / 100,
                'FM999999999999990.00'
            )
        ),
        '; '
    )
    INTO v_pending
    FROM public.cloud_group_members m
    CROSS JOIN LATERAL (
        SELECT
            COALESCE((
                SELECT SUM(round(s.amount * 100))
                FROM public.cloud_group_expense_splits s
                WHERE s.group_id = p_group_id
                  AND s.member_id = m.id
            ), 0)
            -
            COALESCE((
                SELECT SUM(round(p.amount * 100))
                FROM public.cloud_group_expense_payments p
                WHERE p.group_id = p_group_id
                  AND p.member_id = m.id
            ), 0)
            -
            COALESCE((
                SELECT SUM(round(st.amount * 100))
                FROM public.cloud_group_settlements st
                WHERE st.group_id = p_group_id
                  AND st.member_id = m.id
            ), 0) AS remaining_paise
    ) b
    WHERE m.group_id = p_group_id
      AND m.status = 'joined'
      AND b.remaining_paise <> 0;

    IF v_pending IS NOT NULL THEN
        RAISE EXCEPTION
            'Group cannot be deleted. Members are not settled: %',
            v_pending;
    END IF;

    -- Delete approval children explicitly while the parent group exists.
    DELETE FROM public.cloud_group_expense_approvals
    WHERE group_id = p_group_id;

    DELETE FROM public.cloud_group_expense_approval_requests
    WHERE group_id = p_group_id;

    -- This table has NO ACTION foreign keys, so delete it explicitly.
    DELETE FROM public.cloud_group_settlement_transactions
    WHERE group_id = p_group_id;

    DELETE FROM public.cloud_group_expense_payments
    WHERE group_id = p_group_id;

    DELETE FROM public.cloud_group_expense_splits
    WHERE group_id = p_group_id;

    DELETE FROM public.cloud_group_settlements
    WHERE group_id = p_group_id;

    DELETE FROM public.cloud_group_expenses
    WHERE group_id = p_group_id;

    DELETE FROM public.cloud_group_members
    WHERE group_id = p_group_id;

    DELETE FROM public.cloud_groups
    WHERE id = p_group_id;

    RETURN p_group_id;
END;
$function$;

COMMIT;
