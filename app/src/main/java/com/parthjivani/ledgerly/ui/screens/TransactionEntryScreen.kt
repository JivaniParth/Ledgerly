package com.parthjivani.ledgerly.ui.screens

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
import com.parthjivani.ledgerly.ui.components.AmountInputField
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
import com.parthjivani.ledgerly.data.*
import com.parthjivani.ledgerly.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.parthjivani.ledgerly.ui.*
import com.parthjivani.ledgerly.data.*
import com.parthjivani.ledgerly.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryScreen(
    familyMembers: List<FamilyMember>,
    editingTransaction: Transaction? = null,
    onSubmitTransaction: (amount: Double, notes: String, type: TransactionType, source: WalletType, dest: WalletType?, memberId: Long?, dir: TransactionDirection?, date: Long, category: String, creditCardId: Long?, customWalletId: Long?, destinationCustomWalletId: Long?) -> Unit,
    onNavigateToFamily: () -> Unit,
    viewModel: LedgerlyViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val creditCards by viewModel.allCreditCards.collectAsState()
    val customWallets by viewModel.allCustomWallets.collectAsState()

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

    var selectedCustomWalletSource by remember(editingTransaction, customWallets) { 
        mutableStateOf(
            editingTransaction?.let { t -> 
                if (t.sourceWallet == WalletType.TEMPORARY_WALLET) customWallets.find { it.id == t.customWalletId } else null 
            } ?: customWallets.firstOrNull()
        ) 
    }
    var selectedCustomWalletDest by remember(editingTransaction, customWallets) { 
        mutableStateOf(
            editingTransaction?.let { t -> 
                if (t.destinationWallet == WalletType.TEMPORARY_WALLET) customWallets.find { it.id == t.destinationCustomWalletId } else null 
            } ?: customWallets.firstOrNull()
        ) 
    }

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

    LaunchedEffect(customWallets) {
        if (selectedCustomWalletSource == null && customWallets.isNotEmpty()) {
            selectedCustomWalletSource = customWallets.first()
        }
        if (selectedCustomWalletDest == null && customWallets.isNotEmpty()) {
            selectedCustomWalletDest = customWallets.first()
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
                    text = if (editingTransaction != null) "Edit Ledgerly Record" else "New Ledgerly Record",
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
                .verticalScroll(rememberScrollState())
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
            AmountInputField(
                value = amountStr,
                onValueChange = {
                    amountStr = it
                    if (it.toDoubleOrNull() != null && it.toDouble() > 0) {
                        amountError = null
                    }
                },
                label = { Text("Amount (₹)") },
                isError = amountError != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_field")
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

            // Date & Time Selection
            var showDatePickerDialog by remember { mutableStateOf(false) }
            var showTimePickerDialog by remember { mutableStateOf(false) }
            val dateTimeFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
            val context = androidx.compose.ui.platform.LocalContext.current

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
                            contentDescription = "Select Date & Time",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Transaction Date & Time",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateTimeFormatter.format(Date(transactionDate)),
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
                                datePickerState.selectedDateMillis?.let { selectedDate ->
                                    val currentCal = Calendar.getInstance().apply { timeInMillis = transactionDate }
                                    val newCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                    newCal.set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                                    newCal.set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                                    transactionDate = newCal.timeInMillis
                                }
                                showDatePickerDialog = false
                                showTimePickerDialog = true
                            }
                        ) {
                            Text("Next")
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

            if (showTimePickerDialog) {
                val cal = Calendar.getInstance().apply { timeInMillis = transactionDate }
                android.app.TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val newCal = Calendar.getInstance().apply { timeInMillis = transactionDate }
                        newCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        newCal.set(Calendar.MINUTE, minute)
                        transactionDate = newCal.timeInMillis
                        showTimePickerDialog = false
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false // 12-hour format
                ).apply {
                    setOnDismissListener { showTimePickerDialog = false }
                }.show()
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
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedExpenseWalletType == WalletType.EXPENSE_WALLET) ExpenseColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(if (selectedExpenseWalletType == WalletType.EXPENSE_WALLET) 2.dp else 1.dp, if (selectedExpenseWalletType == WalletType.EXPENSE_WALLET) ExpenseColor else Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedExpenseWalletType = WalletType.EXPENSE_WALLET }
                            ) {
                                Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text("Expense Wallet", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedExpenseWalletType == WalletType.EXPENSE_WALLET) ExpenseColor else Color.Unspecified, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedExpenseWalletType == WalletType.CREDIT_CARD) TealGreenPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(if (selectedExpenseWalletType == WalletType.CREDIT_CARD) 2.dp else 1.dp, if (selectedExpenseWalletType == WalletType.CREDIT_CARD) TealGreenPrimary else Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedExpenseWalletType = WalletType.CREDIT_CARD }
                            ) {
                                Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text("Credit Card", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedExpenseWalletType == WalletType.CREDIT_CARD) TealGreenPrimary else Color.Unspecified, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                            
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedExpenseWalletType == WalletType.TEMPORARY_WALLET) FinanceTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(if (selectedExpenseWalletType == WalletType.TEMPORARY_WALLET) 2.dp else 1.dp, if (selectedExpenseWalletType == WalletType.TEMPORARY_WALLET) FinanceTeal else Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedExpenseWalletType = WalletType.TEMPORARY_WALLET }
                            ) {
                                Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text("Temporary", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedExpenseWalletType == WalletType.TEMPORARY_WALLET) FinanceTeal else Color.Unspecified, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }

                        if (selectedExpenseWalletType == WalletType.TEMPORARY_WALLET) {
                            if (customWallets.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "⚠️ No Temporary Wallets found. Create one in Wallets screen.",
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            } else {
                                Text("Select Temporary Wallet:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                var customWalletDropdownExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { customWalletDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = selectedCustomWalletSource?.name ?: "Select Wallet",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = customWalletDropdownExpanded,
                                        onDismissRequest = { customWalletDropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        customWallets.forEach { cw ->
                                            DropdownMenuItem(
                                                text = { Text(cw.name) },
                                                onClick = {
                                                    selectedCustomWalletSource = cw
                                                    customWalletDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
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
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (incomeDepositWallet == WalletType.EXPENSE_WALLET) ExpenseColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(if (incomeDepositWallet == WalletType.EXPENSE_WALLET) 2.dp else 1.dp, if (incomeDepositWallet == WalletType.EXPENSE_WALLET) ExpenseColor else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { incomeDepositWallet = WalletType.EXPENSE_WALLET }
                                .testTag("select_income_expense_wallet")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Expense Wallet", fontWeight = FontWeight.Bold, color = if (incomeDepositWallet == WalletType.EXPENSE_WALLET) ExpenseColor else Color.Unspecified, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Text("For immediate spending", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (incomeDepositWallet == WalletType.SAVINGS_WALLET) SavingsColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(if (incomeDepositWallet == WalletType.SAVINGS_WALLET) 2.dp else 1.dp, if (incomeDepositWallet == WalletType.SAVINGS_WALLET) SavingsColor else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { incomeDepositWallet = WalletType.SAVINGS_WALLET }
                                .testTag("select_income_savings_wallet")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Savings Wallet", fontWeight = FontWeight.Bold, color = if (incomeDepositWallet == WalletType.SAVINGS_WALLET) SavingsColor else Color.Unspecified, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Text("Long term savings", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (incomeDepositWallet == WalletType.TEMPORARY_WALLET) FinanceTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(if (incomeDepositWallet == WalletType.TEMPORARY_WALLET) 2.dp else 1.dp, if (incomeDepositWallet == WalletType.TEMPORARY_WALLET) FinanceTeal else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { incomeDepositWallet = WalletType.TEMPORARY_WALLET }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Temp Wallet", fontWeight = FontWeight.Bold, color = if (incomeDepositWallet == WalletType.TEMPORARY_WALLET) FinanceTeal else Color.Unspecified, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Text("Events/Trips", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }

                    if (incomeDepositWallet == WalletType.TEMPORARY_WALLET) {
                        if (customWallets.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "⚠️ No Temporary Wallets found. Create one in Wallets screen.",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        } else {
                            Text("Select Temporary Wallet:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
                            var customWalletDropdownExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                OutlinedButton(
                                    onClick = { customWalletDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedCustomWalletSource?.name ?: "Select Wallet",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                    }
                                }
                                DropdownMenu(
                                    expanded = customWalletDropdownExpanded,
                                    onDismissRequest = { customWalletDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    customWallets.forEach { cw ->
                                        DropdownMenuItem(
                                            text = { Text(cw.name) },
                                            onClick = {
                                                selectedCustomWalletSource = cw
                                                customWalletDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
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
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (familyTargetWallet == WalletType.EXPENSE_WALLET) ExpenseColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(if (familyTargetWallet == WalletType.EXPENSE_WALLET) 2.dp else 1.dp, if (familyTargetWallet == WalletType.EXPENSE_WALLET) ExpenseColor else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { familyTargetWallet = WalletType.EXPENSE_WALLET }
                                .testTag("apply_expense_wallet")
                        ) {
                            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                Text("Apply to Expense Wallet", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (familyTargetWallet == WalletType.EXPENSE_WALLET) ExpenseColor else Color.Unspecified, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (familyTargetWallet == WalletType.SAVINGS_WALLET) SavingsColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(if (familyTargetWallet == WalletType.SAVINGS_WALLET) 2.dp else 1.dp, if (familyTargetWallet == WalletType.SAVINGS_WALLET) SavingsColor else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { familyTargetWallet = WalletType.SAVINGS_WALLET }
                                .testTag("apply_savings_wallet")
                        ) {
                            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                Text("Savings", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (familyTargetWallet == WalletType.SAVINGS_WALLET) SavingsColor else Color.Unspecified, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (familyTargetWallet == WalletType.TEMPORARY_WALLET) FinanceTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(if (familyTargetWallet == WalletType.TEMPORARY_WALLET) 2.dp else 1.dp, if (familyTargetWallet == WalletType.TEMPORARY_WALLET) FinanceTeal else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { familyTargetWallet = WalletType.TEMPORARY_WALLET }
                        ) {
                            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                Text("Temporary", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (familyTargetWallet == WalletType.TEMPORARY_WALLET) FinanceTeal else Color.Unspecified, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                    
                    if (familyTargetWallet == WalletType.TEMPORARY_WALLET) {
                        if (customWallets.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "⚠️ No Temporary Wallets found. Create one in Wallets screen.",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        } else {
                            Text("Select Temporary Wallet:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
                            var customWalletDropdownExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                OutlinedButton(
                                    onClick = { customWalletDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedCustomWalletSource?.name ?: "Select Wallet",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                    }
                                }
                                DropdownMenu(
                                    expanded = customWalletDropdownExpanded,
                                    onDismissRequest = { customWalletDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    customWallets.forEach { cw ->
                                        DropdownMenuItem(
                                            text = { Text(cw.name) },
                                            onClick = {
                                                selectedCustomWalletSource = cw
                                                customWalletDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> { // WALLET TRANSFER
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Source Selection
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Transfer From:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(WalletType.EXPENSE_WALLET, WalletType.SAVINGS_WALLET, WalletType.TEMPORARY_WALLET).forEach { wType ->
                                    val isSelected = walletTransferSource == wType
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { walletTransferSource = wType },
                                        label = { 
                                            Text(
                                                when(wType) {
                                                    WalletType.EXPENSE_WALLET -> "Expense"
                                                    WalletType.SAVINGS_WALLET -> "Savings"
                                                    else -> "Temporary"
                                                }
                                            ) 
                                        },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                                    )
                                }
                            }
                            if (walletTransferSource == WalletType.TEMPORARY_WALLET) {
                                if (customWallets.isEmpty()) {
                                    Text("⚠️ No Temporary Wallets found.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                } else {
                                    var srcDropdownExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(onClick = { srcDropdownExpanded = true }) {
                                            Text(selectedCustomWalletSource?.name ?: "Select Wallet")
                                        }
                                        DropdownMenu(expanded = srcDropdownExpanded, onDismissRequest = { srcDropdownExpanded = false }) {
                                            customWallets.forEach { cw ->
                                                DropdownMenuItem(text = { Text(cw.name) }, onClick = { selectedCustomWalletSource = cw; srcDropdownExpanded = false })
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Dest Selection
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Transfer To:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(WalletType.EXPENSE_WALLET, WalletType.SAVINGS_WALLET, WalletType.CREDIT_CARD, WalletType.TEMPORARY_WALLET).forEach { wType ->
                                    val isSelected = walletTransferDest == wType
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { walletTransferDest = wType },
                                        label = { 
                                            Text(
                                                when(wType) {
                                                    WalletType.EXPENSE_WALLET -> "Expense"
                                                    WalletType.SAVINGS_WALLET -> "Savings"
                                                    WalletType.CREDIT_CARD -> "Credit Card (Repay)"
                                                    else -> "Temporary"
                                                }
                                            ) 
                                        },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                                    )
                                }
                            }
                            if (walletTransferDest == WalletType.TEMPORARY_WALLET) {
                                if (customWallets.isEmpty()) {
                                    Text("⚠️ No Temporary Wallets found.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                } else {
                                    var destDropdownExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(onClick = { destDropdownExpanded = true }) {
                                            Text(selectedCustomWalletDest?.name ?: "Select Wallet")
                                        }
                                        DropdownMenu(expanded = destDropdownExpanded, onDismissRequest = { destDropdownExpanded = false }) {
                                            customWallets.forEach { cw ->
                                                DropdownMenuItem(text = { Text(cw.name) }, onClick = { selectedCustomWalletDest = cw; destDropdownExpanded = false })
                                            }
                                        }
                                    }
                                }
                            }
                            if (walletTransferDest == WalletType.CREDIT_CARD) {
                                if (creditCards.isEmpty()) {
                                    Text("⚠️ No Credit Cards registered.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                } else {
                                    var cardDropdownExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(onClick = { cardDropdownExpanded = true }) {
                                            Text(selectedRepayCreditCard?.cardName ?: "Select Card")
                                        }
                                        DropdownMenu(expanded = cardDropdownExpanded, onDismissRequest = { cardDropdownExpanded = false }) {
                                            creditCards.forEach { card ->
                                                DropdownMenuItem(text = { Text(card.cardName) }, onClick = { selectedRepayCreditCard = card; cardDropdownExpanded = false })
                                            }
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
                                onSubmitTransaction(amount, notes, TransactionType.PERSONAL_EXPENSE, WalletType.CREDIT_CARD, null, null, null, transactionDate, selectedCategory, selectedCreditCard?.id, null, null)
                            } else if (selectedExpenseWalletType == WalletType.TEMPORARY_WALLET) {
                                onSubmitTransaction(amount, notes, TransactionType.PERSONAL_EXPENSE, WalletType.TEMPORARY_WALLET, null, null, null, transactionDate, selectedCategory, null, selectedCustomWalletSource?.id, null)
                            } else {
                                onSubmitTransaction(amount, notes, TransactionType.PERSONAL_EXPENSE, WalletType.EXPENSE_WALLET, null, null, null, transactionDate, selectedCategory, null, null, null)
                            }
                        }
                        1 -> { // Personal Income
                            onSubmitTransaction(amount, notes, TransactionType.PERSONAL_INCOME, incomeDepositWallet, null, null, null, transactionDate, selectedCategory, null, if (incomeDepositWallet == WalletType.TEMPORARY_WALLET) selectedCustomWalletSource?.id else null, null)
                        }
                        2 -> { // Family Transfer
                            onSubmitTransaction(amount, notes, TransactionType.FAMILY_TRANSFER, familyTargetWallet, null, familyMemberSelected!!.id, familyDirectionChoice, transactionDate, "Other", null, if (familyTargetWallet == WalletType.TEMPORARY_WALLET) selectedCustomWalletSource?.id else null, null)
                        }
                        3 -> { // Wallet Transfer
                            val cId = if (walletTransferSource == WalletType.TEMPORARY_WALLET) selectedCustomWalletSource?.id else null
                            val destCId = if (walletTransferDest == WalletType.TEMPORARY_WALLET) selectedCustomWalletDest?.id else null
                            val cardId = if (walletTransferDest == WalletType.CREDIT_CARD) selectedRepayCreditCard?.id else null
                            onSubmitTransaction(amount, notes, TransactionType.WALLET_TRANSFER, walletTransferSource, walletTransferDest, null, null, transactionDate, "Other", cardId, cId, destCId)
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

