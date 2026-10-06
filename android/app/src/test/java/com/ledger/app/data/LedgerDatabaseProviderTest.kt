package com.ledger.app.data

import org.junit.Test
import org.junit.Assert.assertEquals

class LedgerDatabaseProviderTest {

    @Test
    fun databaseNameIsLedgerDb() {
        assertEquals(
            "ledger.db",
            LedgerDatabaseProvider.DATABASE_NAME
        )
    }
}
