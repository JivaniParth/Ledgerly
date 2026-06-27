package com.parthjivani.hisabtracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parthjivani.hisabtracker.data.*
import com.parthjivani.hisabtracker.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.parthjivani.hisabtracker.ui.*
import com.parthjivani.hisabtracker.data.*
import com.parthjivani.hisabtracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryScreen(
    familyMembers: List<FamilyMember>,
    editingTransaction: Transaction? = null,
    onSubmitTransaction: (amount: Double, notes: String, type: TransactionType, source: WalletType, dest: WalletType?, memberId: Long?, dir: TransactionDirection?, date: Long, category: String, creditCardId: Long?) -> Unit,
    onNavigateToFamily: () -> Unit,
    viewModel: HisabViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val creditCards by viewModel.allCreditCards.collectAsState()

    val initialAmount = editingTransaction?.let {
        if (it.amount % 1.0 == 0.0) it.amount.toInt().toString() else it.amount.toString()
    } ?: ""

    val initialNotes = editingTransaction?.notes ?: ""

    val initialTabIndex = editingTransaction?.let {
        when (it.type) {
            TransactionType.PERSONAL_EXPENSE -> 0
            TransactionType.PERSONAL_INCOME -> 1
            TransactionType.FAMILY_TRANSFER -> 2
            TransactionType.WALLET_TRANSFER -> 3
        }
    } ?: 0

    val initialCategory = editingTransaction?.category ?: "Other"
    val initialDate = editingTransaction?.date ?: System.currentTimeMillis()

    // Form inputs state
    var amountStr by remember(editingTransaction) { mutableStateOf(initialAmount) }
    var notes by remember(editingTransaction) { mutableStateOf(initialNotes) }
    var selectedTabIndex by remember(editingTransaction) { mutableStateOf(initialTabIndex) } // [Expense] | [Income] | [Family Transfer] | [Wallet Transfer]
    var selectedCategory by remember(editingTransaction) { mutableStateOf(initialCategory) }
    var transactionDate by remember(editingTransaction) { mutableStateOf(initialDate) }

    // Wallet selection states
    val initialIncomeDepositWallet = editingTransaction?.let {
        if (it.type == TransactionType.PERSONAL_INCOME) it.sourceWallet else WalletType.EXPENSE_WALLET
    } ?: WalletType.EXPENSE_WALLET

    val initialSelectedExpenseWalletType = editingTransaction?.let {
        if (it.type == TransactionType.PERSONAL_EXPENSE) it.sourceWallet else WalletType.EXPENSE_WALLET
    } ?: WalletType.EXPENSE_WALLET

    val initialSelectedCreditCard = editingTransaction?.let { t ->
        if (t.type == TransactionType.PERSONAL_EXPENSE && t.sourceWallet == WalletType.CREDIT_CARD) {
            creditCards.find { it.id == t.creditCardId }
        } else null
    }

    // Family transfer selector states
    val initialFamilyMemberSelected = editingTransaction?.let { t ->
        if (t.type == TransactionType.FAMILY_TRANSFER) {
            familyMembers.find { it.id == t.familyMemberId }
        } else null
    }

    val initialFamilyDirectionChoice = editingTransaction?.let {
        if (it.type == TransactionType.FAMILY_TRANSFER) (it.direction ?: TransactionDirection.RECEIVED_FROM_THEM) else TransactionDirection.RECEIVED_FROM_THEM
    } ?: TransactionDirection.RECEIVED_FROM_THEM

    val initialFamilyTargetWallet = editingTransaction?.let {
        if (it.type == TransactionType.FAMILY_TRANSFER) it.sourceWallet else WalletType.EXPENSE_WALLET
    } ?: WalletType.EXPENSE_WALLET

    // Wallet Transfer selector states
    val initialWalletTransferSource = editingTransaction?.let {
        if (it.type == TransactionType.WALLET_TRANSFER) it.sourceWallet else WalletType.EXPENSE_WALLET
    } ?: WalletType.EXPENSE_WALLET

    val initialWalletTransferDest = editingTransaction?.let {
        if (it.type == TransactionType.WALLET_TRANSFER) (it.destinationWallet ?: WalletType.SAVINGS_WALLET) else WalletType.SAVINGS_WALLET
    } ?: WalletType.SAVINGS_WALLET

    val initialSelectedRepayCreditCard = editingTransaction?.let { t ->
        if (t.type == TransactionType.WALLET_TRANSFER && t.destinationWallet == WalletType.CREDIT_CARD) {
            creditCards.find { it.id == t.creditCardId }
        } else null
    }

    var incomeDepositWallet by remember(editingTransaction) { mutableStateOf(initialIncomeDepositWallet) }
    var selectedExpenseWalletType by remember(editingTransaction) { mutableStateOf(initialSelectedExpenseWalletType) }
    var selectedCreditCard by remember(editingTransaction, creditCards) { mutableStateOf(initialSelectedCreditCard ?: creditCards.firstOrNull()) }

    // Family transfer selector states
    var familyMemberSelected by remember(editingTransaction, familyMembers) { mutableStateOf(initialFamilyMemberSelected ?: familyMembers.firstOrNull()) }
    var familyDirectionChoice by remember(editingTransaction) { mutableStateOf(initialFamilyDirectionChoice) }
    var familyTargetWallet by remember(editingTransaction) { mutableStateOf(initialFamilyTargetWallet) }

    // Wallet Transfer selector states
    var walletTransferSource by remember(editingTransaction) { mutableStateOf(initialWalletTransferSource) }
    var walletTransferDest by remember(editingTransaction) { mutableStateOf(initialWalletTransferDest) }
    var selectedRepayCreditCard by remember(editingTransaction, creditCards) { mutableStateOf(initialSelectedRepayCreditCard ?: creditCards.firstOrNull()) }

    // Form errors
    var amountError by remember { mutableStateOf<String?>(null) }
    var familyError by remember { mutableStateOf<String?>(null) }

    // Dropdown expanded state
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Auto-select first family member if available
    LaunchedEffect(familyMembers) {
        if (editingTransaction != null && editingTransaction.type == TransactionType.FAMILY_TRANSFER) {
            val matching = familyMembers.find { it.id == editingTransaction.familyMemberId }
            if (matching != null) {
                familyMemberSelected = matching
            }
        } else if (familyMemberSelected == null && familyMembers.isNotEmpty()) {
            familyMemberSelected = familyMembers.first()
        }
    }

    LaunchedEffect(creditCards) {
        if (editingTransaction != null) {
            if (editingTransaction.type == TransactionType.PERSONAL_EXPENSE && editingTransaction.sourceWallet == WalletType.CREDIT_CARD) {
                val matching = creditCards.find { it.id == editingTransaction.creditCardId }
                if (matching != null) {
                    selectedCreditCard = matching
                }
            }
            if (editingTransaction.type == TransactionType.WALLET_TRANSFER && editingTransaction.destinationWallet == WalletType.CREDIT_CARD) {
                val matchingRepay = creditCards.find { it.id == editingTransaction.creditCardId }
                if (matchingRepay != null) {
                    selectedRepayCreditCard = matchingRepay
                }
            }
        }

        if (selectedCreditCard == null && creditCards.isNotEmpty()) {
            selectedCreditCard = creditCards.first()
        }
        if (selectedRepayCreditCard == null && creditCards.isNotEmpty()) {
            selectedRepayCreditCard = creditCards.first()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                AppLogo()
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (editingTransaction != null) "Edit Hisab Record" else "New Hisab Record",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
            }
        }
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .padding(horizontal = 16.dp)
                .testTag("entry_screen")
        ) {
            // Main tabs selector
            val tabTitles = listOf("Expense", "Income", "Family Transfer", "Wallet Transfer")
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("entry_tabs")
            ) {
                tabTitles.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTabIndex == i,
                        onClick = {
                            selectedTabIndex = i
                            amountError = null
                            familyError = null
                            if (editingTransaction == null) {
                                selectedCategory = if (i == 1) {
                                    val incCats = viewModel.incomeCategories.value
                                    if (incCats.isNotEmpty()) incCats.first() else "Salary"
                                } else if (i == 0) {
                                    val expCats = viewModel.categories.value
                                    if (expCats.isNotEmpty()) expCats.first() else "Groceries"
                                } else {
                                    "Other"
                                }
                            }
                        },
                        text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                        modifier = Modifier.testTag("tab_$i")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount Input Field
            OutlinedTextField(
                value = amountStr,
                onValueChange = {
                    amountStr = it
                    if (it.toDoubleOrNull() != null && it.toDouble() > 0) {
                        amountError = null
                    }
                },
                label = { Text("Amount (₹)") },
                placeholder = { Text("Enter Transaction Amount") },
                isError = amountError != null,
                supportingText = { amountError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_field"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Notes field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Purpose") },
                placeholder = { Text("Optional summary, e.g. Medicine purchase") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_field"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date Selection
            var showDatePickerDialog by remember { mutableStateOf(false) }
            val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

            OutlinedCard(
                onClick = { showDatePickerDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("date_selection_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Transaction Date",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateFormatter.format(Date(transactionDate)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Text(
                        text = "Change",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showDatePickerDialog) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = transactionDate
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePickerDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    transactionDate = it
                                }
                                showDatePickerDialog = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePickerDialog = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TAB CUSTOM OPTIONS AREA
            when (selectedTabIndex) {
                0 -> { // EXPENSE TAB (Daily expense vs Credit Card)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Source Wallet / Pay via:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedExpenseWalletType == WalletType.EXPENSE_WALLET) ExpenseColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(if (selectedExpenseWalletType == WalletType.EXPENSE_WALLET) 2.dp else 1.dp, if (selectedExpenseWalletType == WalletType.EXPENSE_WALLET) ExpenseColor else Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedExpenseWalletType = WalletType.EXPENSE_WALLET }
                            ) {
                                Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text("Expense Wallet", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedExpenseWalletType == WalletType.EXPENSE_WALLET) ExpenseColor else Color.Unspecified)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedExpenseWalletType == WalletType.CREDIT_CARD) TealGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(if (selectedExpenseWalletType == WalletType.CREDIT_CARD) 2.dp else 1.dp, if (selectedExpenseWalletType == WalletType.CREDIT_CARD) TealGreenPrimary else Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedExpenseWalletType = WalletType.CREDIT_CARD }
                            ) {
                                Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text("Credit Card", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedExpenseWalletType == WalletType.CREDIT_CARD) TealGreenPrimary else Color.Unspecified)
                                }
                            }
                        }

                        if (selectedExpenseWalletType == WalletType.CREDIT_CARD) {
                            if (creditCards.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToFamily() },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "⚠️ No Credit Cards registered. Tap here or go to Family Hub to add your first credit card.",
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            } else {
                                Text("Select Credit Card:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                var cardDropdownExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { cardDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = selectedCreditCard?.let { "${it.cardName} (${it.ownerName})" } ?: "Select Card",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = cardDropdownExpanded,
                                        onDismissRequest = { cardDropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        creditCards.forEach { card ->
                                            DropdownMenuItem(
                                                text = { Text("${card.cardName} (${card.ownerName})") },
                                                onClick = {
                                                    selectedCreditCard = card
                                                    cardDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text("Select Expense Category:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)

                        val dynamicCategoryList by viewModel.categories.collectAsState()
                        val categoryList = dynamicCategoryList.ifEmpty { listOf("Groceries", "Entertainment", "Bills", "Travel", "Medical", "Other") }
                        val scrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categoryList.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("entry_category_$cat")
                                )
                            }
                        }
                    }
                }
                1 -> { // INCOME TAB (Can deposit to either wallet)
                    Text("Select Wallet to Deposit Money:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (incomeDepositWallet == WalletType.EXPENSE_WALLET) ExpenseColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(if (incomeDepositWallet == WalletType.EXPENSE_WALLET) 2.dp else 1.dp, if (incomeDepositWallet == WalletType.EXPENSE_WALLET) ExpenseColor else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { incomeDepositWallet = WalletType.EXPENSE_WALLET }
                                .testTag("select_income_expense_wallet")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Expense Wallet", fontWeight = FontWeight.Bold, color = if (incomeDepositWallet == WalletType.EXPENSE_WALLET) ExpenseColor else Color.Unspecified)
                                Text("For immediate spending", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (incomeDepositWallet == WalletType.SAVINGS_WALLET) SavingsColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(if (incomeDepositWallet == WalletType.SAVINGS_WALLET) 2.dp else 1.dp, if (incomeDepositWallet == WalletType.SAVINGS_WALLET) SavingsColor else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { incomeDepositWallet = WalletType.SAVINGS_WALLET }
                                .testTag("select_income_savings_wallet")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Savings Wallet", fontWeight = FontWeight.Bold, color = if (incomeDepositWallet == WalletType.SAVINGS_WALLET) SavingsColor else Color.Unspecified)
                                Text("Long term savings", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Income Category:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    val dynamicIncomeCategoryList by viewModel.incomeCategories.collectAsState()
                    val incomeCategoryList = dynamicIncomeCategoryList.ifEmpty { listOf("Salary", "Business", "Investment", "Freelance", "Gift", "Other") }
                    val scrollStateIncome = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollStateIncome)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        incomeCategoryList.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("entry_income_category_$cat")
                            )
                        }
                    }
                }
                2 -> { // FAMILY TRANSFER
                    Text("Transfer Direction:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (familyDirectionChoice == TransactionDirection.RECEIVED_FROM_THEM) TealGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(if (familyDirectionChoice == TransactionDirection.RECEIVED_FROM_THEM) 2.dp else 1.dp, if (familyDirectionChoice == TransactionDirection.RECEIVED_FROM_THEM) TealGreenPrimary else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { familyDirectionChoice = TransactionDirection.RECEIVED_FROM_THEM }
                                .testTag("direction_received")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Inflow", tint = TealGreenPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Received From Them (+)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (familyDirectionChoice == TransactionDirection.RECEIVED_FROM_THEM) TealGreenPrimary else Color.Unspecified)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (familyDirectionChoice == TransactionDirection.GIVEN_TO_THEM) ExpenseColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(if (familyDirectionChoice == TransactionDirection.GIVEN_TO_THEM) 2.dp else 1.dp, if (familyDirectionChoice == TransactionDirection.GIVEN_TO_THEM) ExpenseColor else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { familyDirectionChoice = TransactionDirection.GIVEN_TO_THEM }
                                .testTag("direction_given")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Outflow", tint = ExpenseColor)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Given To Them (-)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (familyDirectionChoice == TransactionDirection.GIVEN_TO_THEM) ExpenseColor else Color.Unspecified)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Select Family Member:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (familyMembers.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("No family members registered.", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                            Button(onClick = onNavigateToFamily, modifier = Modifier.testTag("navigate_to_add_family")) {
                                Text("Create Member", fontSize = 11.sp)
                            }
                        }
                    } else {
                        // Dropdown to select member
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("family_member_dropdown"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = familyMemberSelected?.let { "${it.name} (${it.relationship})" } ?: "Select Member",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                familyMembers.forEach { member ->
                                    DropdownMenuItem(
                                        text = { Text("${member.name} (${member.relationship})") },
                                        onClick = {
                                            familyMemberSelected = member
                                            dropdownExpanded = false
                                            familyError = null
                                        },
                                        modifier = Modifier.testTag("dropdown_item_${member.id}")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Wallet Selector (Which wallet gets affected):", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (familyTargetWallet == WalletType.EXPENSE_WALLET) ExpenseColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(if (familyTargetWallet == WalletType.EXPENSE_WALLET) 2.dp else 1.dp, if (familyTargetWallet == WalletType.EXPENSE_WALLET) ExpenseColor else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { familyTargetWallet = WalletType.EXPENSE_WALLET }
                                .testTag("apply_expense_wallet")
                        ) {
                            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                Text("Apply to Expense Wallet", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (familyTargetWallet == WalletType.EXPENSE_WALLET) ExpenseColor else Color.Unspecified)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (familyTargetWallet == WalletType.SAVINGS_WALLET) SavingsColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(if (familyTargetWallet == WalletType.SAVINGS_WALLET) 2.dp else 1.dp, if (familyTargetWallet == WalletType.SAVINGS_WALLET) SavingsColor else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { familyTargetWallet = WalletType.SAVINGS_WALLET }
                                .testTag("apply_savings_wallet")
                        ) {
                            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                Text("Apply to Savings Wallet", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (familyTargetWallet == WalletType.SAVINGS_WALLET) SavingsColor else Color.Unspecified)
                            }
                        }
                    }
                }
                3 -> { // WALLET TRANSFER (Operational & Credit Card dues payback)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Select Transfer Type:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (walletTransferSource == WalletType.EXPENSE_WALLET && walletTransferDest == WalletType.SAVINGS_WALLET) ExpenseColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(if (walletTransferSource == WalletType.EXPENSE_WALLET && walletTransferDest == WalletType.SAVINGS_WALLET) 2.dp else 1.dp, if (walletTransferSource == WalletType.EXPENSE_WALLET && walletTransferDest == WalletType.SAVINGS_WALLET) ExpenseColor else Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        walletTransferSource = WalletType.EXPENSE_WALLET
                                        walletTransferDest = WalletType.SAVINGS_WALLET
                                    }
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Expense ➔ Savings", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Deduct from Daily Expense", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (walletTransferSource == WalletType.SAVINGS_WALLET && walletTransferDest == WalletType.EXPENSE_WALLET) SavingsColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(if (walletTransferSource == WalletType.SAVINGS_WALLET && walletTransferDest == WalletType.EXPENSE_WALLET) 2.dp else 1.dp, if (walletTransferSource == WalletType.SAVINGS_WALLET && walletTransferDest == WalletType.EXPENSE_WALLET) SavingsColor else Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        walletTransferSource = WalletType.SAVINGS_WALLET
                                        walletTransferDest = WalletType.EXPENSE_WALLET
                                    }
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Savings ➔ Expense", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Top up Daily Expense", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (walletTransferDest == WalletType.CREDIT_CARD) TealGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(if (walletTransferDest == WalletType.CREDIT_CARD) 2.dp else 1.dp, if (walletTransferDest == WalletType.CREDIT_CARD) TealGreenPrimary else Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        walletTransferSource = WalletType.EXPENSE_WALLET
                                        walletTransferDest = WalletType.CREDIT_CARD
                                    }
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Repay Card", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TealGreenPrimary)
                                    Text("Expense ➔ Card Dues", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (walletTransferDest == WalletType.CREDIT_CARD) {
                            if (creditCards.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToFamily() },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "⚠️ No Credit Cards registered. Tap here to add your first credit card.",
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            } else {
                                Text("Select Card to Pay off Dues:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                var cardRepayMenuExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { cardRepayMenuExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = selectedRepayCreditCard?.let { "${it.cardName} (${it.ownerName})" } ?: "Select Card",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = cardRepayMenuExpanded,
                                        onDismissRequest = { cardRepayMenuExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        creditCards.forEach { card ->
                                            DropdownMenuItem(
                                                text = { Text("${card.cardName} (${card.ownerName})") },
                                                onClick = {
                                                    selectedRepayCreditCard = card
                                                    cardRepayMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // SUBMIT RECORD BUTTON
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        amountError = "Please enter a valid positive number."
                        return@Button
                    }
                    if (selectedTabIndex == 2 && familyMemberSelected == null) {
                        familyError = "Please select a family member."
                        return@Button
                    }

                    keyboardController?.hide()

                    when (selectedTabIndex) {
                        0 -> { // Personal Expense (can be expense wallet vs credit card)
                            if (selectedExpenseWalletType == WalletType.CREDIT_CARD) {
                                onSubmitTransaction(amount, notes, TransactionType.PERSONAL_EXPENSE, WalletType.CREDIT_CARD, null, null, null, transactionDate, selectedCategory, selectedCreditCard?.id)
                            } else {
                                onSubmitTransaction(amount, notes, TransactionType.PERSONAL_EXPENSE, WalletType.EXPENSE_WALLET, null, null, null, transactionDate, selectedCategory, null)
                            }
                        }
                        1 -> { // Personal Income
                            onSubmitTransaction(amount, notes, TransactionType.PERSONAL_INCOME, incomeDepositWallet, null, null, null, transactionDate, selectedCategory, null)
                        }
                        2 -> { // Family Transfer
                            onSubmitTransaction(amount, notes, TransactionType.FAMILY_TRANSFER, familyTargetWallet, null, familyMemberSelected!!.id, familyDirectionChoice, transactionDate, "Other", null)
                        }
                        3 -> { // Wallet Transfer
                            if (walletTransferDest == WalletType.CREDIT_CARD) {
                                onSubmitTransaction(amount, notes, TransactionType.WALLET_TRANSFER, WalletType.EXPENSE_WALLET, WalletType.CREDIT_CARD, null, null, transactionDate, "Other", selectedRepayCreditCard?.id)
                            } else {
                                onSubmitTransaction(amount, notes, TransactionType.WALLET_TRANSFER, walletTransferSource, walletTransferDest, null, null, transactionDate, "Other", null)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_transaction_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (editingTransaction != null) "Save Changes" else "Confirm & Record Transaction",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
