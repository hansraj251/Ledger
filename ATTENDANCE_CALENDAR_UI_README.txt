Align Ledger – Staff Attendance UI Calendar Patch

Target branch: feat/online-attendance
File:
android/app/src/main/java/com/ledger/app/ui/AttendanceScreen.kt

Changes:
- Removed the "Welcome to Attendance" card.
- Added date selection to the Mark Attendance card.
- Replaced the Attendance History list with a monthly calendar.
- Green dates = attendance record exists (Present).
- Red past dates = no attendance record exists (shown as Absent / not marked).
- White background and subtle border applied to all cards on Staff Attendance screen.

Important backend constraint:
The current mark_my_attendance RPC marks attendance for the server's current date only.
Therefore the date picker supports selecting/reviewing past dates, but the Mark Attendance
action is available only for today. This patch does not weaken backend date validation.

Red does not prove an employee was absent or on leave; it means no attendance mark is
recorded for that past date. Holidays/weekly offs are not currently excluded by this UI.

Apply from ~/Desktop/Ledger:
unzip -o ~/Downloads/align_ledger_attendance_calendar_patch.zip
cd android
./gradlew :app:assembleDebug
