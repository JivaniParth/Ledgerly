package com.parthjivani.ledgerly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parthjivani.ledgerly.data.LedgerlyRepository
import com.parthjivani.ledgerly.ui.LedgerlyViewModel
import com.parthjivani.ledgerly.ui.LedgerlyViewModelFactory
import com.parthjivani.ledgerly.ui.MainAppContainer
import com.parthjivani.ledgerly.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var repository: LedgerlyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = LedgerlyRepository.getInstance(applicationContext)

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
