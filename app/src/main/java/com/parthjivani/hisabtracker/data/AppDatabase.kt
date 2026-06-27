package com.parthjivani.hisabtracker.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY name ASC")
    fun getAllFamilyMembers(): Flow<List<FamilyMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMember): Long

    @Delete
    suspend fun deleteFamilyMember(member: FamilyMember)

    @Query("SELECT * FROM family_members WHERE id = :id LIMIT 1")
    suspend fun getFamilyMemberById(id: Long): FamilyMember?

    @Query("DELETE FROM family_members")
    suspend fun clearAllFamilyMembers()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE familyMemberId = :familyMemberId ORDER BY date DESC, id DESC")
    fun getTransactionsForFamilyMember(familyMemberId: Long): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings ORDER BY `key` ASC")
    fun getAllSettingsFlow(): Flow<List<AppSetting>>

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingByKey(key: String): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)

    @Query("DELETE FROM app_settings")
    suspend fun clearAllSettings()
}

@Dao
interface BudgetGoalDao {
    @Query("SELECT * FROM budget_goals ORDER BY category ASC")
    fun getAllBudgetGoalsFlow(): Flow<List<BudgetGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetGoal(budgetGoal: BudgetGoal)

    @Query("DELETE FROM budget_goals WHERE category = :category")
    suspend fun deleteBudgetGoalByCategory(category: String)

    @Query("DELETE FROM budget_goals")
    suspend fun clearAllBudgetGoals()
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY name ASC")
    fun getAllSavingsGoalsFlow(): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(savingsGoal: SavingsGoal): Long

    @Delete
    suspend fun deleteSavingsGoal(savingsGoal: SavingsGoal)

    @Query("DELETE FROM savings_goals")
    suspend fun clearAllSavingsGoals()
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY id DESC")
    fun getAllRecurringTransactionsFlow(): Flow<List<RecurringTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction): Long

    @Delete
    suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction)

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1")
    suspend fun getActiveRecurringTransactions(): List<RecurringTransaction>

    @Query("DELETE FROM recurring_transactions")
    suspend fun clearAllRecurringTransactions()
}

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards ORDER BY cardName ASC")
    fun getAllCreditCardsFlow(): Flow<List<CreditCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(creditCard: CreditCard): Long

    @Delete
    suspend fun deleteCreditCard(creditCard: CreditCard)

    @Query("DELETE FROM credit_cards")
    suspend fun clearAllCreditCards()
}

class Converters {
    @TypeConverter
    fun fromWalletType(value: WalletType): String {
        return value.name
    }

    @TypeConverter
    fun toWalletType(value: String): WalletType {
        return WalletType.valueOf(value)
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }

    @TypeConverter
    fun fromDirection(value: TransactionDirection?): String? {
        return value?.name
    }

    @TypeConverter
    fun toDirection(value: String?): TransactionDirection? {
        return value?.let { TransactionDirection.valueOf(it) }
    }
}

@Database(
    entities = [
        FamilyMember::class,
        Transaction::class,
        AppSetting::class,
        BudgetGoal::class,
        SavingsGoal::class,
        RecurringTransaction::class,
        CreditCard::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun transactionDao(): TransactionDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun budgetGoalDao(): BudgetGoalDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun creditCardDao(): CreditCardDao
}
