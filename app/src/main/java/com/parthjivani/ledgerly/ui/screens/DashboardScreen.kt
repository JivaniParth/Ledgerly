package com.parthjivani.ledgerly.ui.screens

import androidx.activity.compose.BackHandler
import com.parthjivani.ledgerly.ui.components.AmountInputField
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
import androidx.compose.material.icons.automirrored.outlined.*
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
import com.parthjivani.ledgerly.data.*
import com.parthjivani.ledgerly.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.parthjivani.ledgerly.ui.*
import com.parthjivani.ledgerly.data.*
import com.parthjivani.ledgerly.ui.theme.*

@Composable
fun DashboardScreen(
    expenseBalance: Double,
    savingsBalance: Double,
    transactions: List<Transaction>,
    familyMembers: List<FamilyMember>,
    onAddTransactionClick: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: LedgerlyViewModel
) {
    val budgetGoals by viewModel.allBudgetGoals.collectAsState()
    val savingsGoals by viewModel.allSavingsGoals.collectAsState()
    val creditCards by viewModel.allCreditCards.collectAsState()
    val expenseWalletColorHex by viewModel.expenseWalletColor.collectAsState()
    val currentMillis = System.currentTimeMillis()
    val ninetyDaysAgo = currentMillis - (90L * 24L * 60L * 60L * 1000L)

    // Helper to parse hex colors securely
    fun parseColorSafe(hex: String, defaultStart: Color, defaultEnd: Color): List<Color> {
        if (hex.isBlank()) return listOf(defaultStart, defaultEnd)
        return try {
            val parsed = Color(android.graphics.Color.parseColor(if (!hex.startsWith("#")) "#$hex" else hex))
            listOf(parsed, parsed)
        } catch (e: Exception) {
            listOf(defaultStart, defaultEnd)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                modifier = Modifier.testTag("dashboard_add_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // App Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    AppLogo()
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Dashboard",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.testTag("profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Dual Wallet Balances Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BalanceCard(
                    title = "Expense Wallet",
                    balance = expenseBalance,
                    walletSymbol = "E",
                    gradientColors = parseColorSafe(expenseWalletColorHex, ExpenseGradientStart, ExpenseGradientEnd),
                    modifier = Modifier.weight(1f).testTag("expense_wallet_card")
                )
                if (creditCards.isNotEmpty()) {
                    // Find the credit card with the highest total spend to feature on dashboard
                    val topCard = creditCards.maxByOrNull { card ->
                        transactions
                            .filter { it.sourceWallet == WalletType.CREDIT_CARD && it.creditCardId == card.id }
                            .sumOf { it.amount }
                    }
                    val topCardSpend = if (topCard != null) {
                        transactions
                            .filter { it.sourceWallet == WalletType.CREDIT_CARD && it.creditCardId == topCard.id }
                            .sumOf { it.amount }
                    } else 0.0
                    BalanceCard(
                        title = "Credit Card Expenses",
                        balance = topCardSpend,
                        walletSymbol = "C",
                        gradientColors = parseColorSafe(topCard?.color ?: "#009688", FinanceTeal, FinanceTealDark),
                        // Show card name + owner as subtitle
                        cardInfo = if (topCard != null) "${topCard.cardName} • ${topCard.ownerName}" else "Total card charges",
                        modifier = Modifier.weight(1f).testTag("credit_card_wallet_card")
                    )
                } else {
                    BalanceCard(
                        title = "Savings Wallet",
                        balance = savingsBalance,
                        walletSymbol = "S",
                        gradientColors = listOf(SavingsGradientStart, SavingsGradientEnd),
                        modifier = Modifier.weight(1f).testTag("savings_wallet_card")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Analytics Summary (replaces Recent Transactions) ──────────────────────────────────
            val scrollableContent = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollableContent)
            ) {
                // Monthly Cost Projection card
                val totalSpending90Days = transactions.filter { t ->
                    t.date >= ninetyDaysAgo && (
                        (t.type == TransactionType.PERSONAL_EXPENSE &&
                            (t.sourceWallet == WalletType.EXPENSE_WALLET || t.sourceWallet == WalletType.CREDIT_CARD)) ||
                        (t.type == TransactionType.FAMILY_TRANSFER &&
                            t.sourceWallet == WalletType.EXPENSE_WALLET &&
                            t.direction == TransactionDirection.GIVEN_TO_THEM)
                    )
                }.sumOf { it.amount }

                Text(
                    text = "Monthly Spending Estimate",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_projection_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Cost Projection",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Based on 3 months",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatCurrency(totalSpending90Days / 3.0),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "/ mo",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.65f)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Total outflows in last 90 days: ${formatCurrency(totalSpending90Days)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Budget Goals section
                if (budgetGoals.isNotEmpty()) {
                    Text(
                        text = "Budget & Spending Goals",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        budgetGoals.forEach { goal ->
                            val startOfMonth = getStartOfCurrentMonthMillis()
                            val categoryExpenses = transactions.filter { t ->
                                t.date >= startOfMonth && (
                                    (t.type == TransactionType.PERSONAL_EXPENSE &&
                                        t.sourceWallet != WalletType.TEMPORARY_WALLET &&
                                        (goal.category.equals("Overall", ignoreCase = true) || t.category.equals(goal.category, ignoreCase = true))) ||
                                    (t.type == TransactionType.FAMILY_TRANSFER &&
                                        t.direction == TransactionDirection.GIVEN_TO_THEM &&
                                        t.sourceWallet != WalletType.TEMPORARY_WALLET &&
                                        (goal.category.equals("Overall", ignoreCase = true) || t.category.equals(goal.category, ignoreCase = true)))
                                )
                            }.sumOf { it.amount }

                            val progress = if (goal.limitAmount > 0) (categoryExpenses / goal.limitAmount).toFloat() else 0f
                            val progressColor = when {
                                progress > 0.9f -> FinanceRed
                                progress > 0.7f -> FinanceGold
                                else -> FinanceGreen
                            }

                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .testTag("budget_goal_${goal.category.lowercase()}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = goal.category,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (progress >= 1.0f) {
                                            Box(
                                                modifier = Modifier
                                                    .background(FinanceRedContainer, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("🚨 OVER!", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = FinanceRedOnContainer)
                                            }
                                        } else if (progress >= 0.75f) {
                                            Box(
                                                modifier = Modifier
                                                    .background(FinanceGoldContainer, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("⚠️ NEARING", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = FinanceGoldOnContainer)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Spent: ${formatCurrency(categoryExpenses)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Limit: ${formatCurrency(goal.limitAmount)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { progress.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = progressColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Savings Goals Progress Section
                if (savingsGoals.isNotEmpty()) {
                    Text(
                        text = "Savings Goals Progress",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val sgScrollState = rememberScrollState()
                    var activeSavingsGoal by remember { mutableStateOf<SavingsGoal?>(null) }
                    
                    if (activeSavingsGoal != null) {
                        var amountInput by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { activeSavingsGoal = null },
                            title = { Text("Manage Funds") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Update progress for ${activeSavingsGoal!!.name}. Current saved: ${formatCurrency(activeSavingsGoal!!.currentAmount)}", fontSize = 13.sp)
                                    AmountInputField(
                                        value = amountInput,
                                        onValueChange = { amountInput = it },
                                        label = { Text("Amount") }
                                    )
                                }
                            },
                            confirmButton = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                                            if (amt > 0) {
                                                viewModel.updateSavingsGoal(
                                                    activeSavingsGoal!!.copy(currentAmount = activeSavingsGoal!!.currentAmount + amt)
                                                )
                                                activeSavingsGoal = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = FinanceGreen)
                                    ) {
                                        Text("Add")
                                    }
                                    Button(
                                        onClick = {
                                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                                            if (amt > 0) {
                                                viewModel.updateSavingsGoal(
                                                    activeSavingsGoal!!.copy(currentAmount = maxOf(0.0, activeSavingsGoal!!.currentAmount - amt))
                                                )
                                                activeSavingsGoal = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = FinanceRed)
                                    ) {
                                        Text("Withdraw")
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { activeSavingsGoal = null }) { Text("Cancel") }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(sgScrollState)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        savingsGoals.forEach { sg ->
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable { activeSavingsGoal = sg },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(sg.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(sg.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val progress = if (sg.targetAmount > 0) (sg.currentAmount / sg.targetAmount).toFloat() else 0f
                                    LinearProgressIndicator(
                                        progress = { progress.coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(formatCurrency(sg.currentAmount), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(formatCurrency(sg.targetAmount), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Category spend breakdown (last 30 days)
                val startOfMonth = getStartOfCurrentMonthMillis()
                val categoryBreakdown = transactions
                    .filter { it.date >= startOfMonth && it.type == TransactionType.PERSONAL_EXPENSE && it.sourceWallet != WalletType.TEMPORARY_WALLET }
                    .groupBy { it.category }
                    .mapValues { (_, txs) -> txs.sumOf { it.amount } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5)

                if (categoryBreakdown.isNotEmpty()) {
                    Text(
                        text = "This Month's Top Spending",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("dashboard_category_breakdown"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val maxAmount = categoryBreakdown.firstOrNull()?.second ?: 1.0
                            categoryBreakdown.forEach { (category, amount) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(100.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth((amount / maxAmount).toFloat())
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatCurrency(amount),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transactions yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap + to log your first record.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Bottom padding for FAB
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

}
