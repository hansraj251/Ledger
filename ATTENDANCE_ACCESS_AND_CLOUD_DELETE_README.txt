Align Ledger – Attendance access visibility and cloud staff deletion

Files:
- android/app/src/main/java/com/ledger/app/MainActivity.kt
- android/app/src/main/java/com/ledger/app/ui/HomeScreen.kt
- android/app/src/main/java/com/ledger/app/ui/AttendanceScreen.kt
- android/app/src/main/java/com/ledger/app/attendance/AttendanceRepository.kt
- supabase/migrations/20261126120000_owner_delete_online_attendance_staff.sql

Changes:
1. The Home screen shows the "Attendance Report" entry only when the existing
   Online Groups/owner Google account is connected. Staff-only attendance login
   does not expose the owner report entry.
2. Accepted staff continue to see only the "Staff Attendance" card; pending
   invitations continue to appear as invitation cards until accepted.
3. Long-press an accepted staff card in the owner's Attendance Report to open a
   permanent-delete confirmation dialog.
4. Confirming deletion calls an owner-authorized Supabase RPC. It deletes all
   attendance_records for that cloud staff ID and then deletes the matching
   attendance_staff row (including the accepted invitation/link).
5. The RPC checks auth.uid() against attendance_workplaces.owner_id and cannot
   delete another owner's staff.
6. The local Room staff record is not accessed or deleted by this workflow.
   The staff remains in the local Ledger staff list.

Apply the SQL migration before installing the Android patch.
This does not delete the staff member's Google/Auth account.
