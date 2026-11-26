Align Ledger Online Attendance — Phase 2B

Included:
- android/app/src/main/java/com/ledger/app/attendance/AttendanceRepository.kt
- android/app/src/main/java/com/ledger/app/ui/AttendanceScreen.kt
- android/app/src/main/java/com/ledger/app/ui/HomeScreen.kt
- android/app/src/main/java/com/ledger/app/MainActivity.kt
- android/app/src/main/java/com/ledger/app/ui/StaffDetailScreen.kt

Features:
1. Attendance entry point from the Home screen.
2. Owner monthly view reads attendance staff and attendance records through the existing CloudSupabaseClient session.
3. Staff Google sign-in uses AttendanceAuthManager and the dedicated AttendanceSupabaseClient session key.
4. Staff login calls claim_my_attendance_staff and displays linked active staff records.
5. Staff can mark today's attendance using mark_my_attendance; the database RPC enforces one record per staff per workplace-local date.
6. Staff sign-out only signs out the Attendance Supabase client.
7. Removed the duplicate StaffBonusEntity branch warning in StaffDetailScreen.

Important:
- This phase assumes the foundation SQL migration has been reviewed and applied to Supabase before runtime use.
- Do not run the migration until reviewing the complete SQL and confirming the Supabase project.
- This phase has not been built on the developer's Mac; run ./gradlew :app:assembleDebug.
- Owner report shows marks recorded, not inferred absences. Weekly offs, holidays, and joining dates are not yet used to calculate absent days.
- No Room schema or version change.
- Existing CloudSupabaseClient and Online Groups session settings are not changed.
