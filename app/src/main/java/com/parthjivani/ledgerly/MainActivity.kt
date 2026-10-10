package com.parthjivani.ledgerly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.parthjivani.ledgerly.data.AppDatabase
import com.parthjivani.ledgerly.data.LedgerlyRepository
import com.parthjivani.ledgerly.ui.LedgerlyViewModel
import com.parthjivani.ledgerly.ui.LedgerlyViewModelFactory
import com.parthjivani.ledgerly.ui.MainAppContainer
import com.parthjivani.ledgerly.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: LedgerlyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create custom_wallets table
                db.execSQL("CREATE TABLE IF NOT EXISTS `custom_wallets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `color` TEXT NOT NULL)")
                
                // Add columns to transactions
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `customWalletId` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `destinationCustomWalletId` INTEGER")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add color column to credit_cards
                db.execSQL("ALTER TABLE `credit_cards` ADD COLUMN `color` TEXT NOT NULL DEFAULT '#E91E63'")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create contacts table
                db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                
                // Create transaction_splits table
                db.execSQL("CREATE TABLE IF NOT EXISTS `transaction_splits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transactionId` INTEGER NOT NULL, `amount` REAL NOT NULL, `contactId` INTEGER, `familyMemberId` INTEGER, `isSettled` INTEGER NOT NULL)")
                
                // Add contactId to transactions
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `contactId` INTEGER")
            }
        }

        // Setup database and repository singletons
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ledgerly_database"
        )
        .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

        repository = LedgerlyRepository(
            database.familyMemberDao(),
            database.transactionDao(),
            database.appSettingDao(),
            database.budgetGoalDao(),
            database.savingsGoalDao(),
            database.recurringTransactionDao(),
            database.creditCardDao(),
            database.customWalletDao(),
            database.contactDao(),
            database.transactionSplitDao()
        )

        enableEdgeToEdge()
        
        setContent {
            // Initialize ViewModel with our production repository
            val viewModel: LedgerlyViewModel = viewModel(
                factory = LedgerlyViewModelFactory(repository)
            )
            val appTheme by viewModel.appTheme.collectAsState()

            // Theme automatically follows the device's dark/light mode setting or user preference
            MyApplicationTheme(appTheme = appTheme) {
                // Load root application screens
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}
