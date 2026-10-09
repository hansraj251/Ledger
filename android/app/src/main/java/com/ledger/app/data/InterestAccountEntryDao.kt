package com.ledger.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query

@Dao
interface InterestAccountEntryDao {
    @Insert
    fun insert(entry: InterestAccountEntryEntity): Long

    @Update
    fun update(entry: InterestAccountEntryEntity): Int

    @Query(
        "SELECT * FROM interest_account_entries " +
            "WHERE partyId = :partyId " +
            "ORDER BY transactionDate DESC, id DESC"
    )
    fun getByPartyId(partyId: Long): List<InterestAccountEntryEntity>

    @Query("DELETE FROM interest_account_entries WHERE id = :entryId AND partyId = :partyId")
    fun deleteByIdAndPartyId(entryId: Long, partyId: Long): Int
}
