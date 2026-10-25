package com.parthjivani.ledgerly.ui

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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.parthjivani.ledgerly.ui.screens.AuthScreen
import com.parthjivani.ledgerly.data.*
import com.parthjivani.ledgerly.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.parthjivani.ledgerly.ui.screens.*
import com.parthjivani.ledgerly.ui.screens.ProfileScreen

enum class AppScreen(
    val label: String,
    val iconSelected: androidx.compose.ui.graphics.vector.ImageVector,
    val iconUnselected: androidx.compose.ui.graphics.vector.ImageVector,
    val isMainTab: Boolean = true
) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
    WALLETS("Wallets", Icons.Default.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    TRANSACTIONS("Transactions", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    FAMILY("Family Hub", Icons.Default.People, Icons.Outlined.People),
    ENTRY("New Entry", Icons.Default.Add, Icons.Outlined.Add, isMainTab = false),
    PROJECTOR("Stats", Icons.Default.BarChart, Icons.Outlined.BarChart, isMainTab = false),
    SETTINGS("Settings", Icons.Default.Settings, Icons.Outlined.Settings, isMainTab = false),
    PROFILE("Profile", Icons.Default.Person, Icons.Outlined.Person, isMainTab = false),
    EXCEL_IMPORT("Import Excel", androidx.compose.material.icons.Icons.Default.CloudUpload, androidx.compose.material.icons.Icons.Outlined.CloudUpload, isMainTab = false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: LedgerlyViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    
    if (!isLoggedIn) {
        AuthScreen(viewModel)
        return
    }
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.DASHBOARD) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    // Back navigation screen history stack
    var screenHistory by remember { mutableStateOf(listOf(AppScreen.DASHBOARD)) }

    fun navigateTo(screen: AppScreen, resetStack: Boolean = false) {
        if (resetStack || screen == AppScreen.DASHBOARD) {
            screenHistory = listOf(AppScreen.DASHBOARD)
        } else {
            val updated = screenHistory.toMutableList()
            if (updated.lastOrNull() != screen) {
                val index = updated.indexOf(screen)
                if (index != -1) {
                    // Prevent circular backstack loops by trimming
                    screenHistory = updated.subList(0, index + 1)
                } else {
                    updated.add(screen)
                    screenHistory = updated
                }
            }
        }
        currentScreen = screen
    }

    // Intercept back pressed events when we can go back in history
    BackHandler(enabled = screenHistory.size > 1) {
        val updated = screenHistory.toMutableList()
        if (updated.size > 1) {
            updated.removeAt(updated.lastIndex)
            screenHistory = updated
            currentScreen = updated.last()
        }
    }

    // Observers
    val context = androidx.compose.ui.platform.LocalContext.current
    val syncSuccess by viewModel.supabaseSyncSuccess.collectAsState()
    val syncError by viewModel.supabaseSyncError.collectAsState()

    androidx.compose.runtime.LaunchedEffect(syncSuccess) {
        syncSuccess?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.resetSupabaseSyncMessages()
        }
    }

    androidx.compose.runtime.LaunchedEffect(syncError) {
        syncError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.resetSupabaseSyncMessages()
        }
    }

    val transactions by viewModel.allTransactions.collectAsState()
    val familyMembers by viewModel.allFamilyMembers.collectAsState()
    val expenseBalance by viewModel.expenseWalletBalance.collectAsState()
    val savingsBalance by viewModel.savingsWalletBalance.collectAsState()
    val projectedAverage by viewModel.averageMonthlyExpenseSpending.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        bottomBar = {
            val navOutlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            NavigationBar(
                modifier = Modifier
                    .testTag("bottom_nav_bar")
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .drawBehind {
                        drawLine(
                            color = navOutlineColor,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                AppScreen.values().filter { it.isMainTab }.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            editingTransaction = null
                            navigateTo(screen)
                        },
                        label = { Text(text = screen.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.iconSelected else screen.iconUnselected,
                                contentDescription = screen.label
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            var isSwipeEnabled by remember { mutableStateOf(true) }
            val mainTabs = remember { AppScreen.values().filter { it.isMainTab } }
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                initialPage = mainTabs.indexOf(currentScreen).coerceAtLeast(0),
                pageCount = { mainTabs.size }
            )
            val coroutineScope = rememberCoroutineScope()

            // Sync pager to currentScreen if currentScreen is changed from outside (e.g., bottom nav)
            LaunchedEffect(currentScreen) {
                val index = mainTabs.indexOf(currentScreen)
                if (index != -1 && pagerState.currentPage != index) {
                    pagerState.animateScrollToPage(index)
                }
            }

            // Sync currentScreen to pager if pager is swiped
            LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                if (!pagerState.isScrollInProgress) {
                    val newScreen = mainTabs[pagerState.currentPage]
                    if (currentScreen != newScreen && mainTabs.contains(currentScreen)) {
                        navigateTo(newScreen)
                    }
                }
            }
            
            // Render the HorizontalPager always as the base layer
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                userScrollEnabled = isSwipeEnabled,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val screen = mainTabs[page]
                when (screen) {
                    AppScreen.DASHBOARD -> DashboardScreen(
                        expenseBalance = expenseBalance,
                        savingsBalance = savingsBalance,
                        transactions = transactions,
                        familyMembers = familyMembers,
                        onAddTransactionClick = {
                            editingTransaction = null
                            navigateTo(AppScreen.ENTRY)
                        },
                        onNavigateToAnalytics = { navigateTo(AppScreen.PROJECTOR) },
                        onNavigateToProfile = { navigateTo(AppScreen.PROFILE) },
                        onNavigateToSettings = { navigateTo(AppScreen.SETTINGS) },
                        viewModel = viewModel,
                        onScrollStateChange = { isScrolled -> isSwipeEnabled = !isScrolled }
                    )
                    AppScreen.WALLETS -> WalletsScreen(
                        viewModel = viewModel,
                        onNavigateToTransactions = { navigateTo(AppScreen.TRANSACTIONS) },
                        onScrollStateChange = { isScrolled -> isSwipeEnabled = !isScrolled }
                    )
                    AppScreen.TRANSACTIONS -> TransactionsScreen(
                        viewModel = viewModel,
                        onAddTransactionClick = {
                            editingTransaction = null
                            navigateTo(AppScreen.ENTRY)
                        },
                        onEditTransactionClick = { transaction ->
                            editingTransaction = transaction
                            navigateTo(AppScreen.ENTRY)
                        },
                        onScrollStateChange = { isScrolled -> isSwipeEnabled = !isScrolled }
                    )
                    AppScreen.FAMILY -> FamilyHubScreen(
                        familyMembers = familyMembers,
                        transactions = transactions,
                        viewModel = viewModel,
                        onAddNewMember = { name, relation ->
                            viewModel.addFamilyMember(name, relation)
                        },
                        onFamilyMemberClick = { memberId ->
                            viewModel.setTxMemberFilter(memberId)
                            viewModel.setTxCardFilter(null)
                            navigateTo(AppScreen.TRANSACTIONS)
                        },
                        onCreditCardClick = { cardId ->
                            viewModel.setTxCardFilter(cardId)
                            viewModel.setTxMemberFilter(null)
                            navigateTo(AppScreen.TRANSACTIONS)
                        },
                        onScrollStateChange = { isScrolled -> isSwipeEnabled = !isScrolled }
                    )
                    else -> {}
                }
            }
            
            // Render non-main tabs as overlays with AnimatedContent
            if (!currentScreen.isMainTab) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    },
                    label = "OverlayTransition",
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) { targetScreen ->
                    when (targetScreen) {
                        AppScreen.ENTRY -> TransactionEntryScreen(
                            familyMembers = familyMembers,
                            editingTransaction = editingTransaction,
                            onSubmitTransaction = { amount, notes, type, source, dest, memberId, dir, date, category, creditCardId, customWalletId, destCustomWalletId, contactId, splits ->
                                if (editingTransaction != null) {
                                    viewModel.updateTransaction(editingTransaction!!.id, amount, notes, type, source, dest, memberId, dir, date, category, creditCardId, customWalletId, destCustomWalletId, contactId, splits)
                                } else {
                                    viewModel.addTransaction(amount, notes, type, source, dest, memberId, dir, date, category, creditCardId, customWalletId, destCustomWalletId, contactId, splits)
                                }
                                editingTransaction = null
                                
                                val updated = screenHistory.toMutableList()
                                if (updated.size > 1 && updated.lastOrNull() == AppScreen.ENTRY) {
                                    updated.removeAt(updated.lastIndex)
                                    screenHistory = updated
                                    currentScreen = updated.last()
                                } else {
                                    navigateTo(AppScreen.DASHBOARD)
                                }
                            },
                            onNavigateToFamily = { navigateTo(AppScreen.FAMILY) },
                            viewModel = viewModel
                        )
                        AppScreen.PROJECTOR -> AnalyticsScreen(
                            transactions = transactions,
                            projectedAverage = projectedAverage,
                            viewModel = viewModel
                        )
                        AppScreen.SETTINGS -> SettingsScreen(
                            onNavigateBack = { navigateTo(AppScreen.DASHBOARD) },
                            onNavigateToExcelImport = { navigateTo(AppScreen.EXCEL_IMPORT) },
                            viewModel = viewModel
                        )
                        AppScreen.EXCEL_IMPORT -> ExcelImportScreen(
                            onNavigateBack = { navigateTo(AppScreen.SETTINGS) },
                            viewModel = viewModel
                        )
                        AppScreen.PROFILE -> ProfileScreen(
                            onNavigateBack = { navigateTo(AppScreen.DASHBOARD) },
                            onNavigateToSettings = { navigateTo(AppScreen.SETTINGS) },
                            viewModel = viewModel
                        )
                        else -> {}
                    }
                }
            }

            // In-App Auto-Update Dialog
            val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
            val updateInfo by viewModel.updateInfo.collectAsState()
            val downloadState by viewModel.downloadState.collectAsState()

            if (showUpdateDialog && updateInfo != null) {
                com.parthjivani.ledgerly.ui.components.AppUpdateDialog(
                    updateInfo = updateInfo!!,
                    downloadState = downloadState,
                    onStartDownload = { viewModel.startUpdateDownload(context) },
                    onInstall = { viewModel.installDownloadedApk(context) },
                    onDismiss = { viewModel.dismissUpdateDialog() }
                )
            }
        }
    }
}

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(5.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = "LEDGERLY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
    }
}
// FORMATTERS
// Indian number system: ₹ 1,000 → ₹ 10,000 → ₹ 1,00,000 (lakh) → ₹ 1,00,00,000 (crore)
private val indianNumberFormat = java.text.NumberFormat.getInstance(java.util.Locale.forLanguageTag("en-IN"))

