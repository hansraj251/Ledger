Align Ledger – Attendance Invitation / Acceptance Workflow

IMPORTANT ORDER
1. Apply the Supabase migration first:
   supabase/migrations/20261126110000_attendance_invitation_acceptance.sql
2. Then replace the Android files from this patch and build the app.

WORKFLOW
- When the owner adds or updates a staff Gmail, register_attendance_staff creates/updates a Pending invitation.
- A staff member signs in through Profile > Attendance Login using that exact verified Google email.
- Pending invitations are fetched securely through get_my_attendance_invites().
- The Home screen shows an Attendance Invitation card with the staff name, inviting owner, and Accept Invitation action.
- Accept calls accept_my_attendance_invite(). The RPC verifies the authenticated user's confirmed email matches the invitation, then links the auth user and marks the invitation Accepted.
- Only active, linked, Accepted staff appear in the owner's Attendance Report.
- Only active, linked, Accepted staff receive the Staff Attendance card and can mark attendance.
- Pending invitations refresh on app foreground/resume, on Attendance Login, and through the existing Profile refresh action.

BACKWARD COMPATIBILITY
- Existing staff records that were already linked before this migration are backfilled as Accepted, so current attendance access is not unexpectedly removed.
- Existing pending records remain Pending and require explicit acceptance.
- If the owner changes the Gmail on an existing staff record, the old link is cleared and a new Pending invitation is created.
- Re-saving a previously declined invitation (if any) resets it to Pending.

SECURITY
- Staff invitations are returned only for the verified email of the signed-in Attendance account.
- Staff cannot accept an invitation for a different email or another staff record.
- Login/claim no longer auto-accepts invitations.
- Both staff and owner attendance RPCs require Accepted status.
- New/replaced RPCs are executable only by authenticated users.

FILES
- android/app/src/main/java/com/ledger/app/MainActivity.kt
- android/app/src/main/java/com/ledger/app/ui/HomeScreen.kt
- android/app/src/main/java/com/ledger/app/ui/StaffFormDialog.kt
- android/app/src/main/java/com/ledger/app/ui/AttendanceScreen.kt
- android/app/src/main/java/com/ledger/app/attendance/AttendanceRepository.kt
- supabase/migrations/20261126110000_attendance_invitation_acceptance.sql

No Room database schema, Google Drive backup, or Online Groups data schema changes.
The invitation is an in-app request shown after staff signs into Attendance with the invited Gmail; this patch does not send an external email or push notification.
