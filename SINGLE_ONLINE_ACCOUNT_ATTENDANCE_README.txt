Align Ledger – Single Ledger Online account for attendance

Replace these files:
- android/app/src/main/java/com/ledger/app/MainActivity.kt
- android/app/src/main/java/com/ledger/app/attendance/AttendanceRepository.kt
- android/app/src/main/java/com/ledger/app/ui/AttendanceScreen.kt

Changes:
1. Removes the separate Attendance Google sign-in / sign-out flow from MainActivity.
2. Removes the separate "Staff Attendance Login" card from Profile. The existing
   Ledger Online / Online Groups account card is the only account connection UI.
3. AttendanceRepository now uses CloudSupabaseClient.client for owner and staff
   operations, so invitations, acceptance, attendance reading and marking use
   the same signed-in account as Online Groups.
4. StaffAttendanceScreen no longer restores an AttendanceAuthManager session;
   it checks the current Ledger Online account.
5. Attendance invitation data refreshes when returning from Profile and when
   the app resumes.
6. Owner Attendance Report visibility is checked against attendance_workplaces,
   not attendance_staff. This avoids treating a staff member's own accepted
   staff row as proof that the account is a workplace owner.

Account behavior:
- The staff member must connect Ledger Online using the same Gmail address
  entered by the owner in the staff invitation.
- Pending invitations appear on Home for that connected account.
- After acceptance, only the Staff Attendance card appears for a staff-only
  account.
- The Owner Attendance Report entry appears only for an account that owns an
  attendance workplace.

No new SQL migration is included. Keep the previously applied attendance
foundation, invitation-acceptance and owner-delete migrations as they are.
