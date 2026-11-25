ALIGN LEDGER — GROUP EXPENSE APPROVAL WORKFLOW PATCH

Base branch: feat/ledger-room-foundation
Base commit: b98d862372b377871ad21702a1d83afa46763291

CONTENTS
- supabase/migrations/20261125_group_expense_approval_workflow.sql
- Android Kotlin source changes for pending requests, approvals, reject/resubmit, and repository tests.

IMPORTANT DEPLOYMENT ORDER
1. Take a Supabase database backup.
2. Review and run the SQL migration in Supabase SQL Editor.
3. Confirm the migration completes successfully and verify the new tables/RPCs exist.
4. Copy the android/ folder from this patch over the matching android/ folder in ~/Desktop/Ledger.
5. From ~/Desktop/Ledger/android run:
     ./gradlew testDebugUnitTest
     ./gradlew assembleDebug
6. Install and test the debug build before any Play Store release.

BEHAVIOUR IMPLEMENTED
- Normal expenses: every member in the split must approve; creator is automatically approved.
- Settled expenses: only the single receiver (the split member) must approve; payer is not an approver.
- If any required member rejects, request becomes changes_required and returns to creator.
- Creator edits and resubmits; approvals are recorded against a new revision, so previous approvals do not carry forward.
- Only after all required approvals does the server create the final expense/split/payment rows.
- Pending requests are not included in the final expense tables, balances, or reports.
- Existing final expense updates remain on the existing update RPC path. New expense inserts are blocked unless an approved request is being finalized.
- Existing Google Drive backup/restore/auto-sync source files are not changed.

ACCOUNT LINKING REQUIREMENT
Every non-creator member who must approve must have a linked account (cloud_group_members.user_id) and joined status. If a required member has not linked their account, the server rejects submission instead of creating an approval that nobody can action.

LIMITATIONS / VERIFICATION
- This environment has no network access to download the project's Gradle 9.7.1 distribution. Gradle tests and assembleDebug could not be run here.
- SQL was prepared from the schema/function/policy/trigger definitions supplied for this database, but it has not been executed against the live Supabase project.
- No push notifications are added; approvers see pending requests when they open/refresh the group.
- Final expenses that are edited after approval continue to use the current edit path; this workflow gates new submissions and resubmissions.

Do not deploy this patch as production-ready until the SQL migration has been applied and Android unit tests/build plus manual multi-account approval tests have passed.
