-- Align Ledger: include Paid By members in Add/Edit/Delete approval requests.
-- Approvers are the union of split members and payment (Paid By) members,
-- excluding the request creator. Duplicate member rows are ignored.
-- Applies to new requests and resubmissions. Existing pending requests are backfilled.

BEGIN;

CREATE OR REPLACE FUNCTION public.add_paid_by_members_to_group_expense_approvals()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
BEGIN
    -- Only pending requests need approver rows. This fires on initial insert
    -- and on resubmission (where the revision is incremented).
    IF NEW.status <> 'pending' THEN
        RETURN NEW;
    END IF;

    -- A non-creator Paid By member must have a linked account to approve.
    IF EXISTS (
        SELECT 1
          FROM jsonb_to_recordset(COALESCE(NEW.payments, '[]'::jsonb))
               AS x(member_id uuid, amount numeric, payment_type text)
          LEFT JOIN public.cloud_group_members m
            ON m.id = x.member_id
           AND m.group_id = NEW.group_id
           AND m.status = 'joined'
         WHERE m.id IS NULL
            OR (m.user_id IS NULL AND m.user_id IS DISTINCT FROM NEW.created_by)
    ) THEN
        RAISE EXCEPTION 'Every Paid By member required to approve must be linked to an account and joined to the group.';
    END IF;

    INSERT INTO public.cloud_group_expense_approvals (
        request_id, group_id, revision, member_id, member_name_snapshot
    )
    SELECT DISTINCT
        NEW.id, NEW.group_id, NEW.revision, m.id, m.member_name
      FROM jsonb_to_recordset(COALESCE(NEW.payments, '[]'::jsonb))
           AS x(member_id uuid, amount numeric, payment_type text)
      JOIN public.cloud_group_members m
        ON m.id = x.member_id
       AND m.group_id = NEW.group_id
       AND m.status = 'joined'
     WHERE m.user_id IS DISTINCT FROM NEW.created_by
       -- Split-member approvals are inserted by the existing submit RPCs.
       -- Add only payment members who are not already represented in splits.
       AND NOT EXISTS (
           SELECT 1
             FROM jsonb_to_recordset(COALESCE(NEW.splits, '[]'::jsonb))
                  AS s(member_id uuid, amount numeric, share_units numeric)
            WHERE s.member_id = m.id
       )
    ON CONFLICT (request_id, revision, member_id) DO NOTHING;

    RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_add_paid_by_members_to_expense_approvals
    ON public.cloud_group_expense_approval_requests;

CREATE TRIGGER trg_add_paid_by_members_to_expense_approvals
AFTER INSERT OR UPDATE ON public.cloud_group_expense_approval_requests
FOR EACH ROW
WHEN (NEW.status = 'pending')
EXECUTE FUNCTION public.add_paid_by_members_to_group_expense_approvals();

-- Backfill Paid By approvers for requests that were already pending when this
-- migration is applied. Existing split-member approval rows remain unchanged.
INSERT INTO public.cloud_group_expense_approvals (
    request_id, group_id, revision, member_id, member_name_snapshot
)
SELECT DISTINCT
    r.id, r.group_id, r.revision, m.id, m.member_name
  FROM public.cloud_group_expense_approval_requests r
  CROSS JOIN LATERAL jsonb_to_recordset(COALESCE(r.payments, '[]'::jsonb))
       AS x(member_id uuid, amount numeric, payment_type text)
  JOIN public.cloud_group_members m
    ON m.id = x.member_id
   AND m.group_id = r.group_id
   AND m.status = 'joined'
 WHERE r.status = 'pending'
   AND m.user_id IS DISTINCT FROM r.created_by
   -- The existing split approval row already covers members in both sets.
   AND NOT EXISTS (
       SELECT 1
         FROM jsonb_to_recordset(COALESCE(r.splits, '[]'::jsonb))
              AS s(member_id uuid, amount numeric, share_units numeric)
        WHERE s.member_id = m.id
   )
ON CONFLICT (request_id, revision, member_id) DO NOTHING;

COMMIT;

-- Verification query (run separately after applying):
-- SELECT r.id, r.request_type, r.status, r.revision,
--        a.member_id, a.member_name_snapshot, a.approval_status
-- FROM public.cloud_group_expense_approval_requests r
-- JOIN public.cloud_group_expense_approvals a
--   ON a.request_id = r.id AND a.revision = r.revision
-- WHERE r.status = 'pending'
-- ORDER BY r.created_at DESC, a.member_name_snapshot;
