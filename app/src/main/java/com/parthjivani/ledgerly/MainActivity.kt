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
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create custom_wallets table
                database.execSQL("CREATE TABLE IF NOT EXISTS `custom_wallets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `color` TEXT NOT NULL)")
                
                // Add columns to transactions
                database.execSQL("ALTER TABLE `transactions` ADD COLUMN `customWalletId` INTEGER")
                database.execSQL("ALTER TABLE `transactions` ADD COLUMN `destinationCustomWalletId` INTEGER")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add color column to credit_cards
                database.execSQL("ALTER TABLE `credit_cards` ADD COLUMN `color` TEXT NOT NULL DEFAULT '#E91E63'")
            }
        }

        // Setup database and repository singletons
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ledgerly_database"
        )
        .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
        .fallbackToDestructiveMigration()
        .build()

        repository = LedgerlyRepository(
            database.familyMemberDao(),
            database.transactionDao(),
            database.appSettingDao(),
            database.budgetGoalDao(),
            database.savingsGoalDao(),
            database.recurringTransactionDao(),
            database.creditCardDao(),
            database.customWalletDao()
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
