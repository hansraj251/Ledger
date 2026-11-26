Align Ledger — Prevent Owner Self-Registration as Attendance Staff

What this patch does
1. Compares the staff Gmail with the currently connected Ledger Online account email (trimmed, case-insensitive).
2. Stops the app from sending register_attendance_staff when the emails match.
3. Adds a Supabase migration that repeats the check against auth.users inside the SECURITY DEFINER RPC. This is the authoritative protection if a client-side check is bypassed.
4. Does not change Staff Attendance visibility, Home card visibility, Owner Attendance Report filtering, or existing attendance records.

Files
- android/app/src/main/java/com/ledger/app/attendance/AttendanceOwnerRepository.kt
- android/app/src/test/java/com/ledger/app/attendance/AttendanceOwnerEmailValidationTest.kt
- supabase/migrations/20261126130000_prevent_owner_self_attendance_registration.sql

Apply Android patch
From ~/Desktop/Ledger:
  unzip -o ~/Downloads/align_ledger_prevent_owner_self_attendance.zip
  cd android
  ./gradlew testDebugUnitTest
  ./gradlew :app:assembleDebug
  ./gradlew installDebug

Apply database migration
1. Open Supabase Dashboard for the Ledger project.
2. Open SQL Editor -> New query.
3. Paste the complete contents of:
   supabase/migrations/20261126130000_prevent_owner_self_attendance_registration.sql
4. Run it and confirm Success.

Important
- The database migration is required for server-side enforcement. Installing the APK alone does not secure direct RPC calls.
- Existing self-registered online attendance rows are not deleted by this migration.
- This patch does not modify the local Room staff list or delete any cloud rows.
