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
fun TransactionsScreen(
    viewModel: HisabViewModel,
    onAddTransactionClick: () -> Unit,
    onEditTransactionClick: (Transaction) -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val familyMembers by viewModel.allFamilyMembers.collectAsState()
    val creditCards by viewModel.allCreditCards.collectAsState()

    // Filter states — stored in ViewModel so they survive navigation to/from the edit screen
    val walletFilter by viewModel.txWalletFilter.collectAsState()
    val cardFilter by viewModel.txCardFilter.collectAsState()
    val memberFilter by viewModel.txMemberFilter.collectAsState()
    val categoryFilter by viewModel.txCategoryFilter.collectAsState()
    val typeFilter by viewModel.txTypeFilter.collectAsState()
    val searchQuery by viewModel.txSearchQuery.collectAsState()

    val lastSavedId by viewModel.lastSavedTransactionId.collectAsState()
    val listState = rememberLazyListState()
    var highlightedId by remember { mutableStateOf<Long?>(null) }

    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var selectedTransactionForOptions by remember { mutableStateOf<Transaction?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var showSupabaseSyncDialog by remember { mutableStateOf(false) }
    val selectedTransactionIds = remember { androidx.compose.runtime.mutableStateListOf<Long>() }
    var showMultipleDeleteConfirmation by remember { mutableStateOf(false) }

    // Dropdown expand states
    var walletMenuExpanded by remember { mutableStateOf(false) }
    var cardMenuExpanded by remember { mutableStateOf(false) }
    var memberMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    val categoriesList = listOf("Groceries", "Entertainment", "Bills", "Travel", "Medical", "Other")

    // Filter and Search logic
    val filteredTransactions = transactions.filter { t ->
        val matchesWallet = walletFilter == null || t.sourceWallet == walletFilter || t.destinationWallet == walletFilter
        val matchesCard = cardFilter == null || t.creditCardId == cardFilter
        val matchesMember = memberFilter == null || t.familyMemberId == memberFilter
        val matchesCategory = categoryFilter == null || t.category.equals(categoryFilter, ignoreCase = true)
        val matchesType = typeFilter == null || t.type == typeFilter

        val matchesSearch = if (searchQuery.isBlank()) {
            true
        } else {
            val q = searchQuery.trim().lowercase()
            val notesMatch = t.notes.lowercase().contains(q)
            val categoryMatch = t.category.lowercase().contains(q)
            val amountMatch = t.amount.toString().contains(q) ||
                    (t.amount % 1.0 == 0.0 && t.amount.toInt().toString().contains(q))
            val typeMatch = t.type.name.lowercase().replace("_", " ").contains(q)
            notesMatch || categoryMatch || amountMatch || typeMatch
        }
        matchesWallet && matchesCard && matchesMember && matchesCategory && matchesType && matchesSearch
    }

    LaunchedEffect(lastSavedId) {
        val targetId = lastSavedId ?: return@LaunchedEffect

        // First try to scroll within the current (filtered) view
        val filteredIndex = filteredTransactions.indexOfFirst { it.id == targetId }
        if (filteredIndex != -1) {
            // Found in current filtered list — scroll to it and highlight
            highlightedId = targetId
            try { listState.animateScrollToItem(filteredIndex) } catch (_: Exception) {}
            viewModel.setLastSavedTransactionId(null)
            kotlinx.coroutines.delay(2500)
            highlightedId = null
        } else {
            // Not visible with current filters — scroll to it in the full list
            // without clearing the user's filters (they are preserved in ViewModel)
            val allIndex = transactions.indexOfFirst { it.id == targetId }
            if (allIndex != -1) {
                // Clear filters just enough to reveal the record, then scroll
                viewModel.clearTxFilters()
                // Wait one frame for filteredTransactions to recompute
                kotlinx.coroutines.delay(100)
                val newIndex = transactions.indexOfFirst { it.id == targetId }
                if (newIndex != -1) {
                    highlightedId = targetId
                    try { listState.animateScrollToItem(newIndex) } catch (_: Exception) {}
                }
            }
            viewModel.setLastSavedTransactionId(null)
            kotlinx.coroutines.delay(2500)
            highlightedId = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("transactions_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                modifier = Modifier.testTag("transactions_add_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Entry")
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
                if (isSelectionMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedTransactionIds.clear()
                            },
                            modifier = Modifier.testTag("cancel_selection")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                        Text(
                            text = "${selectedTransactionIds.size} Selected",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val allFilteredIds = filteredTransactions.map { it.id }
                        val isAllSelected = allFilteredIds.isNotEmpty() && selectedTransactionIds.containsAll(allFilteredIds)

                        TextButton(
                            onClick = {
                                if (isAllSelected) {
                                    selectedTransactionIds.clear()
                                } else {
                                    selectedTransactionIds.clear()
                                    selectedTransactionIds.addAll(allFilteredIds)
                                }
                            },
                            modifier = Modifier.testTag("select_all_button")
                        ) {
                            Text(if (isAllSelected) "Deselect All" else "Select All", fontSize = 13.sp)
                        }

                        IconButton(
                            onClick = {
                                if (selectedTransactionIds.isNotEmpty()) {
                                    showMultipleDeleteConfirmation = true
                                }
                            },
                            enabled = selectedTransactionIds.isNotEmpty(),
                            modifier = Modifier.testTag("delete_selected")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = if (selectedTransactionIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                } else {
                    Column {
                        AppLogo()
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "All Transactions",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Supabase cloud status indicator button
                        IconButton(
                            onClick = { showSupabaseSyncDialog = true },
                            modifier = Modifier.testTag("supabase_sync_settings_btn")
                        ) {
                            val iconColor = if (SupabaseClient.isConfigured) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                            Icon(
                                imageVector = if (SupabaseClient.isConfigured) Icons.Default.CloudSync else Icons.Default.CloudOff,
                                contentDescription = "Supabase Sync",
                                tint = iconColor
                            )
                        }

                        IconButton(
                            onClick = {
                                isSelectionMode = true
                                selectedTransactionIds.clear()
                            },
                            modifier = Modifier.testTag("enable_multi_select")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = "Enable Multi-Select",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Search Bar Text Field (Search by notes, category, amount, type of record)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setTxSearchQuery(it) },
                placeholder = { Text("Search by notes, category, amount, type...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setTxSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("transaction_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                )
            )

            // Filters Scroll Row
            val lazyRowScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(lazyRowScrollState)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Wallet Filter Dropdown
                Box {
                    FilterChip(
                        selected = walletFilter != null,
                        onClick = { walletMenuExpanded = true },
                        label = { Text(walletFilter?.name?.replace("_WALLET", "") ?: "Wallet") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                    )
                    DropdownMenu(expanded = walletMenuExpanded, onDismissRequest = { walletMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("All Wallets") }, onClick = { viewModel.setTxWalletFilter(null); walletMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Expense") }, onClick = { viewModel.setTxWalletFilter(WalletType.EXPENSE_WALLET); walletMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Savings") }, onClick = { viewModel.setTxWalletFilter(WalletType.SAVINGS_WALLET); walletMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Credit Card") }, onClick = { viewModel.setTxWalletFilter(WalletType.CREDIT_CARD); walletMenuExpanded = false })
                    }
                }

                // Credit Card Filter Dropdown
                if (creditCards.isNotEmpty()) {
                    Box {
                        FilterChip(
                            selected = cardFilter != null,
                            onClick = { cardMenuExpanded = true },
                            label = {
                                val currentCard = creditCards.find { it.id == cardFilter }
                                Text(currentCard?.cardName ?: "Card")
                            },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                        )
                        DropdownMenu(expanded = cardMenuExpanded, onDismissRequest = { cardMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("All Cards") }, onClick = { viewModel.setTxCardFilter(null); cardMenuExpanded = false })
                            creditCards.forEach { card ->
                                DropdownMenuItem(text = { Text(card.cardName) }, onClick = { viewModel.setTxCardFilter(card.id); cardMenuExpanded = false })
                            }
                        }
                    }
                }

                // Family Member Filter Dropdown
                Box {
                    FilterChip(
                        selected = memberFilter != null,
                        onClick = { memberMenuExpanded = true },
                        label = {
                            val currentMember = familyMembers.find { it.id == memberFilter }
                            Text(currentMember?.name ?: "Member")
                        },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                    )
                    DropdownMenu(expanded = memberMenuExpanded, onDismissRequest = { memberMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("All Members") }, onClick = { viewModel.setTxMemberFilter(null); memberMenuExpanded = false })
                        familyMembers.forEach { member ->
                            DropdownMenuItem(text = { Text(member.name) }, onClick = { viewModel.setTxMemberFilter(member.id); memberMenuExpanded = false })
                        }
                    }
                }

                // Category Filter Dropdown
                Box {
                    FilterChip(
                        selected = categoryFilter != null,
                        onClick = { categoryMenuExpanded = true },
                        label = { Text(categoryFilter ?: "Category") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                    )
                    DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("All Categories") }, onClick = { viewModel.setTxCategoryFilter(null); categoryMenuExpanded = false })
                        categoriesList.forEach { category ->
                            DropdownMenuItem(text = { Text(category) }, onClick = { viewModel.setTxCategoryFilter(category); categoryMenuExpanded = false })
                        }
                    }
                }

                // Transaction Type Filter Dropdown
                Box {
                    FilterChip(
                        selected = typeFilter != null,
                        onClick = { typeMenuExpanded = true },
                        label = { Text(typeFilter?.name?.replace("PERSONAL_", "")?.replace("FAMILY_", "") ?: "Type") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                    )
                    DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("All Types") }, onClick = { viewModel.setTxTypeFilter(null); typeMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Personal Income") }, onClick = { viewModel.setTxTypeFilter(TransactionType.PERSONAL_INCOME); typeMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Personal Expense") }, onClick = { viewModel.setTxTypeFilter(TransactionType.PERSONAL_EXPENSE); typeMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Family Transfer") }, onClick = { viewModel.setTxTypeFilter(TransactionType.FAMILY_TRANSFER); typeMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Wallet Transfer") }, onClick = { viewModel.setTxTypeFilter(TransactionType.WALLET_TRANSFER); typeMenuExpanded = false })
                    }
                }
            }

            // Clear filter chip if any active
            if (walletFilter != null || cardFilter != null || memberFilter != null || categoryFilter != null || typeFilter != null || searchQuery.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.clearTxFilters() }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear Filters")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All Filters", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Display results
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = "No transactions matches filters",
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching transactions found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Adjust your filter settings above.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { t ->
                        val isHighlighted = t.id == highlightedId
                        val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        val backgroundColor by animateColorAsState(
                            targetValue = if (isHighlighted) highlightColor else Color.Transparent,
                            animationSpec = tween(durationMillis = 600),
                            label = "RowHighlightColor"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor, shape = RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isHighlighted) 1.5.dp else 0.dp,
                                    color = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isSelectionMode) {
                                        if (selectedTransactionIds.contains(t.id)) {
                                            selectedTransactionIds.remove(t.id)
                                        } else {
                                            selectedTransactionIds.add(t.id)
                                        }
                                    } else {
                                        selectedTransactionForOptions = t
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("transaction_row_selectable_${t.id}")
                        ) {
                            if (isSelectionMode) {
                                Checkbox(
                                    checked = selectedTransactionIds.contains(t.id),
                                    onCheckedChange = { checked ->
                                        if (checked == true) {
                                            if (!selectedTransactionIds.contains(t.id)) {
                                                selectedTransactionIds.add(t.id)
                                            }
                                        } else {
                                            selectedTransactionIds.remove(t.id)
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .testTag("transaction_checkbox_${t.id}")
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TransactionRow(
                                    transaction = t,
                                    familyMembers = familyMembers,
                                    onClick = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedTransactionForOptions != null) {
        val t = selectedTransactionForOptions!!
        AlertDialog(
            onDismissRequest = { selectedTransactionForOptions = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Transaction Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Would you like to edit or delete this transaction?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Amount:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatCurrency(t.amount),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (t.type == TransactionType.PERSONAL_INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Category:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = t.category, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (t.notes.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Note:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = t.notes, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            transactionToDelete = t
                            selectedTransactionForOptions = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onEditTransactionClick(t)
                            selectedTransactionForOptions = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedTransactionForOptions = null }
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("transaction_options_dialog")
        )
    }

    if (transactionToDelete != null) {
        val t = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this recorded transaction of ${formatCurrency(t.amount)}?\nThis affects local wallet and budget limits.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(t)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("delete_dialog")
        )
    }

    if (showMultipleDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showMultipleDeleteConfirmation = false },
            title = { Text("Delete Multiple Transactions") },
            text = { Text("Are you sure you want to delete the ${selectedTransactionIds.size} selected transactions?\nThis will permanently update local balances and budget limit tracking.") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = transactions.filter { selectedTransactionIds.contains(it.id) }
                        toDelete.forEach { t ->
                            viewModel.deleteTransaction(t)
                        }
                        selectedTransactionIds.clear()
                        isSelectionMode = false
                        showMultipleDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_multiple_delete")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showMultipleDeleteConfirmation = false },
                    modifier = Modifier.testTag("cancel_multiple_delete")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("multiple_delete_dialog")
        )
    }

    if (showSupabaseSyncDialog) {
        SupabaseSyncDialog(
            onDismissRequest = { showSupabaseSyncDialog = false },
            viewModel = viewModel
        )
    }
}
