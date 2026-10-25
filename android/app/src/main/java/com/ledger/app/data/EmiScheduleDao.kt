package com.ledger.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EmiScheduleDao {
    @Insert
    fun insertAll(items: List<EmiScheduleEntity>): List<Long>

    @Query("SELECT * FROM emi_schedules WHERE transactionId = :transactionId ORDER BY installmentNumber")
    fun getByTransactionId(transactionId: Long): List<EmiScheduleEntity>

    @Query("SELECT * FROM emi_schedules WHERE transactionId IN (SELECT id FROM ledger_entries WHERE partyId = :partyId) ORDER BY dueDate, installmentNumber")
    fun getByPartyId(partyId: Long): List<EmiScheduleEntity>

    @Query("UPDATE emi_schedules SET isPaid = :paid WHERE id = :id")
    fun setPaid(id: Long, paid: Boolean)

    @Update
    fun updateSchedule(item: EmiScheduleEntity)
}
