Align Ledger Online Attendance — Phase 2A patch

This patch adds owner-side staff Gmail registration to the existing Staff add/edit forms.

Included files:
- android/app/src/main/java/com/ledger/app/ui/StaffFormDialog.kt
- android/app/src/main/java/com/ledger/app/ui/HomeScreen.kt
- android/app/src/main/java/com/ledger/app/ui/StaffDetailScreen.kt
- android/app/src/main/java/com/ledger/app/attendance/AttendanceOwnerRepository.kt (new)

What changed:
1. Add Staff now has an optional staff email field for online attendance.
2. If an email is supplied, the owner must already be signed in through the existing Cloud/Online Groups Google account.
3. The local Room staff row is created first to obtain its local ID, then the owner session calls register_attendance_staff.
4. If online registration fails during Add Staff, the just-created local staff row and starting salary revision are rolled back. The form stays open and shows the error.
5. Edit Staff also exposes the optional email field, allowing an existing local staff row to be registered or its email mapping updated.
6. Staff email is stored in Supabase, not added to Room; Room database version remains unchanged.
7. Staff attendance authentication continues to use AttendanceSupabaseClient's separate session key.

Apply:
1. Extract this ZIP over the existing repository root (~/Desktop/Ledger), preserving folder paths.
2. From Terminal:
   cd ~/Desktop/Ledger/android
   ./gradlew :app:assembleDebug

Important:
- This is Phase 2A only: owner-side email registration.
- It does not yet add the Staff Google sign-in/claim screen, attendance marking UI, or owner attendance reports.
- The Supabase migration from the Foundation ZIP must be applied to the Supabase project before online registration can work.
- Do not change Room database version 41 or CloudSupabaseClient session configuration.
- Build could not be executed in this environment because the Gradle distribution download was blocked by unavailable network access. Run the build locally and share the complete output if it fails.
