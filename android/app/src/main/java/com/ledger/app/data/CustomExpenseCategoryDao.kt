package com.ledger.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CustomExpenseCategoryDao {
    @Query("SELECT * FROM custom_expense_categories ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<CustomExpenseCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CustomExpenseCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CustomExpenseCategoryEntity>)

    @Query("DELETE FROM custom_expense_categories WHERE name = :name")
    suspend fun deleteByName(name: String)
}