fun formatCurrency(amount: Double): String {
    val absolute = Math.abs(amount)
    val nf = indianNumberFormat
    val formatted: String = if (absolute % 1.0 == 0.0) {
        nf.minimumFractionDigits = 0
        nf.maximumFractionDigits = 0
        "₹ ${nf.format(absolute)}"
    } else {
        nf.minimumFractionDigits = 0
        nf.maximumFractionDigits = 2
        val raw = nf.format(absolute)
        // Strip trailing zeros: 150.50 → ₹ 150.5, 150.75 → ₹ 150.75
        val stripped = if (raw.contains('.')) raw.trimEnd('0').trimEnd('.') else raw
        "₹ $stripped"
    }
    return if (amount < 0) "- $formatted" else formatted
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun BalanceCard(
    title: String,
    balance: Double,
    walletSymbol: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    // Optional: pass card name + owner for credit cards (overrides auto desc)
    cardInfo: String? = null
) {
    // Expense wallet → Deep trust blue card
    // Savings wallet → Rich profit green card
    // Credit card  → Teal card
    val isExpense = title.contains("Expense", ignoreCase = true) && !title.contains("Credit", ignoreCase = true)
    val isCredit = title.contains("Credit", ignoreCase = true)
    val isMainSavings = title.contains("Savings", ignoreCase = true)
    
    // Instead of hardcoding, use the provided gradient colors for the background.
    // Dashboard and Wallets screens already pass parsed custom colors here!
    val cardBgColor = if (gradientColors.isNotEmpty()) gradientColors.first() else when {
        isCredit  -> FinanceTeal
        isExpense -> FinanceBlue
        else      -> FinanceGreen
    }
    
    val contentColor = Color(0xFFFFFFFF)
    val dotColor = contentColor.copy(alpha = 0.6f)
    
    // Tag label: EXPENSE / CREDIT / SAVINGS / TEMP
    val tagLabel = when {
        isCredit  -> "CREDIT"
        isExpense -> "EXPENSE"
        isMainSavings -> "SAVINGS"
        else      -> "TEMP WALLET"
    }
    
    // Bottom descriptor (cardInfo takes priority for credit cards)
    val desc = cardInfo ?: when {
        isCredit  -> "Total card charges"
        isExpense -> "Daily & bills wallet"
        isMainSavings -> "Long-term savings"
        else      -> "Events/Trips"
    }

    Card(
        modifier = modifier
            .height(140.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(dotColor, CircleShape)
                    )
                    Text(
                        text = tagLabel,
                        color = contentColor.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = formatCurrency(balance),
                    color = contentColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }
            Text(
                text = desc,
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    familyMembers: List<FamilyMember>,
    onDeleteClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val member = familyMembers.find { it.id == transaction.familyMemberId }
    val isExpenseWalletAffected = transaction.sourceWallet == WalletType.EXPENSE_WALLET || transaction.destinationWallet == WalletType.EXPENSE_WALLET
    val isSavingsWalletAffected = transaction.sourceWallet == WalletType.SAVINGS_WALLET || transaction.destinationWallet == WalletType.SAVINGS_WALLET

    val (badgeText, isSavings) = when {
        transaction.type == TransactionType.WALLET_TRANSFER -> (if (transaction.sourceWallet == WalletType.EXPENSE_WALLET) "S" else "E") to (transaction.sourceWallet == WalletType.EXPENSE_WALLET)
        isSavingsWalletAffected -> "S" to true
        else -> "E" to false
    }

    // Determine initials/emojis
    val indicatorCharOrEmoji = when (transaction.type) {
        TransactionType.FAMILY_TRANSFER -> member?.name?.firstOrNull()?.toString()?.uppercase() ?: "F"
        TransactionType.PERSONAL_EXPENSE -> {
            val catL = transaction.category.lowercase()
            val noteL = transaction.notes.lowercase()
            when {
                catL.contains("grocer") -> "🛒"
                catL.contains("food") || catL.contains("dining") -> "🍔"
                catL.contains("bill") || catL.contains("utility") || catL.contains("electric") || catL.contains("recharge") -> "⚡"
                catL.contains("travel") || catL.contains("fuel") || catL.contains("transport") -> "🚗"
                catL.contains("medic") || catL.contains("health") || catL.contains("doctor") -> "💊"
                catL.contains("entertain") || catL.contains("movie") -> "🎬"
                catL.contains("shop") -> "🛍️"
                catL.contains("educat") || catL.contains("school") || catL.contains("tuition") || catL.contains("book") -> "📚"
                catL.contains("invest") -> "📈"
                catL.contains("rent") || catL.contains("home") || catL.contains("house") -> "🏠"
                catL.contains("gift") || catL.contains("donation") -> "🎁"
                // Check notes keywords if category is generic/other
                noteL.contains("coffee") || noteL.contains("starbucks") || noteL.contains("cafe") || noteL.contains("tea") -> "☕"
                noteL.contains("food") || noteL.contains("restaurant") || noteL.contains("dinner") || noteL.contains("lunch") -> "🍔"
                noteL.contains("cab") || noteL.contains("fuel") || noteL.contains("petrol") || noteL.contains("auto") || noteL.contains("uber") -> "🚗"
                noteL.contains("medicine") || noteL.contains("doctor") || noteL.contains("hospital") || noteL.contains("health") -> "💊"
                noteL.contains("movie") || noteL.contains("netflix") || noteL.contains("cinema") -> "🎬"
                noteL.contains("flight") || noteL.contains("airport") || noteL.contains("ticket") -> "✈️"
                noteL.contains("rent") || noteL.contains("flat") || noteL.contains("hostel") -> "🏠"
                noteL.contains("salary") -> "💵"
                else -> "🏷️"
            }
        }
        TransactionType.PERSONAL_INCOME -> "💰"
        TransactionType.WALLET_TRANSFER -> "⇄"
        TransactionType.BORROW_LEND -> "🤝"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle/Square avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (transaction.type == TransactionType.FAMILY_TRANSFER) {
                    Text(
                        text = indicatorCharOrEmoji,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                } else {
                    Text(
                        text = indicatorCharOrEmoji,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val titleText = when (transaction.type) {
                    TransactionType.PERSONAL_INCOME -> {
                        if (transaction.notes.isNotEmpty()) {
                            transaction.notes
                        } else {
                            if (transaction.category == "Other") "Salary / Personal Income" else transaction.category
                        }
                    }
                    TransactionType.PERSONAL_EXPENSE -> transaction.notes.ifEmpty { "Personal Expense" }
                    TransactionType.FAMILY_TRANSFER -> {
                        val relationshipHint = member?.relationship?.let { " ($it)" } ?: ""
                        if (transaction.direction == TransactionDirection.RECEIVED_FROM_THEM) {
                            "${member?.name ?: "Brother"} (Received)"
                        } else {
                            "${member?.name ?: "Brother"} (Given)"
                        }
                    }
                    TransactionType.WALLET_TRANSFER -> "Wallet Transfer"
                    TransactionType.BORROW_LEND -> {
                        if (transaction.notes.contains("Settled", ignoreCase = true) || transaction.notes.contains("Settlement", ignoreCase = true)) {
                            if (transaction.direction == TransactionDirection.BORROWED) "Settlement Received" else "Settlement Paid"
                        } else {
                            if (transaction.direction == TransactionDirection.LENT) "Lent Money" else "Borrowed Money"
                        }
                    }
                }

                val subtitleText = when (transaction.type) {
                    TransactionType.PERSONAL_INCOME -> {
                        val walletStr = if (transaction.sourceWallet == WalletType.EXPENSE_WALLET) "Expense Wallet" else "Savings Wallet"
                        if (transaction.notes.isNotEmpty()) {
                            "${transaction.category} • Deposited to $walletStr"
                        } else {
                            "Deposited to $walletStr"
                        }
                    }
                    TransactionType.PERSONAL_EXPENSE -> "Daily expenditure"
                    TransactionType.FAMILY_TRANSFER -> {
                        if (transaction.direction == TransactionDirection.RECEIVED_FROM_THEM) {
                            "Added to ${if (transaction.sourceWallet == WalletType.EXPENSE_WALLET) "Expense" else "Savings"}"
                        } else {
                            "Drawn from ${if (transaction.sourceWallet == WalletType.EXPENSE_WALLET) "Expense" else "Savings"}"
                        }
                    }
                    TransactionType.WALLET_TRANSFER -> {
                        if (transaction.sourceWallet == WalletType.EXPENSE_WALLET) "Expense to Savings" else "Savings to Expense"
                    }
                    TransactionType.BORROW_LEND -> {
                        if (transaction.notes.contains("Settled", ignoreCase = true) || transaction.notes.contains("Settlement", ignoreCase = true)) {
                            if (transaction.direction == TransactionDirection.BORROWED) "Debt Repayment (+)" else "Debt Repayment (-)"
                        } else {
                            if (transaction.direction == TransactionDirection.LENT) "Lent to contact" else "Borrowed from contact"
                        }
                    }
                }

                Text(
                    text = titleText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = subtitleText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )

                val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                Text(
                    text = dateFormatter.format(Date(transaction.date)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount + Wallet tag
            val isPositive = transaction.type == TransactionType.PERSONAL_INCOME ||
                    (transaction.type == TransactionType.FAMILY_TRANSFER && transaction.direction == TransactionDirection.RECEIVED_FROM_THEM)

            // Income → green, Expense → red, Transfer → neutral blue
            val amountColor = when {
                isPositive -> FinanceGreen
                transaction.type == TransactionType.WALLET_TRANSFER -> MaterialTheme.colorScheme.primary
                else -> FinanceRed
            }
            val amountPrefix = if (isPositive) "+" else "-"

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$amountPrefix${formatCurrency(transaction.amount)}",
                    color = amountColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("transaction_amount_${transaction.id}")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Small pill badge
                if (isSavings) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "S",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "E",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            if (onEditClick != null || onDeleteClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onEditClick != null) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("edit_transaction_${transaction.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (onDeleteClick != null) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("delete_transaction_${transaction.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getStartOfCurrentMonthMillis(): Long {
    val calendar = java.util.Calendar.getInstance()
    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}















@Composable
fun SupabaseSyncDialog(
    onDismissRequest: () -> Unit,
    viewModel: LedgerlyViewModel
) {
    val syncInProgress by viewModel.supabaseSyncing.collectAsState()
    val pullInProgress by viewModel.supabasePulling.collectAsState()
    val pushInProgress by viewModel.supabasePushing.collectAsState()
    val syncError by viewModel.supabaseSyncError.collectAsState()
    val syncSuccess by viewModel.supabaseSyncSuccess.collectAsState()

    AlertDialog(
        onDismissRequest = { if (!syncInProgress) onDismissRequest() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Supabase Cloud Sync", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection Status
                val isConfigured = SupabaseClient.isConfigured

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConfigured) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = if (isConfigured) Color(0xFF4CAF50) else Color(0xFFF44336),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (isConfigured) "Connected to Supabase" else "Credentials Missing",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isConfigured) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        val detectedUrl = try { com.parthjivani.ledgerly.BuildConfig.SUPABASE_URL } catch (e: Exception) { "" }
                        val detectedKey = try { com.parthjivani.ledgerly.BuildConfig.SUPABASE_ANON_KEY } catch (e: Exception) { "" }
                        val urlStatus = when {
                            detectedUrl.isEmpty() -> "Not found"
                            detectedUrl.contains("YOUR_SUPABASE_URL") || detectedUrl.contains("YOUR_") -> "Default Placeholder"
                            else -> "Detected and configured"
                        }
                        val keyStatus = when {
                            detectedKey.isEmpty() -> "Not found"
                            detectedKey.contains("YOUR_SUPABASE_ANON_KEY") || detectedKey.contains("YOUR_") -> "Default Placeholder"
                            else -> "Detected and configured (Masked)"
                        }

                        if (!isConfigured) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "To sync your data, configure credentials in either of these locations:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "• AI Studio Cloud: Enter values in the Secrets Panel in AI Studio.\n" +
                                            "• Android Studio (Local): Create a file named '.env' (with a dot prefix) at the project's root folder, add your keys, then click 'Sync Project with Gradle Files' in Android Studio and Rebuild.",
                                    style = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, lineHeight = 15.sp),
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Build Diagnostics:\n" +
                                            "• SUPABASE_URL: $urlStatus\n" +
                                            "• SUPABASE_ANON_KEY: $keyStatus",
                                    style = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                if (syncError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = syncError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (syncSuccess != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = syncSuccess ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (isConfigured) {
                    Text(
                        text = "Perform synchronization operations below to up-sync your local imports or down-sync SQL updates made directly to your Supabase PostgreSQL databases.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "💡 Sync Guidelines & RLS Policies:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "1. Secure Row Level Security (RLS) is fully supported! Make sure you create RLS policies allowing 'anon' or public access for SELECT, INSERT, UPDATE, and DELETE operations.\n" +
                                        "2. If RLS is enabled on your tables without proper policies, queries will return 0 rows.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.pullFromSupabase() },
                            modifier = Modifier.weight(1f).testTag("pull_supabase_btn"),
                            enabled = !syncInProgress,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (pullInProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pull / Download", fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.pushToSupabase() },
                            modifier = Modifier.weight(1f).testTag("push_supabase_btn"),
                            enabled = !syncInProgress,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            if (pushInProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Push / Upload", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Security Warning according to Secret Management Mandates
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Security Warning: Storing API keys directly in configurations carries decompilation extraction risks. Do not build public production APKs without utilizing secure server-side proxy middleware.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.resetSupabaseSyncMessages()
                    onDismissRequest()
                },
                enabled = !syncInProgress,
                modifier = Modifier.testTag("dismiss_sync_dialog")
            ) {
                Text("Close")
            }
        },
        modifier = Modifier.testTag("supabase_sync_dialog")
    )
}