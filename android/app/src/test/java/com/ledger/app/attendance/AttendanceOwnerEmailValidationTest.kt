package com.ledger.app.attendance

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceOwnerEmailValidationTest {

    @Test
    fun sameEmailIsRejectedIgnoringCaseAndWhitespace() {
        assertTrue(
            isSameAttendanceEmail(
                ownerEmail = "  Owner@Gmail.com ",
                staffEmail = "owner@gmail.com"
            )
        )
    }

    @Test
    fun differentEmailIsAllowed() {
        assertFalse(
            isSameAttendanceEmail(
                ownerEmail = "owner@gmail.com",
                staffEmail = "staff@gmail.com"
            )
        )
    }

    @Test
    fun missingOwnerEmailDoesNotCreateFalseMatch() {
        assertFalse(isSameAttendanceEmail(ownerEmail = null, staffEmail = "staff@gmail.com"))
    }
}
