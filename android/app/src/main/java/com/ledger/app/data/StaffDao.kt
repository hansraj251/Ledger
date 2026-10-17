package com.ledger.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff_members ORDER BY isActive DESC, name COLLATE NOCASE")
    fun getAllStaff(): List<StaffEntity>

    @Query("SELECT * FROM staff_members WHERE id = :staffId LIMIT 1")
    fun getStaff(staffId: Long): StaffEntity?

    @Insert
    fun insertStaff(staff: StaffEntity): Long

    @Update
    fun updateStaff(staff: StaffEntity)

    @Query("DELETE FROM salary_revisions WHERE staffId = :staffId")
    fun deleteSalaryRevisionsForStaff(staffId: Long): Int

    @Query("DELETE FROM salary_payments WHERE staffId = :staffId")
    fun deleteSalaryPaymentsForStaff(staffId: Long): Int

    @Query("DELETE FROM staff_members WHERE id = :staffId")
    fun deleteStaffById(staffId: Long): Int

    @Query("SELECT * FROM salary_revisions WHERE staffId = :staffId ORDER BY effectiveDate DESC, id DESC")
    fun getSalaryRevisions(staffId: Long): List<SalaryRevisionEntity>

    @Insert
    fun insertSalaryRevision(revision: SalaryRevisionEntity): Long

    @Update
    fun updateSalaryRevision(revision: SalaryRevisionEntity)

    @Query("DELETE FROM salary_revisions WHERE id = :revisionId AND staffId = :staffId")
    fun deleteSalaryRevisionById(revisionId: Long, staffId: Long): Int

    @Query("SELECT * FROM salary_payments WHERE staffId = :staffId ORDER BY paymentDate DESC, id DESC")
    fun getSalaryPayments(staffId: Long): List<SalaryPaymentEntity>

    @Insert
    fun insertSalaryPayment(payment: SalaryPaymentEntity): Long

    @Update
    fun updateSalaryPayment(payment: SalaryPaymentEntity)

    @Query("DELETE FROM salary_payments WHERE id = :paymentId AND staffId = :staffId")
    fun deleteSalaryPaymentById(paymentId: Long, staffId: Long): Int


    @Query("SELECT * FROM staff_advance_transactions WHERE staffId = :staffId ORDER BY transactionDate DESC, id DESC")
    fun getStaffAdvanceTransactions(staffId: Long): List<StaffAdvanceTransactionEntity>

    @Insert
    fun insertStaffAdvanceTransaction(transaction: StaffAdvanceTransactionEntity): Long

    @Query("DELETE FROM staff_advance_transactions WHERE staffId = :staffId")
    fun deleteStaffAdvanceTransactionsForStaff(staffId: Long): Int

    @Query("SELECT * FROM staff_bonus_payments WHERE staffId = :staffId ORDER BY bonusDate DESC, id DESC")
    fun getStaffBonusPayments(staffId: Long): List<StaffBonusEntity>

    @Insert
    fun insertStaffBonusPayment(bonus: StaffBonusEntity): Long

    @Update
    fun updateStaffBonusPayment(bonus: StaffBonusEntity)

    @Query("DELETE FROM staff_bonus_payments WHERE id = :bonusId AND staffId = :staffId")
    fun deleteStaffBonusPaymentById(bonusId: Long, staffId: Long): Int

    @Query("DELETE FROM staff_bonus_payments WHERE staffId = :staffId")
    fun deleteStaffBonusPaymentsForStaff(staffId: Long): Int
}
