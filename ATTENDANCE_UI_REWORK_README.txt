Align Ledger — Attendance UI Rework

Changes:
1. Attendance Report screen now contains only the Owner Monthly Report. Staff Login section has been removed from this screen.
2. Staff Login has moved to Profile, directly below the Ledger Online (CloudAccountCard) card.
3. Profile login uses the isolated AttendanceAuthManager / AttendanceSupabaseClient session. It does not sign out or overwrite Online Groups.
4. Profile shows the signed-in staff Gmail, linked-registration status, retry registration check, and sign-out.
5. On app startup, an existing Attendance session is restored and matching staff registrations are refreshed.
6. Home keeps an Attendance Report entry for the owner.
7. A separate Staff Attendance card appears on Home only when the Attendance account has at least one active linked staff registration.
8. Opening Staff Attendance allows the employee to mark attendance and view monthly attendance history. No login controls are shown on that screen.
9. Multiple workplace registrations linked to the same verified Gmail are supported.

Files:
- android/app/src/main/java/com/ledger/app/MainActivity.kt
- android/app/src/main/java/com/ledger/app/ui/HomeScreen.kt
- android/app/src/main/java/com/ledger/app/ui/AttendanceScreen.kt

Apply:
From ~/Desktop/Ledger:
  unzip -o ~/Downloads/align_ledger_attendance_ui_rework_patch.zip
  cd android
  ./gradlew :app:assembleDebug

No SQL migration, Room entity, Room schema version, Online Groups session, or Google Drive sync changes are included.

Build note:
This patch has not been built on the user's Mac. Run the Gradle command above and share the complete output before installing/testing.
