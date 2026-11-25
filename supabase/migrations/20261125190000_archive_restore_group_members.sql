-- Allow archived members.
ALTER TABLE public.cloud_group_members
    DROP CONSTRAINT cloud_group_members_status_check;

ALTER TABLE public.cloud_group_members
    ADD CONSTRAINT cloud_group_members_status_check
    CHECK (status IN ('pending', 'joined', 'removed'));

-- Preserve the original identity state for archived members.
ALTER TABLE public.cloud_group_members
    DROP CONSTRAINT cloud_group_members_check;

ALTER TABLE public.cloud_group_members
    ADD CONSTRAINT cloud_group_members_check
    CHECK (
        (status = 'pending'
            AND user_id IS NULL
            AND joined_at IS NULL)
        OR
        (status = 'joined'
            AND user_id IS NOT NULL
            AND joined_at IS NOT NULL)
        OR
        (status = 'removed'
            AND (
                (user_id IS NULL AND joined_at IS NULL)
                OR
                (user_id IS NOT NULL AND joined_at IS NOT NULL)
            ))
    );

-- Archive a member instead of permanently deleting the row.
CREATE OR REPLACE FUNCTION public.remove_cloud_group_member(
    p_group_id uuid,
    p_member_id uuid
)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
DECLARE
    v_member_id uuid;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Authentication required.';
    END IF;

    IF NOT public.is_cloud_group_owner(p_group_id) THEN
        RAISE EXCEPTION 'Only the group owner can remove members.';
    END IF;

    UPDATE public.cloud_group_members
       SET status = 'removed'
     WHERE id = p_member_id
       AND group_id = p_group_id
       AND role = 'member'
       AND status IN ('pending', 'joined')
    RETURNING id INTO v_member_id;

    IF v_member_id IS NULL THEN
        RAISE EXCEPTION 'Member not found or already removed.';
    END IF;

    RETURN v_member_id;
END;
$function$;
