package com.ledger.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PartyEntity::class,
        LedgerEntryEntity::class,
        LedgerDatabaseMarker::class,
        ProfileEntity::class,
        InterestAccountEntryEntity::class,
        DailyExpenseEntity::class,
        CustomExpenseCategoryEntity::class,
        SharingGroupEntity::class,
        SharingMemberEntity::class,
        GroupExpenseEntity::class,
        GroupExpenseSplitEntity::class,
        GroupExpensePaymentEntity::class,
        GroupSettlementEntity::class,
        AppSettingsEntity::class,
        StaffEntity::class,
        SalaryRevisionEntity::class,
        SalaryPaymentEntity::class,
        StaffAdvanceTransactionEntity::class,
        StaffBonusEntity::class,
   StaffSalaryDeductionEntity::class,
        PartySellItemEntity::class,
        EmiScheduleEntity::class,
        TransactionSettlementEntity::class,
        EmiChargeSettlementEntity::class
    ],
    version = 37,
    exportSchema = false
)
abstract class LedgerDatabase : RoomDatabase() {

    abstract fun partyDao(): PartyDao

    abstract fun partySellItemDao(): PartySellItemDao

    abstract fun emiScheduleDao(): EmiScheduleDao

    abstract fun ledgerEntryDao(): LedgerEntryDao

    abstract fun profileDao(): ProfileDao

    abstract fun interestAccountEntryDao(): InterestAccountEntryDao

    abstract fun dailyExpenseDao(): DailyExpenseDao

    abstract fun customExpenseCategoryDao(): CustomExpenseCategoryDao

    abstract fun sharingGroupDao(): SharingGroupDao

    abstract fun sharingMemberDao(): SharingMemberDao

    abstract fun groupExpenseDao(): GroupExpenseDao

    abstract fun groupExpenseSplitDao(): GroupExpenseSplitDao

    abstract fun groupExpensePaymentDao(): GroupExpensePaymentDao

    abstract fun groupSettlementDao(): GroupSettlementDao

    abstract fun appSettingsDao(): AppSettingsDao

    abstract fun staffDao(): StaffDao

    abstract fun transactionSettlementDao(): TransactionSettlementDao

    abstract fun emiChargeSettlementDao(): EmiChargeSettlementDao
}
