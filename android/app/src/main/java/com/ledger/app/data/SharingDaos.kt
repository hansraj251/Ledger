package com.ledger.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SharingGroupDao {
    @Query("SELECT * FROM sharing_groups ORDER BY name COLLATE NOCASE")
    fun getAll(): List<SharingGroupEntity>

    @Insert
    fun insert(item: SharingGroupEntity): Long

    @Update
    fun update(item: SharingGroupEntity): Int

    @Delete
    fun delete(item: SharingGroupEntity): Int
}

@Dao
interface SharingMemberDao {
    @Query("SELECT * FROM sharing_members WHERE groupId = :groupId ORDER BY name COLLATE NOCASE")
    fun getByGroup(groupId: Long): List<SharingMemberEntity>

    @Insert
    fun insert(item: SharingMemberEntity): Long

    @Update
    fun update(item: SharingMemberEntity): Int

    @Delete
    fun delete(item: SharingMemberEntity): Int

    @Query("DELETE FROM sharing_members WHERE groupId = :groupId")
    fun deleteByGroup(groupId: Long)
}

@Dao
interface GroupSettlementDao {
    @Query("SELECT * FROM group_settlements WHERE groupId = :groupId ORDER BY receivedAt DESC, id DESC")
    fun getByGroup(groupId: Long): List<GroupSettlementEntity>

    @Insert
    fun insert(item: GroupSettlementEntity): Long

    @Query("DELETE FROM group_settlements WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM group_settlements WHERE groupId = :groupId")
    fun deleteByGroup(groupId: Long)
}

@Dao
interface GroupExpenseDao {
    @Query("SELECT COUNT(*) FROM group_expenses")
    fun count(): Int
    @Query("SELECT * FROM group_expenses WHERE groupId = :groupId ORDER BY expenseDate DESC, id DESC")
    fun getByGroup(groupId: Long): List<GroupExpenseEntity>

    @Insert
    fun insert(item: GroupExpenseEntity): Long

    @Update
    fun update(item: GroupExpenseEntity): Int

    @Delete
    fun delete(item: GroupExpenseEntity): Int

    @Query("DELETE FROM group_expenses WHERE groupId = :groupId")
    fun deleteByGroup(groupId: Long)
}

@Dao
interface GroupExpensePaymentDao {
    @Query("SELECT * FROM group_expense_payments WHERE expenseId = :expenseId ORDER BY memberNameSnapshot COLLATE NOCASE")
    fun getByExpense(expenseId: Long): List<GroupExpensePaymentEntity>

    @Insert
    fun insertAll(items: List<GroupExpensePaymentEntity>)

    @Query("DELETE FROM group_expense_payments WHERE expenseId = :expenseId")
    fun deleteByExpense(expenseId: Long)

    @Query("""
        DELETE FROM group_expense_payments
        WHERE expenseId IN (
            SELECT id FROM group_expenses WHERE groupId = :groupId
        )
    """)
    fun deleteByGroup(groupId: Long)
}

@Dao
interface GroupExpenseSplitDao {
    @Query("SELECT * FROM group_expense_splits WHERE expenseId = :expenseId ORDER BY memberNameSnapshot COLLATE NOCASE")
    fun getByExpense(expenseId: Long): List<GroupExpenseSplitEntity>

    @Insert
    fun insertAll(items: List<GroupExpenseSplitEntity>)

    @Query("DELETE FROM group_expense_splits WHERE expenseId = :expenseId")
    fun deleteByExpense(expenseId: Long)

    @Query("""
        DELETE FROM group_expense_splits
        WHERE expenseId IN (
            SELECT id FROM group_expenses WHERE groupId = :groupId
        )
    """)
    fun deleteByGroup(groupId: Long)
}
