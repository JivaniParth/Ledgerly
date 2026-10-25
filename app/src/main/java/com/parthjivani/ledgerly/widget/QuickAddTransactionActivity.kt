package com.parthjivani.ledgerly.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.parthjivani.ledgerly.data.*
import com.parthjivani.ledgerly.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickAddTransactionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TRANSACTION_TYPE = "EXTRA_TRANSACTION_TYPE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialTypeStr = intent.getStringExtra(EXTRA_TRANSACTION_TYPE) ?: "PERSONAL_EXPENSE"
        val initialType = if (initialTypeStr == "PERSONAL_INCOME") {
            TransactionType.PERSONAL_INCOME
        } else {
            TransactionType.PERSONAL_EXPENSE
        }

        setContent {
            MyApplicationTheme {
                val repository = LedgerlyRepository.getInstance(applicationContext)
                val creditCards by repository.allCreditCards.collectAsState(initial = emptyList())
                val customWallets by repository.allCustomWallets.collectAsState(initial = emptyList())

                var expCats by remember {
                    mutableStateOf(listOf("Food", "Groceries", "Bills", "Travel", "Entertainment", "Shopping", "Medical", "Other"))
                }
                var incCats by remember {
                    mutableStateOf(listOf("Salary", "Business", "Investment", "Freelance", "Gift", "Other"))
                }

                LaunchedEffect(Unit) {
                    val rawExp = repository.getSetting("expense_categories", "")
                    if (rawExp.isNotEmpty()) {
                        val parsed = rawExp.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        if (parsed.isNotEmpty()) expCats = parsed
                    }
                    val rawInc = repository.getSetting("income_categories", "")
                    if (rawInc.isNotEmpty()) {
                        val parsed = rawInc.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        if (parsed.isNotEmpty()) incCats = parsed
                    }
                }

                QuickAddContent(
                    initialType = initialType,
                    expenseCategories = expCats,
                    incomeCategories = incCats,
                    creditCards = creditCards,
                    customWallets = customWallets,
                    onDismiss = { finish() },
                    onSave = { amount, type, category, wallet, notes, cardId, walletId ->
                        saveTransaction(amount, type, category, wallet, notes, cardId, walletId)
                    }
                )
            }
        }
    }

    private fun saveTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        wallet: WalletType,
        notes: String,
        creditCardId: Long? = null,
        customWalletId: Long? = null
    ) {
        val repository = LedgerlyRepository.getInstance(applicationContext)
        val tx = Transaction(
            amount = amount,
            date = System.currentTimeMillis(),
            notes = notes.trim(),
            type = type,
            sourceWallet = wallet,
            category = category,
            creditCardId = creditCardId,
            customWalletId = customWalletId
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val id = repository.insertTransaction(tx)

            // Update Home Screen Widget immediately
            LedgerlyWidgetProvider.sendUpdateBroadcast(applicationContext)

            // Guaranteed offline/online sync
            try {
                val api = SupabaseClient.api
                if (api != null) {
                    api.upsertTransactions(listOf(tx.copy(id = id).toSupabase()))
                } else {
                    SyncWorker.enqueue(applicationContext)
                }
            } catch (e: Exception) {
                // Network error / offline mode: WorkManager guarantees sync when back online
                SyncWorker.enqueue(applicationContext)
            }

            withContext(Dispatchers.Main) {
                val label = if (type == TransactionType.PERSONAL_EXPENSE) "Expense" else "Income"
                Toast.makeText(
                    applicationContext,
                    "✓ Added $label: ₹$amount ($category)",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}

@Composable
fun QuickAddContent(
    initialType: TransactionType,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    creditCards: List<CreditCard>,
    customWallets: List<CustomWallet>,
    onDismiss: () -> Unit,
    onSave: (amount: Double, type: TransactionType, category: String, wallet: WalletType, notes: String, cardId: Long?, customWalletId: Long?) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf(WalletType.EXPENSE_WALLET) }
    var selectedCardId by remember { mutableStateOf<Long?>(null) }
    var selectedCustomWalletId by remember { mutableStateOf<Long?>(null) }

    var selectedCategory by remember(selectedType, expenseCategories, incomeCategories) {
        mutableStateOf(
            if (selectedType == TransactionType.PERSONAL_EXPENSE) {
                expenseCategories.firstOrNull() ?: "Food"
            } else {
                incomeCategories.firstOrNull() ?: "Salary"
            }
        )
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Title and Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Type Toggle: Expense vs Income
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedType = TransactionType.PERSONAL_EXPENSE },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.PERSONAL_EXPENSE) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == TransactionType.PERSONAL_EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("- Expense", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { selectedType = TransactionType.PERSONAL_INCOME },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.PERSONAL_INCOME) Color(0xFF1B6F3E) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedType == TransactionType.PERSONAL_INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Income", fontWeight = FontWeight.Bold)
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                            amountText = input
                        }
                    },
                    label = { Text("Amount") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(14.dp)
                )

                // Category Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Category", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val categories = if (selectedType == TransactionType.PERSONAL_EXPENSE) expenseCategories else incomeCategories
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Wallet Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Wallet", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedWallet == WalletType.EXPENSE_WALLET,
                            onClick = {
                                selectedWallet = WalletType.EXPENSE_WALLET
                                selectedCardId = null
                                selectedCustomWalletId = null
                            },
                            label = { Text("Expense Wallet", fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = selectedWallet == WalletType.SAVINGS_WALLET,
                            onClick = {
                                selectedWallet = WalletType.SAVINGS_WALLET
                                selectedCardId = null
                                selectedCustomWalletId = null
                            },
                            label = { Text("Savings Wallet", fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                        if (selectedType == TransactionType.PERSONAL_EXPENSE) {
                            creditCards.forEach { card ->
                                FilterChip(
                                    selected = selectedWallet == WalletType.CREDIT_CARD && selectedCardId == card.id,
                                    onClick = {
                                        selectedWallet = WalletType.CREDIT_CARD
                                        selectedCardId = card.id
                                        selectedCustomWalletId = null
                                    },
                                    label = { Text("💳 ${card.cardName}", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                        customWallets.forEach { cw ->
                            FilterChip(
                                selected = selectedWallet == WalletType.TEMPORARY_WALLET && selectedCustomWalletId == cw.id,
                                onClick = {
                                    selectedWallet = WalletType.TEMPORARY_WALLET
                                    selectedCustomWalletId = cw.id
                                    selectedCardId = null
                                },
                                label = { Text("👛 ${cw.name}", fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // Notes Field
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull()
                            if (amt != null && amt > 0.0 && amt.isFinite() && !amt.isNaN()) {
                                onSave(
                                    Math.round(amt * 100.0) / 100.0,
                                    selectedType,
                                    selectedCategory,
                                    selectedWallet,
                                    notesText,
                                    selectedCardId,
                                    selectedCustomWalletId
                                )
                            }
                        },
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
