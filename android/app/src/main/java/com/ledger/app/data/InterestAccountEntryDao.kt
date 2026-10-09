package com.ledger.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface InterestAccountEntryDao {
    @Insert
    fun insert(entry: InterestAccountEntryEntity): Long

    @Query(
        "SELECT * FROM interest_account_entries " +
            "WHERE partyId = :partyId " +
            "ORDER BY transactionDate DESC, id DESC"
    )
    fun getByPartyId(partyId: Long): List<InterestAccountEntryEntity>
}
