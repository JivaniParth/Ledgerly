package com.parthjivani.ledgerly.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.parthjivani.ledgerly.ui.components.AmountInputField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parthjivani.ledgerly.data.*
import com.parthjivani.ledgerly.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext
import com.parthjivani.ledgerly.ui.*
import com.parthjivani.ledgerly.data.*
import com.parthjivani.ledgerly.ui.theme.*

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExcelImport: () -> Unit,
    viewModel: LedgerlyViewModel
) {
    val budgetGoals by viewModel.allBudgetGoals.collectAsState()
    val savingsGoals by viewModel.allSavingsGoals.collectAsState()
    val activeRecurring by viewModel.allRecurringTransactions.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val incomeCategoriesList by viewModel.incomeCategories.collectAsState()

    val fallbackCategories = listOf("Groceries", "Entertainment", "Bills", "Travel", "Medical", "Other")
    val displayCategories = categoriesList.ifEmpty { fallbackCategories }

    val fallbackIncomeCategories = listOf("Salary", "Business", "Investment", "Freelance", "Gift", "Other")
    val displayIncomeCategories = incomeCategoriesList.ifEmpty { fallbackIncomeCategories }

    // Categories states
    var newCategoryName by remember { mutableStateOf("") }
    var newIncomeCategoryName by remember { mutableStateOf("") }

    // Budget goals states
    var newGoalCategory by remember { mutableStateOf("Groceries") }
    var newGoalLimit by remember { mutableStateOf("") }

    // Recurring transactions states
    var newRecAmount by remember { mutableStateOf("") }
    var newRecNotes by remember { mutableStateOf("") }
    var newRecCategory by remember { mutableStateOf("Groceries") }
    var newRecType by remember { mutableStateOf(TransactionType.PERSONAL_EXPENSE) }
    var newRecWallet by remember { mutableStateOf(WalletType.EXPENSE_WALLET) }
    var newRecInterval by remember { mutableStateOf("Monthly") }
    var newRecNextDueDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                AppLogo()
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Settings & Customization",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Preferences Section
            item {
                Text(
                    text = "Preferences",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val appTheme by viewModel.appTheme.collectAsState()
                var showThemeDialog by remember { mutableStateOf(false) }

                Card(
                    onClick = { showThemeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_selector_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text("App Theme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = when (appTheme) {
                                        "light" -> "Light Mode"
                                        "dark" -> "Dark Mode"
                                        else -> "System Default"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (showThemeDialog) {
                    AlertDialog(
                        onDismissRequest = { showThemeDialog = false },
                        title = { Text("Select App Theme") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("system" to "System Default", "light" to "Light Mode", "dark" to "Dark Mode").forEach { (value, label) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.setAppTheme(value)
                                                showThemeDialog = false
                                            }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        RadioButton(
                                            selected = appTheme == value,
                                            onClick = {
                                                viewModel.setAppTheme(value)
                                                showThemeDialog = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(label)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemeDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Excel Bulk Importer Section
            item {
                Text(
                    text = "Data utilities",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    onClick = onNavigateToExcelImport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("navigate_to_excel_import_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text("Bulk Import from Excel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Load income / expense records from workbook", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                var showWipeConfirmDialog by remember { mutableStateOf(false) }

                if (showWipeConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showWipeConfirmDialog = false },
                        title = { Text("Confirm Wipeout?") },
                        text = { Text("This will completely delete all local transactions, family members, credit cards, budget goals, and recurring entries. This action is permanent and cannot be undone.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.clearAllData()
                                    showWipeConfirmDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Wipe Everything", color = MaterialTheme.colorScheme.onError)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showWipeConfirmDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Card(
                    onClick = { showWipeConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wipe_all_data_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text("Wipe All Local Data", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Delete all transactions, members, and cards to start fresh", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Categories Management Section
            item {
                Text(
                    text = "Manage expense categories",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Add Custom Category",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                placeholder = { Text("e.g. Health, Charity, Gifts", fontSize = 13.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("category_name_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = {
                                    val cat = newCategoryName.trim()
                                    if (cat.isNotEmpty()) {
                                        viewModel.addCategory(cat)
                                        newCategoryName = ""
                                    }
                                },
                                modifier = Modifier.testTag("add_category_button"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Active Categories",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Use FlowRow for a beautiful wrap-around chip list
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            displayCategories.forEach { cat ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = cat, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        IconButton(
                                            onClick = { viewModel.deleteCategory(cat) },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .testTag("delete_category_$cat")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Income Categories Management Section
            item {
                Text(
                    text = "Manage income categories",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Add Custom Income Category",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newIncomeCategoryName,
                                onValueChange = { newIncomeCategoryName = it },
                                placeholder = { Text("e.g. Dividend, Refund, Cashback", fontSize = 13.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("income_category_name_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = {
                                    val cat = newIncomeCategoryName.trim()
                                    if (cat.isNotEmpty()) {
                                        viewModel.addIncomeCategory(cat)
                                        newIncomeCategoryName = ""
                                    }
                                },
                                modifier = Modifier.testTag("add_income_category_button"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Active Income Categories",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Use FlowRow for a beautiful wrap-around chip list
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            displayIncomeCategories.forEach { cat ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = cat, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        IconButton(
                                            onClick = { viewModel.deleteIncomeCategory(cat) },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .testTag("delete_income_category_$cat")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Set Budget Goals Section
            item {
                Text(
                    text = "Set monthly budget goals",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add/Update limit:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        val categoryDropdownList = listOf("Overall") + displayCategories
                        if (!categoryDropdownList.contains(newGoalCategory)) {
                            // Safely reset goal category to Groceries or whatever is first if the chosen one was deleted
                            newGoalCategory = "Overall"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                var catExpanded by remember { mutableStateOf(false) }
                                OutlinedButton(
                                    onClick = { catExpanded = true },
                                    modifier = Modifier.fillMaxWidth().testTag("budget_goal_cat_select"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(newGoalCategory, fontSize = 12.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "dropdown", modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                    categoryDropdownList.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat, fontSize = 12.sp) },
                                            onClick = {
                                                newGoalCategory = cat
                                                catExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            AmountInputField(
                                value = newGoalLimit,
                                onValueChange = { newGoalLimit = it },
                                label = { Text("Limit (₹)", fontSize = 11.sp) },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("budget_goal_limit_input")
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val amt = newGoalLimit.toDoubleOrNull()
                                if (amt != null && amt > 0) {
                                    viewModel.addBudgetGoal(newGoalCategory, amt)
                                    newGoalLimit = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_budget_goal_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Set Budget Limit", fontSize = 12.sp)
                        }

                        if (budgetGoals.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Active Goals list:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            var editingGoal by remember { mutableStateOf<BudgetGoal?>(null) }
                            if (editingGoal != null) {
                                var editLimit by remember { mutableStateOf(editingGoal!!.limitAmount.toString()) }
                                AlertDialog(
                                    onDismissRequest = { editingGoal = null },
                                    title = { Text("Edit Budget Goal") },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Category: ${editingGoal!!.category}", fontWeight = FontWeight.Bold)
                                            AmountInputField(
                                                value = editLimit,
                                                onValueChange = { editLimit = it },
                                                label = { Text("Limit (₹)") }
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            val amt = editLimit.toDoubleOrNull() ?: 0.0
                                            if (amt > 0) {
                                                viewModel.addBudgetGoal(editingGoal!!.category, amt)
                                                editingGoal = null
                                            }
                                        }) { Text("Save") }
                                    },
                                    dismissButton = { TextButton(onClick = { editingGoal = null }) { Text("Cancel") } }
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                budgetGoals.forEach { g ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .clickable { editingGoal = g }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${g.category}: ₹ ${String.format("%.0f", g.limitAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        IconButton(
                                            onClick = { viewModel.deleteBudgetGoal(g.category) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("delete_goal_${g.category}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Goal", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                var editingSavingsGoal by remember { mutableStateOf<SavingsGoal?>(null) }
                
                if (editingSavingsGoal != null) {
                    var edName by remember { mutableStateOf(editingSavingsGoal!!.name) }
                    var edTarget by remember { mutableStateOf(editingSavingsGoal!!.targetAmount.toString()) }
                    var edCurrent by remember { mutableStateOf(editingSavingsGoal!!.currentAmount.toString()) }
                    var edCategory by remember { mutableStateOf(editingSavingsGoal!!.category) }

                    AlertDialog(
                        onDismissRequest = { editingSavingsGoal = null },
                        title = { Text("Edit Savings Goal") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = edName, onValueChange = { edName = it }, label = { Text("Goal Name") }, singleLine = true)
                                AmountInputField(value = edTarget, onValueChange = { edTarget = it }, label = { Text("Target Amount") })
                                AmountInputField(value = edCurrent, onValueChange = { edCurrent = it }, label = { Text("Current Saved") })
                                OutlinedTextField(value = edCategory, onValueChange = { edCategory = it }, label = { Text("Category") }, singleLine = true)
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                val t = edTarget.toDoubleOrNull() ?: 0.0
                                val c = edCurrent.toDoubleOrNull() ?: 0.0
                                if (edName.isNotBlank() && t > 0) {
                                    viewModel.updateSavingsGoal(editingSavingsGoal!!.copy(
                                        name = edName, targetAmount = t, currentAmount = c, category = edCategory
                                    ))
                                    editingSavingsGoal = null
                                }
                            }) { Text("Save") }
                        },
                        dismissButton = { TextButton(onClick = { editingSavingsGoal = null }) { Text("Cancel") } }
                    )
                }

                // Savings Goals Section
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Savings Goals",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    var showAddGoal by remember { mutableStateOf(false) }
                    TextButton(onClick = { showAddGoal = true }) {
                        Text("+ Add Goal", fontSize = 13.sp)
                    }
                    if (showAddGoal) {
                        var name by remember { mutableStateOf("") }
                        var target by remember { mutableStateOf("") }
                        var current by remember { mutableStateOf("") }
                        var category by remember { mutableStateOf("Overall") }
                        AlertDialog(
                            onDismissRequest = { showAddGoal = false },
                            title = { Text("New Savings Goal") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Goal Name (e.g. Bike)") }, singleLine = true)
                                    AmountInputField(value = target, onValueChange = { target = it }, label = { Text("Target Amount") })
                                    AmountInputField(value = current, onValueChange = { current = it }, label = { Text("Current Saved (Optional)") })
                                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Overall or Specific)") }, singleLine = true)
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val t = target.toDoubleOrNull() ?: 0.0
                                    val c = current.toDoubleOrNull() ?: 0.0
                                    if (name.isNotBlank() && t > 0) {
                                        viewModel.addSavingsGoal(name, t, c, category)
                                        showAddGoal = false
                                    }
                                }) { Text("Save") }
                            },
                            dismissButton = { TextButton(onClick = { showAddGoal = false }) { Text("Cancel") } }
                        )
                    }
                }
                
                if (savingsGoals.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        savingsGoals.forEach { sg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { editingSavingsGoal = sg }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(sg.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Target: ${formatCurrency(sg.targetAmount)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSavingsGoal(sg) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Goal", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Recurring Transactions Section
            item {
                Text(
                    text = "Manage recurring transactions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add new recurring schedule:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        val selectCategoriesRecurring = displayCategories
                        if (!selectCategoriesRecurring.contains(newRecCategory)) {
                            newRecCategory = "Other"
                        }

                        AmountInputField(
                            value = newRecAmount,
                            onValueChange = { newRecAmount = it },
                            label = { Text("Amount (₹)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recurring_amount_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newRecNotes,
                            onValueChange = { newRecNotes = it },
                            label = { Text("Notes / Description") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recurring_notes_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Type Selector
                        Text("Transaction Type:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val types = listOf(TransactionType.PERSONAL_EXPENSE, TransactionType.PERSONAL_INCOME)
                            types.forEach { type ->
                                val isSel = newRecType == type
                                val label = if (type == TransactionType.PERSONAL_EXPENSE) "Expense" else "Income"
                                ElevatedButton(
                                    onClick = { newRecType = type },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(label, fontSize = 11.sp, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Wallet Selector
                        Text("Target Wallet:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val wallets = listOf(WalletType.EXPENSE_WALLET, WalletType.SAVINGS_WALLET)
                            wallets.forEach { w ->
                                val isSel = newRecWallet == w
                                val label = if (w == WalletType.EXPENSE_WALLET) "Expense" else "Savings"
                                ElevatedButton(
                                    onClick = { newRecWallet = w },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(label, fontSize = 11.sp, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Select
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Box {
                                var catExpanded by remember { mutableStateOf(false) }
                                OutlinedButton(onClick = { catExpanded = true }, shape = RoundedCornerShape(10.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(newRecCategory, fontSize = 11.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                    selectCategoriesRecurring.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat, fontSize = 12.sp) },
                                            onClick = {
                                                newRecCategory = cat
                                                catExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Interval Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Interval:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Box {
                                var intExpanded by remember { mutableStateOf(false) }
                                OutlinedButton(onClick = { intExpanded = true }, shape = RoundedCornerShape(10.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(newRecInterval, fontSize = 11.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(expanded = intExpanded, onDismissRequest = { intExpanded = false }) {
                                    listOf("Daily", "Weekly", "Monthly").forEach { interval ->
                                        DropdownMenuItem(
                                            text = { Text(interval, fontSize = 12.sp) },
                                            onClick = {
                                                newRecInterval = interval
                                                intExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // Date and Time picker
                        var showNewRecDatePicker by remember { mutableStateOf(false) }
                        var showNewRecTimePicker by remember { mutableStateOf(false) }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Next Due / Start Date:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(newRecNextDueDate))
                            OutlinedButton(
                                onClick = { showNewRecDatePicker = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(formattedDate, fontSize = 11.sp)
                            }
                        }

                        if (showNewRecDatePicker) {
                            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = newRecNextDueDate)
                            androidx.compose.material3.DatePickerDialog(
                                onDismissRequest = { showNewRecDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let { selectedDate ->
                                            val currentCal = Calendar.getInstance().apply { timeInMillis = newRecNextDueDate }
                                            val newCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                            newCal.set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                                            newCal.set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                                            newRecNextDueDate = newCal.timeInMillis
                                        }
                                        showNewRecDatePicker = false
                                        showNewRecTimePicker = true
                                    }) { Text("Next") }
                                },
                                dismissButton = { TextButton(onClick = { showNewRecDatePicker = false }) { Text("Cancel") } }
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }

                        if (showNewRecTimePicker) {
                            val cal = Calendar.getInstance().apply { timeInMillis = newRecNextDueDate }
                            val timePickerState = rememberTimePickerState(
                                initialHour = cal.get(Calendar.HOUR_OF_DAY),
                                initialMinute = cal.get(Calendar.MINUTE),
                                is24Hour = false
                            )
                            AlertDialog(
                                onDismissRequest = { showNewRecTimePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val newCal = Calendar.getInstance().apply { timeInMillis = newRecNextDueDate }
                                        newCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                        newCal.set(Calendar.MINUTE, timePickerState.minute)
                                        newRecNextDueDate = newCal.timeInMillis
                                        showNewRecTimePicker = false
                                    }) { Text("OK") }
                                },
                                dismissButton = { TextButton(onClick = { showNewRecTimePicker = false }) { Text("Cancel") } },
                                text = { TimePicker(state = timePickerState) }
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val amt = newRecAmount.toDoubleOrNull()
                                if (amt != null && amt > 0 && newRecNotes.isNotBlank()) {
                                    viewModel.addRecurringTransaction(
                                        amount = amt,
                                        notes = newRecNotes,
                                        type = newRecType,
                                        sourceWallet = newRecWallet,
                                        category = newRecCategory,
                                        intervalType = newRecInterval,
                                        nextDueDate = newRecNextDueDate
                                    )
                                    newRecAmount = ""
                                    newRecNotes = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_recurring_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add Recurring Plan", fontSize = 12.sp)
                        }

                        if (activeRecurring.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Active Scheduled Flows:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            var editingRec by remember { mutableStateOf<RecurringTransaction?>(null) }
                            
                            if (editingRec != null) {
                                var edAmount by remember { mutableStateOf(editingRec!!.amount.toString()) }
                                var edNotes by remember { mutableStateOf(editingRec!!.notes) }
                                var edInterval by remember { mutableStateOf(editingRec!!.intervalType) }
                                var edNextDue by remember { mutableStateOf(editingRec!!.nextDueDate) }
                                
                                AlertDialog(
                                    onDismissRequest = { editingRec = null },
                                    title = { Text("Edit Recurring Flow", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            AmountInputField(
                                                value = edAmount,
                                                onValueChange = { edAmount = it },
                                                label = { Text("Amount") }
                                            )
                                            OutlinedTextField(
                                                value = edNotes,
                                                onValueChange = { edNotes = it },
                                                label = { Text("Notes") },
                                                singleLine = true
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("Next Due:", fontSize = 12.sp)
                                                val fd = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(edNextDue))
                                                
                                                var showEditDatePicker by remember { mutableStateOf(false) }
                                                var showEditTimePicker by remember { mutableStateOf(false) }
                                                
                                                TextButton(onClick = { showEditDatePicker = true }) { Text(fd, fontSize = 12.sp) }

                                                if (showEditDatePicker) {
                                                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = edNextDue)
                                                    androidx.compose.material3.DatePickerDialog(
                                                        onDismissRequest = { showEditDatePicker = false },
                                                        confirmButton = {
                                                            TextButton(onClick = {
                                                                datePickerState.selectedDateMillis?.let {
                                                                    val currentCal = Calendar.getInstance().apply { timeInMillis = edNextDue }
                                                                    val newCal = Calendar.getInstance().apply { timeInMillis = it }
                                                                    newCal.set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                                                                    newCal.set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                                                                    edNextDue = newCal.timeInMillis
                                                                }
                                                                showEditDatePicker = false
                                                                showEditTimePicker = true
                                                            }) { Text("Next") }
                                                        },
                                                        dismissButton = { TextButton(onClick = { showEditDatePicker = false }) { Text("Cancel") } }
                                                    ) { DatePicker(state = datePickerState) }
                                                }

                                                if (showEditTimePicker) {
                                                    val cal = Calendar.getInstance().apply { timeInMillis = edNextDue }
                                                    val timePickerState = rememberTimePickerState(initialHour = cal.get(Calendar.HOUR_OF_DAY), initialMinute = cal.get(Calendar.MINUTE), is24Hour = false)
                                                    AlertDialog(
                                                        onDismissRequest = { showEditTimePicker = false },
                                                        confirmButton = {
                                                            TextButton(onClick = {
                                                                val newCal = Calendar.getInstance().apply { timeInMillis = edNextDue }
                                                                newCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                                                newCal.set(Calendar.MINUTE, timePickerState.minute)
                                                                edNextDue = newCal.timeInMillis
                                                                showEditTimePicker = false
                                                            }) { Text("OK") }
                                                        },
                                                        dismissButton = { TextButton(onClick = { showEditTimePicker = false }) { Text("Cancel") } },
                                                        text = { TimePicker(state = timePickerState) }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Apply changes to past payments too?", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(onClick = {
                                                    val amt = edAmount.toDoubleOrNull() ?: return@Button
                                                    viewModel.updateRecurringTransaction(
                                                        id = editingRec!!.id, amount = amt, notes = edNotes,
                                                        type = editingRec!!.type, sourceWallet = editingRec!!.sourceWallet,
                                                        category = editingRec!!.category, intervalType = edInterval,
                                                        nextDueDate = edNextDue, updatePastPayments = false
                                                    )
                                                    editingRec = null
                                                }) { Text("Future Only", fontSize = 11.sp) }
                                                Button(onClick = {
                                                    val amt = edAmount.toDoubleOrNull() ?: return@Button
                                                    viewModel.updateRecurringTransaction(
                                                        id = editingRec!!.id, amount = amt, notes = edNotes,
                                                        type = editingRec!!.type, sourceWallet = editingRec!!.sourceWallet,
                                                        category = editingRec!!.category, intervalType = edInterval,
                                                        nextDueDate = edNextDue, updatePastPayments = true
                                                    )
                                                    editingRec = null
                                                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                                                    Text("All Past & Future", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    },
                                    dismissButton = { TextButton(onClick = { editingRec = null }) { Text("Cancel") } }
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                activeRecurring.forEach { rec ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .clickable { editingRec = rec }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${rec.notes} (${rec.intervalType})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            val nextDStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(rec.nextDueDate))
                                            Text("₹ ${rec.amount} • Next: $nextDStr", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                            Text("${rec.category} • ${if (rec.sourceWallet == WalletType.EXPENSE_WALLET) "Expense" else "Savings"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteRecurringTransaction(rec) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("delete_rec_${rec.id}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Schedule", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
                val context = androidx.compose.ui.platform.LocalContext.current

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ledgerly Updates", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Current Version: v${com.parthjivani.ledgerly.BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                viewModel.checkForUpdates(isManual = true) { isNew, message ->
                                    if (message != null) {
                                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isCheckingUpdate,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                }
                Spacer(modifier = Modifier.height(100.dp)) // Extra padding for bottom nav
            }
        }
    }
}
