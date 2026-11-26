Align Ledger Online Attendance – foundation files

Files:
- supabase/migrations/20261126090000_online_attendance_foundation.sql
- android/app/src/main/java/com/ledger/app/attendance/AttendanceSupabaseClient.kt
- android/app/src/main/java/com/ledger/app/attendance/AttendanceAuthManager.kt

Important:
1. Copy these files into the matching paths in ~/Desktop/Ledger.
2. Do not apply the SQL migration to the production Supabase project yet.
3. Do not modify Room version 41 or the existing CloudSupabaseClient.
4. Build/test the Android project after copying. The owner/staff UI and repository calls
   are intentionally not part of this first foundation drop.
5. The attendance client stores its Auth session under a dedicated SettingsSessionManager
   key, separate from the Online Groups client session.
