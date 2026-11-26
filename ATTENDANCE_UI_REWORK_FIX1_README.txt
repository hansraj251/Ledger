Attendance UI Rework — Fix 1

Fix:
- Replaced the unavailable EventAvailable Material icon reference with the already-supported Person icon in MainActivity.kt and HomeScreen.kt. This avoids relying on an icon artifact not present in the current project dependencies.

Apply from ~/Desktop/Ledger:
  unzip -o ~/Downloads/align_ledger_attendance_ui_rework_fix1.zip
  cd android
  ./gradlew :app:assembleDebug

No database, Supabase, Room, Online Groups auth, or Drive sync changes.
