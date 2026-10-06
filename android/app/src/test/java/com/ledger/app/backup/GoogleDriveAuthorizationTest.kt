package com.ledger.app.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleDriveAuthorizationTest {

    @Test
    fun driveFileScope_isGoogleDriveFileScope() {
        assertEquals(
            "https://www.googleapis.com/auth/" + "drive.file",
            GoogleDriveAuthorization.DRIVE_FILE_SCOPE
        )
    }
}
