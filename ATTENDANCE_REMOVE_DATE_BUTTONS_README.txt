Align Ledger – Attendance UI Cleanup

Target branch: feat/online-attendance

File updated:
android/app/src/main/java/com/ledger/app/ui/AttendanceScreen.kt

Changes:
- Removed the separate "Select date" button from Owner Attendance detail.
- Removed the separate "Select date" button from Staff Attendance.
- Removed both DatePicker dialogs and their now-unused imports/state.
- Date selection now happens only by tapping a day directly in the calendar.
- Removed the full explanatory sentence from the bottom of Staff Attendance calendar:
  "Only dates explicitly marked by the owner are shown as Present or Absent."
- Kept the selected date/status and Mark Present / Mark Absent actions.

Apply from ~/Desktop/Ledger:
unzip -o ~/Downloads/align_ledger_attendance_remove_date_buttons.zip
cd android
./gradlew :app:assembleDebug

Then:
cd ~/Desktop/Ledger
git diff --check
git status --short
