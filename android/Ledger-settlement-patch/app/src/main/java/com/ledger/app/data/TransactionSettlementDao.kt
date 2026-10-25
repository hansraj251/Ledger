package com.ledger.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TransactionSettlementDao {
    @Insert
    fun insert(settlement: TransactionSettlementEntity): Long

    @Query("SELECT * FROM transaction_settlements WHERE transactionId = :transactionId ORDER BY settlementDate ASC, id ASC")
    fun getByTransactionId(transactionId: Long): List<TransactionSettlementEntity>

    @Query("SELECT s.* FROM transaction_settlements s INNER JOIN ledger_entries e ON e.id = s.transactionId WHERE e.partyId = :partyId ORDER BY s.settlementDate ASC, s.id ASC")
    fun getByPartyId(partyId: Long): List<TransactionSettlementEntity>

    @Query("SELECT * FROM transaction_settlements ORDER BY settlementDate ASC, id ASC")
    fun getAll(): List<TransactionSettlementEntity>
}
