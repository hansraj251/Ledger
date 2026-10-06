package com.ledger.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LedgerEntryDao {

    @Update
    fun update(entry: LedgerEntryEntity)

    @Insert
    fun insert(entry: LedgerEntryEntity): Long

    @Delete
    fun delete(entry: LedgerEntryEntity)

    @Query(
        """
        SELECT *
        FROM ledger_entries
        WHERE partyId = :partyId
        ORDER BY createdAt ASC, id ASC
        """
    )
    fun getByPartyId(partyId: Long): List<LedgerEntryEntity>

    @Query(
        """
        SELECT COALESCE(
            SUM(
                CASE
                    WHEN type = 'CREDIT' THEN amount
                    WHEN type = 'DEBIT' THEN -amount
                    ELSE 0
                END
            ),
            0
        )
        FROM ledger_entries
        WHERE partyId = :partyId
        """
    )
    fun getBalance(partyId: Long): Double

    @Query("SELECT COUNT(*) FROM ledger_entries")
    fun count(): Int
}
