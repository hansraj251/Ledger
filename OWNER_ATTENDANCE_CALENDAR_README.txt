Align Ledger – Owner Attendance Calendar & Manual Status Patch

Target branch: feat/online-attendance

Files:
1. android/app/src/main/java/com/ledger/app/ui/AttendanceScreen.kt
2. android/app/src/main/java/com/ledger/app/attendance/AttendanceRepository.kt
3. supabase/migrations/20261126100000_owner_attendance_status.sql

Owner-side UI:
- Removes the Monthly Attendance summary card.
- Shows clean white staff cards with Present/Absent totals and today's status.
- Tapping a staff card opens that staff member's monthly attendance calendar.
- Owner can select any date (past, today, or future) and mark Present or Absent.
- Owner can change an existing status; the selected date is highlighted.
- The calendar displays explicit Present dates in green and explicit Absent dates in red.
- Staff-side calendar remains available and reads the same Present/Absent status.

Backend:
- Adds attendance_records.status (default 'present') without deleting existing records.
- Adds SECURITY DEFINER RPC set_owner_attendance(uuid, date, text).
- RPC checks auth.uid() owns the selected staff member's workplace before upserting.
- Only authenticated users can execute the RPC; anon and PUBLIC are revoked.
- Existing mark_my_attendance() remains unchanged and continues to self-mark today's attendance.

IMPORTANT – Apply SQL before installing/testing the app:
1. Open Supabase Dashboard → SQL Editor.
2. Run the complete file:
   supabase/migrations/20261126100000_owner_attendance_status.sql
3. Confirm it returns Success.
4. Then apply this ZIP from ~/Desktop/Ledger.

Terminal:
cd ~/Desktop/Ledger
unzip -o ~/Downloads/align_ledger_owner_attendance_patch.zip
cd android
./gradlew :app:assembleDebug

After successful build:
cd ~/Desktop/Ledger
git diff --check
git status --short

Do not commit/push until the SQL migration and Android build have both been verified.

Note:
- A past date with no record is shown as unmarked/neutral, not automatically Absent.
- Red means an explicit Absent status saved by the owner.
- The owner can mark future dates too, as requested. Consider whether future-date marking is operationally appropriate before using it in production.
