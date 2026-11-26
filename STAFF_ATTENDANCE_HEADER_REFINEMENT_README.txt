Align Ledger – Staff Attendance header refinement

File replaced:
android/app/src/main/java/com/ledger/app/ui/AttendanceScreen.kt

Changes:
- Staff name and email now appear on the same row; name is left-aligned and email is right-aligned.
- Removed the standalone "Mark Attendance" heading below the email.
- Kept the actual "Mark Attendance" action button unchanged.
- Replaced the text "Refresh" action in the Staff Attendance top bar with a refresh icon button.
- Refresh icon is disabled while attendance data is loading.

No Supabase SQL, database schema, or repository changes.
