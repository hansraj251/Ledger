package com.ledger.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DailyExpenseDao {
    @Insert
    fun insert(expense: DailyExpenseEntity): Long

    @Update
    fun update(expense: DailyExpenseEntity): Int

    @Delete
    fun delete(expense: DailyExpenseEntity): Int

    @Query("SELECT * FROM daily_expenses ORDER BY expenseDate DESC, id DESC")
    fun getAll(): List<DailyExpenseEntity>
}
