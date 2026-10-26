package com.ledger.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EmiChargeSettlementDao {
    @Insert
    fun insert(item: EmiChargeSettlementEntity): Long

    @Query(
        "SELECT * FROM emi_charge_settlements " +
        "WHERE transactionId = :transactionId " +
        "ORDER BY settlementDate DESC, id DESC"
    )
    fun getByTransactionId(transactionId: Long): List<EmiChargeSettlementEntity>

    @Query("DELETE FROM emi_charge_settlements WHERE id = :id")
    fun deleteById(id: Long): Int
}
