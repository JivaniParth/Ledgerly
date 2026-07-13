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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.parthjivani.ledgerly.ui.theme.*

fun parseColorSafe(hex: String, defaultStart: Color, defaultEnd: Color): List<Color> {
    if (hex.isBlank()) return listOf(defaultStart, defaultEnd)
    return try {
        val parsed = Color(android.graphics.Color.parseColor(if (!hex.startsWith("#")) "#$hex" else hex))
        listOf(parsed, parsed)
    } catch (e: Exception) {
        listOf(defaultStart, defaultEnd)
    }
}

@Composable
fun WalletsScreen(viewModel: LedgerlyViewModel, onNavigateToTransactions: () -> Unit = {}) {
    val expenseBalance by viewModel.expenseWalletBalance.collectAsState()
    val savingsBalance by viewModel.savingsWalletBalance.collectAsState()
    val creditCards by viewModel.allCreditCards.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val customWallets by viewModel.allCustomWallets.collectAsState()
    val expenseWalletColorHex by viewModel.expenseWalletColor.collectAsState()
    
    var editingCard by remember { mutableStateOf<CreditCard?>(null) }
    var editingCustomWallet by remember { mutableStateOf<CustomWallet?>(null) }
    var showCustomWalletDialog by remember { mutableStateOf(false) }
    var showColorPickerFor by remember { mutableStateOf<String?>(null) }

    if (editingCard != null) {
        var cardName by remember { mutableStateOf(editingCard!!.cardName) }
        var ownerName by remember { mutableStateOf(editingCard!!.ownerName) }
        var limit by remember { mutableStateOf(editingCard!!.creditLimit.toString()) }
        var cColor by remember { mutableStateOf<String>(editingCard!!.color) }
        var showCCColorPicker by remember { mutableStateOf(false) }

        if (showCCColorPicker) {
            ColorPickerDialog(
                initialColor = cColor,
                onColorSelected = { cColor = it; showCCColorPicker = false },
                onDismiss = { showCCColorPicker = false }
            )
        } else {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable { showCCColorPicker = true }.padding(vertical = 8.dp)
                        ) {
                            Text("Card Color:", fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(parseColorSafe(cColor, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.onSurfaceVariant).first(), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val l = limit.toDoubleOrNull() ?: 0.0
                        if (cardName.isNotBlank() && ownerName.isNotBlank()) {
                            viewModel.updateCreditCard(editingCard!!.copy(cardName = cardName, ownerName = ownerName, creditLimit = l, color = cColor))
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
    }

    if (showCustomWalletDialog || editingCustomWallet != null) {
        var walletName by remember { mutableStateOf(editingCustomWallet?.name ?: "") }
        var walletColor by remember { mutableStateOf<String>(editingCustomWallet?.color ?: FinanceTeal.toArgb().let { "#${Integer.toHexString(it).substring(2)}" }) }
        var showColorPicker by remember { mutableStateOf(false) }

        if (showColorPicker) {
            ColorPickerDialog(
                initialColor = walletColor,
                onColorSelected = { walletColor = it; showColorPicker = false },
                onDismiss = { showColorPicker = false }
            )
        } else {
            AlertDialog(
                onDismissRequest = {
                    showCustomWalletDialog = false
                    editingCustomWallet = null
                },
                title = { Text(if (editingCustomWallet != null) "Edit Temporary Wallet" else "Add Temporary Wallet") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = walletName,
                            onValueChange = { walletName = it },
                            label = { Text("Wallet Name") },
                            singleLine = true
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable { showColorPicker = true }.padding(vertical = 8.dp)
                        ) {
                            Text("Color:", fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(parseColorSafe(walletColor, FinanceTeal, FinanceTealDark).first(), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (walletName.isNotBlank()) {
                            if (editingCustomWallet != null) {
                                viewModel.updateCustomWallet(editingCustomWallet!!.copy(name = walletName, color = walletColor))
                            } else {
                                viewModel.addCustomWallet(walletName, walletColor)
                            }
                            showCustomWalletDialog = false
                            editingCustomWallet = null
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (editingCustomWallet != null) {
                            TextButton(onClick = {
                                viewModel.deleteCustomWallet(editingCustomWallet!!)
                                showCustomWalletDialog = false
                                editingCustomWallet = null
                            }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Text("Delete")
                            }
                        }
                        TextButton(onClick = {
                            showCustomWalletDialog = false
                            editingCustomWallet = null
                        }) { Text("Cancel") }
                    }
                }
            )
        }
    }

    if (showColorPickerFor != null) {
        val initialColor = if (showColorPickerFor == "Expense") expenseWalletColorHex else ""
        ColorPickerDialog(
            initialColor = initialColor,
            onColorSelected = { hex ->
                if (showColorPickerFor == "Expense") {
                    viewModel.setExpenseWalletColor(hex)
                }
                showColorPickerFor = null
            },
            onDismiss = { showColorPickerFor = null }
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

                    val cardColors = parseColorSafe(card.color, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    // Custom Credit Card Visual Design
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clickable { editingCard = card },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColors.first()),
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
                                        color = if (cardColors.first().toArgb() == android.graphics.Color.WHITE) Color.Black else Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "CARD OWNER: ${card.ownerName.uppercase()}",
                                        color = if (cardColors.first().toArgb() == android.graphics.Color.WHITE) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
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
                                    color = if (cardColors.first().toArgb() == android.graphics.Color.WHITE) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = formatCurrency(spentOnCard),
                                    color = if (cardColors.first().toArgb() == android.graphics.Color.WHITE) Color.Black else Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } // ends else block

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Temporary Wallets", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { showCustomWalletDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Add Temporary Wallet", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (customWallets.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No temporary wallets.", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Use temporary wallets for events, trips, or temporary funds.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        customWallets.forEach { cw ->
                            val cwBalance = viewModel.calculateTempWalletBalance(cw.id)
                            Box {
                                BalanceCard(
                                    title = cw.name,
                                    balance = cwBalance,
                                    walletSymbol = cw.name.take(1).uppercase(),
                                    gradientColors = parseColorSafe(cw.color, FinanceTeal, FinanceTealDark),
                                    modifier = Modifier.clickable {
                                        viewModel.clearTxFilters()
                                        viewModel.setTxCustomWalletFilter(cw.id)
                                        onNavigateToTransactions()
                                    }
                                )
                                IconButton(
                                    onClick = { editingCustomWallet = cw },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Wallet", tint = Color.White)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val predefinedColors = listOf(
        "#E91E63", "#F44336", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
        "#795548", "#9E9E9E", "#607D8B", "#000000"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(predefinedColors.chunked(4)) { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowColors.forEach { colorHex ->
                            val color = parseColorSafe(colorHex, Color.Gray, Color.Gray).first()
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = if (colorHex == initialColor) 3.dp else 1.dp,
                                        color = if (colorHex == initialColor) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { onColorSelected(colorHex) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
