package com.parthjivani.hisabtracker

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
import com.parthjivani.hisabtracker.data.AppDatabase
import com.parthjivani.hisabtracker.data.HisabRepository
import com.parthjivani.hisabtracker.ui.HisabViewModel
import com.parthjivani.hisabtracker.ui.HisabViewModelFactory
import com.parthjivani.hisabtracker.ui.MainAppContainer
import com.parthjivani.hisabtracker.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: HisabRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup database and repository singletons
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "hisab_database"
        )
        .fallbackToDestructiveMigration()
        .build()

        repository = HisabRepository(
            database.familyMemberDao(),
            database.transactionDao(),
            database.appSettingDao(),
            database.budgetGoalDao(),
            database.savingsGoalDao(),
            database.recurringTransactionDao(),
            database.creditCardDao()
        )

        enableEdgeToEdge()
        
        setContent {
            // Initialize ViewModel with our production repository
            val viewModel: HisabViewModel = viewModel(
                factory = HisabViewModelFactory(repository)
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
