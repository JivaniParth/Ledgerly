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

@Composable
fun WalletsScreen(viewModel: HisabViewModel) {
    val expenseBalance by viewModel.expenseWalletBalance.collectAsState()
    val savingsBalance by viewModel.savingsWalletBalance.collectAsState()
    val creditCards by viewModel.allCreditCards.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    var editingCard by remember { mutableStateOf<CreditCard?>(null) }

    if (editingCard != null) {
        var cardName by remember { mutableStateOf(editingCard!!.cardName) }
        var ownerName by remember { mutableStateOf(editingCard!!.ownerName) }
        var limit by remember { mutableStateOf(editingCard!!.creditLimit.toString()) }

        AlertDialog(
            onDismissRequest = { editingCard = null },
            title = { Text("Edit Credit Card") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cardName, onValueChange = { cardName = it }, label = { Text("Card Name") }, singleLine = true)
                    OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, singleLine = true)
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text("Credit Limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val l = limit.toDoubleOrNull() ?: 0.0
                    if (cardName.isNotBlank() && ownerName.isNotBlank()) {
                        viewModel.updateCreditCard(editingCard!!.copy(cardName = cardName, ownerName = ownerName, creditLimit = l))
                        editingCard = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        viewModel.deleteCreditCard(editingCard!!)
                        editingCard = null
                    }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete")
                    }
                    TextButton(onClick = { editingCard = null }) { Text("Cancel") }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("wallets_screen")
            .padding(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                AppLogo()
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "My Wallets & Accounts",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Wallet Cards List in a LazyColumn
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Operational Wallets", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BalanceCard(
                        title = "Expense Wallet",
                        balance = expenseBalance,
                        walletSymbol = "E",
                        gradientColors = listOf(ExpenseGradientStart, ExpenseGradientEnd),
                        modifier = Modifier.testTag("wallets_screen_expense_wallet_card")
                    )
                    BalanceCard(
                        title = "Savings Wallet",
                        balance = savingsBalance,
                        walletSymbol = "S",
                        gradientColors = listOf(SavingsGradientStart, SavingsGradientEnd),
                        modifier = Modifier.testTag("wallets_screen_savings_wallet_card")
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Credit Cards Ledger", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "${creditCards.size} Cards",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (creditCards.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = "No cards", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No credit cards managed yet.", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Add credit cards in the Family Hub screen to track card expenses separately.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                items(creditCards) { card ->
                    // Calculate expenses done using credit card
                    val spentOnCard = transactions
                        .filter { it.sourceWallet == WalletType.CREDIT_CARD && it.creditCardId == card.id }
                        .sumOf { it.amount }

                    // Custom Credit Card Visual Design
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clickable { editingCard = card },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Chip and Name design
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = card.cardName.uppercase(),
                                        color = MaterialTheme.colorScheme.surface,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "CARD OWNER: ${card.ownerName.uppercase()}",
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp, 24.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                )
                            }

                            // Balance info and Limit
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "TOTAL CARD EXPENSES",
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = formatCurrency(spentOnCard),
                                    color = MaterialTheme.colorScheme.surface,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
