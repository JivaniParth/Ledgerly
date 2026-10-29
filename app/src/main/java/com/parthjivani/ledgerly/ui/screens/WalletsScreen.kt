package com.parthjivani.ledgerly.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parthjivani.ledgerly.data.*
import com.parthjivani.ledgerly.ui.theme.*
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

data class LedgerEntity(
    val id: Long,
    val name: String,
    val isFamilyMember: Boolean,
    val balance: Double,
    val contactObj: Contact? = null,
    val memberObj: FamilyMember? = null
)

@Composable
fun WalletsScreen(
    viewModel: LedgerlyViewModel, 
    onNavigateToTransactions: () -> Unit = {},
    onScrollStateChange: (Boolean) -> Unit = {}
) {
    var selectedWalletTab by remember { mutableStateOf(0) }
    
    val expenseBalance by viewModel.expenseWalletBalance.collectAsState()
    val savingsBalance by viewModel.savingsWalletBalance.collectAsState()
    val creditCards by viewModel.allCreditCards.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val customWallets by viewModel.allCustomWallets.collectAsState()
    val expenseWalletColorHex by viewModel.expenseWalletColor.collectAsState()
    
    val familyMembers by viewModel.allFamilyMembers.collectAsState()
    val contacts by viewModel.allContacts.collectAsState()
    val splits by viewModel.allTransactionSplits.collectAsState()
    
    var editingCard by remember { mutableStateOf<CreditCard?>(null) }
    var editingCustomWallet by remember { mutableStateOf<CustomWallet?>(null) }
    var showCustomWalletDialog by remember { mutableStateOf(false) }
    var showColorPickerFor by remember { mutableStateOf<String?>(null) }
    
    var showAddContactDialog by remember { mutableStateOf(false) }
    var newContactName by remember { mutableStateOf("") }
    
    var showSettleUpConfirm by remember { mutableStateOf<LedgerEntity?>(null) }
    var selectedEntityForDetails by remember { mutableStateOf<LedgerEntity?>(null) }
    var editingEntityName by remember { mutableStateOf<String?>(null) }

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
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                AppLogo()
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Wallets & Ledger",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
            }
        }
        
        TabRow(
            selectedTabIndex = selectedWalletTab,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            containerColor = Color.Transparent
        ) {
            Tab(
                selected = selectedWalletTab == 0,
                onClick = { selectedWalletTab = 0 },
                text = { Text("Wallets", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedWalletTab == 1,
                onClick = { selectedWalletTab = 1 },
                text = { Text("Contacts Ledger", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        val listState = rememberLazyListState()
        val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }
        LaunchedEffect(isScrolled) { onScrollStateChange(isScrolled) }

        if (selectedWalletTab == 0) {
            // Wallet Cards List in a LazyColumn
            LazyColumn(
            state = listState,
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
        } else {
            // Contacts Ledger View
            val ledgerEntities = remember(familyMembers, contacts, splits, transactions) {
                val entities = mutableListOf<LedgerEntity>()
                
                // Process Contacts
                contacts.forEach { contact ->
                    val splitsOwed = splits.filter { it.contactId == contact.id && !it.isSettled }.sumOf { it.amount }
                    val lent = transactions.filter { it.contactId == contact.id && it.type == TransactionType.BORROW_LEND && it.direction == TransactionDirection.LENT }.sumOf { it.amount }
                    val borrowed = transactions.filter { it.contactId == contact.id && it.type == TransactionType.BORROW_LEND && it.direction == TransactionDirection.BORROWED }.sumOf { it.amount }
                    val balance = splitsOwed + lent - borrowed
                    
                    entities.add(LedgerEntity(id = contact.id, name = contact.name, isFamilyMember = false, balance = balance, contactObj = contact))
                }
                
                // Process Family Members
                familyMembers.forEach { member ->
                    val splitsOwed = splits.filter { it.familyMemberId == member.id && !it.isSettled }.sumOf { it.amount }
                    val lent = transactions.filter { it.familyMemberId == member.id && it.type == TransactionType.BORROW_LEND && it.direction == TransactionDirection.LENT }.sumOf { it.amount }
                    val borrowed = transactions.filter { it.familyMemberId == member.id && it.type == TransactionType.BORROW_LEND && it.direction == TransactionDirection.BORROWED }.sumOf { it.amount }
                    val balance = splitsOwed + lent - borrowed
                    
                    // Show family members only if they have a ledger balance, to keep it clean.
                    if (balance != 0.0) {
                        entities.add(LedgerEntity(id = member.id, name = "${member.name} (Family)", isFamilyMember = true, balance = balance, memberObj = member))
                    }
                }
                
                entities.sortedByDescending { Math.abs(it.balance) }
            }
            
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val oweYou = ledgerEntities.filter { it.balance > 0 }
                    val youOwe = ledgerEntities.filter { it.balance < 0 }
                    val settled = ledgerEntities.filter { it.balance == 0.0 }
                    
                    if (oweYou.isNotEmpty()) {
                        item {
                            Text("Owes You", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ExpenseColor)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(oweYou) { entity ->
                            LedgerEntityCard(entity, onClick = { selectedEntityForDetails = entity }, onSettleUp = { showSettleUpConfirm = it })
                        }
                    }
                    
                    if (youOwe.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("You Owe", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(youOwe) { entity ->
                            LedgerEntityCard(entity, onClick = { selectedEntityForDetails = entity }, onSettleUp = { showSettleUpConfirm = it })
                        }
                    }
                    
                    if (settled.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Settled Up", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(settled) { entity ->
                            LedgerEntityCard(entity, onClick = { selectedEntityForDetails = entity }, onSettleUp = { showSettleUpConfirm = it })
                        }
                    }
                    
                    if (ledgerEntities.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No contacts in your ledger yet.", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Add a contact to start splitting expenses or borrowing/lending.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
                
                FloatingActionButton(
                    onClick = { showAddContactDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
                }
            }
            
            if (showAddContactDialog) {
                AlertDialog(
                    onDismissRequest = { showAddContactDialog = false },
                    title = { Text("Add Contact") },
                    text = {
                        OutlinedTextField(
                            value = newContactName,
                            onValueChange = { newContactName = it },
                            label = { Text("Contact Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newContactName.isNotBlank()) {
                                viewModel.addContact(newContactName.trim())
                                newContactName = ""
                                showAddContactDialog = false
                            }
                        }) { Text("Add") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddContactDialog = false }) { Text("Cancel") }
                    }
                )
            }
            
            if (showSettleUpConfirm != null) {
                val entity = showSettleUpConfirm!!
                AlertDialog(
                    onDismissRequest = { showSettleUpConfirm = null },
                    title = { Text("Settle Up with ${entity.name}?") },
                    text = {
                        if (entity.balance > 0) {
                            Text("Record a payment of ${formatCurrency(entity.balance)} received from ${entity.name}. This will settle all splits and borrow/lend transactions and update your Expense Wallet balance.")
                        } else if (entity.balance < 0) {
                            Text("Record a payment of ${formatCurrency(Math.abs(entity.balance))} made to ${entity.name}. This will settle all splits and borrow/lend transactions and reduce your Expense Wallet balance.")
                        } else {
                            Text("This contact is already settled up.")
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (entity.isFamilyMember && entity.memberObj != null) {
                                viewModel.settleUpFamilyMember(entity.memberObj.id, WalletType.EXPENSE_WALLET)
                            } else if (!entity.isFamilyMember && entity.contactObj != null) {
                                viewModel.settleUpContact(entity.contactObj.id, WalletType.EXPENSE_WALLET)
                            }
                            showSettleUpConfirm = null
                        }, enabled = entity.balance != 0.0) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSettleUpConfirm = null }) { Text("Cancel") }
                    }
                )
            }

            if (selectedEntityForDetails != null) {
                val entity = selectedEntityForDetails!!
                
                if (editingEntityName != null) {
                    AlertDialog(
                        onDismissRequest = { editingEntityName = null },
                        title = { Text("Edit Name") },
                        text = {
                            OutlinedTextField(
                                value = editingEntityName!!,
                                onValueChange = { editingEntityName = it },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                val trimmed = editingEntityName!!.trim()
                                if (trimmed.isNotEmpty()) {
                                    if (entity.isFamilyMember && entity.memberObj != null) {
                                        viewModel.updateFamilyMember(entity.memberObj.copy(name = trimmed))
                                    } else if (!entity.isFamilyMember && entity.contactObj != null) {
                                        viewModel.updateContact(entity.contactObj.copy(name = trimmed))
                                    }
                                }
                                editingEntityName = null
                                selectedEntityForDetails = null
                            }) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { editingEntityName = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                } else {
                    AlertDialog(
                        onDismissRequest = { selectedEntityForDetails = null },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(if (entity.isFamilyMember) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(entity.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = if (entity.isFamilyMember) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(entity.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    val statusText = when {
                                        entity.balance > 0 -> "Owes you ${formatCurrency(entity.balance)}"
                                        entity.balance < 0 -> "You owe ${formatCurrency(Math.abs(entity.balance))}"
                                        else -> "Settled up"
                                    }
                                    val statusColor = when {
                                        entity.balance > 0 -> ExpenseColor
                                        entity.balance < 0 -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Text(statusText, fontSize = 14.sp, color = statusColor)
                                }
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        viewModel.clearTxFilters()
                                        if (entity.isFamilyMember) {
                                            viewModel.setTxMemberFilter(entity.id)
                                        } else {
                                            viewModel.setTxContactFilter(entity.id)
                                        }
                                        selectedEntityForDetails = null
                                        onNavigateToTransactions()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View History")
                                }
                                
                                OutlinedButton(
                                    onClick = { editingEntityName = entity.name },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Edit Name")
                                }
                                
                                if (entity.balance != 0.0) {
                                    OutlinedButton(
                                        onClick = { 
                                            showSettleUpConfirm = entity 
                                            selectedEntityForDetails = null
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.AttachMoney, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Settle Up")
                                    }
                                }
                                
                                TextButton(
                                    onClick = {
                                        if (entity.isFamilyMember && entity.memberObj != null) {
                                            viewModel.deleteFamilyMember(entity.memberObj)
                                        } else if (!entity.isFamilyMember && entity.contactObj != null) {
                                            viewModel.deleteContact(entity.contactObj)
                                        }
                                        selectedEntityForDetails = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete Contact")
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { selectedEntityForDetails = null }) {
                                Text("Close")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerEntityCard(entity: LedgerEntity, onClick: () -> Unit, onSettleUp: (LedgerEntity) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (entity.isFamilyMember) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(entity.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = if (entity.isFamilyMember) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(entity.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val statusText = when {
                        entity.balance > 0 -> "Owes you ${formatCurrency(entity.balance)}"
                        entity.balance < 0 -> "You owe ${formatCurrency(Math.abs(entity.balance))}"
                        else -> "Settled up"
                    }
                    val statusColor = when {
                        entity.balance > 0 -> ExpenseColor
                        entity.balance < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(statusText, fontSize = 13.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                }
            }
            
            if (entity.balance != 0.0) {
                Button(
                    onClick = { onSettleUp(entity) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Settle", fontSize = 12.sp)
                }
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
