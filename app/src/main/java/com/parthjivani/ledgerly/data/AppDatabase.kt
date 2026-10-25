package com.parthjivani.ledgerly.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY name ASC")
    fun getAllFamilyMembers(): Flow<List<FamilyMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMember): Long

    @Update
    suspend fun updateFamilyMember(member: FamilyMember)

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

@Dao
interface CustomWalletDao {
    @Query("SELECT * FROM custom_wallets ORDER BY name ASC")
    fun getAllCustomWalletsFlow(): Flow<List<CustomWallet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomWallet(wallet: CustomWallet): Long

    @Delete
    suspend fun deleteCustomWallet(wallet: CustomWallet)

    @Query("DELETE FROM custom_wallets")
    suspend fun clearAllCustomWallets()
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContactsFlow(): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Update
    suspend fun updateContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Long): Contact?

    @Query("DELETE FROM contacts")
    suspend fun clearAllContacts()
}

@Dao
interface TransactionSplitDao {
    @Query("SELECT * FROM transaction_splits WHERE transactionId = :transactionId")
    fun getSplitsForTransaction(transactionId: Long): Flow<List<TransactionSplit>>

    @Query("SELECT * FROM transaction_splits WHERE contactId = :contactId OR familyMemberId = :familyMemberId")
    fun getSplitsForPerson(contactId: Long?, familyMemberId: Long?): Flow<List<TransactionSplit>>
    
    @Query("SELECT * FROM transaction_splits")
    fun getAllSplitsFlow(): Flow<List<TransactionSplit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplit(split: TransactionSplit): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<TransactionSplit>)

    @Delete
    suspend fun deleteSplit(split: TransactionSplit)

    @Query("UPDATE transaction_splits SET isSettled = 1 WHERE contactId = :contactId")
    suspend fun settleSplitsForContact(contactId: Long)
    
    @Query("UPDATE transaction_splits SET isSettled = 1 WHERE familyMemberId = :familyMemberId")
    suspend fun settleSplitsForFamilyMember(familyMemberId: Long)

    @Query("DELETE FROM transaction_splits WHERE transactionId = :transactionId")
    suspend fun deleteSplitsByTransactionId(transactionId: Long)

    @Query("DELETE FROM transaction_splits")
    suspend fun clearAllSplits()
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
        CreditCard::class,
        CustomWallet::class,
        Contact::class,
        TransactionSplit::class
    ],
    version = 7,
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
    abstract fun customWalletDao(): CustomWalletDao
    abstract fun contactDao(): ContactDao
    abstract fun transactionSplitDao(): TransactionSplitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `custom_wallets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `color` TEXT NOT NULL)")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `customWalletId` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `destinationCustomWalletId` INTEGER")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `credit_cards` ADD COLUMN `color` TEXT NOT NULL DEFAULT '#E91E63'")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `transaction_splits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transactionId` INTEGER NOT NULL, `amount` REAL NOT NULL, `contactId` INTEGER, `familyMemberId` INTEGER, `isSettled` INTEGER NOT NULL)")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `contactId` INTEGER")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ledgerly_database"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
