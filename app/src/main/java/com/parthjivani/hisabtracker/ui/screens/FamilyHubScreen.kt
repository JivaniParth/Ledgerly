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

// FAMILY HUB SCREEN
@Composable
fun FamilyHubScreen(
    familyMembers: List<FamilyMember>,
    transactions: List<Transaction>,
    onAddNewMember: (String, String) -> Unit,
    viewModel: HisabViewModel,
    onFamilyMemberClick: (Long) -> Unit,
    onCreditCardClick: (Long) -> Unit
) {
    var selectedHubTab by remember { mutableStateOf(0) } // 0 = Members, 1 = Credit Cards

    // Member form values
    var inlineFormExpanded by remember { mutableStateOf(false) }
    var nameStr by remember { mutableStateOf("") }
    var relationStr by remember { mutableStateOf("") }
    var nameValidationErr by remember { mutableStateOf<String?>(null) }
    var relationValidationErr by remember { mutableStateOf<String?>(null) }

    // Edit member state
    var editingMember by remember { mutableStateOf<FamilyMember?>(null) }
    var editNameStr by remember { mutableStateOf("") }
    var editRelationStr by remember { mutableStateOf("") }

    // Card form values
    var cardFormExpanded by remember { mutableStateOf(false) }
    var cardNameStr by remember { mutableStateOf("") }
    var cardLimitStr by remember { mutableStateOf("") }
    var cardOwnerName by remember { mutableStateOf("") }
    val creditCards by viewModel.allCreditCards.collectAsState()

    // Edit card state
    var editingCard by remember { mutableStateOf<CreditCard?>(null) }
    var editCardNameStr by remember { mutableStateOf("") }
    var editCardOwnerName by remember { mutableStateOf("") }

    if (editingMember != null) {
        // Edit Member Dialog
        AlertDialog(
            onDismissRequest = { editingMember = null },
            title = { Text("Edit Family Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editNameStr,
                        onValueChange = { editNameStr = it },
                        label = { Text("Full Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editRelationStr,
                        onValueChange = { editRelationStr = it },
                        label = { Text("Relationship (e.g. Spouse)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val member = editingMember
                        if (member != null && editNameStr.isNotBlank() && editRelationStr.isNotBlank()) {
                            viewModel.updateFamilyMember(member.copy(name = editNameStr.trim(), relationship = editRelationStr.trim()))
                        }
                        editingMember = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMember = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (editingCard != null) {
        // Edit Credit Card Dialog
        AlertDialog(
            onDismissRequest = { editingCard = null },
            title = { Text("Edit Credit Card", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editCardNameStr,
                        onValueChange = { editCardNameStr = it },
                        label = { Text("Card Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Card Owner:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    var editOwnerMenuExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { editOwnerMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(editCardOwnerName, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(expanded = editOwnerMenuExpanded, onDismissRequest = { editOwnerMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("My Cards (Self)") }, onClick = { editCardOwnerName = "Self"; editOwnerMenuExpanded = false })
                            familyMembers.forEach { m ->
                                DropdownMenuItem(text = { Text(m.name) }, onClick = { editCardOwnerName = m.name; editOwnerMenuExpanded = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val card = editingCard
                        if (card != null && editCardNameStr.isNotBlank() && editCardOwnerName.isNotBlank()) {
                            viewModel.updateCreditCard(card.copy(cardName = editCardNameStr.trim(), ownerName = editCardOwnerName.trim()))
                        }
                        editingCard = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCard = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedHubTab == 0) {
                        inlineFormExpanded = !inlineFormExpanded
                    } else {
                        cardFormExpanded = !cardFormExpanded
                    }
                },
                modifier = Modifier.testTag(if (selectedHubTab == 0) "family_add_member_fab" else "family_add_card_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = if (selectedHubTab == 0) {
                        if (inlineFormExpanded) Icons.Default.Close else Icons.Default.PersonAdd
                    } else {
                        if (cardFormExpanded) Icons.Default.Close else Icons.Default.Add
                    },
                    contentDescription = "New Card or member"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .testTag("family_hub_screen")
        ) {
            // Hub Header
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
                        text = "Family Ledger Hub",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            // Navigation Tabs for Hub options
            TabRow(
                selectedTabIndex = selectedHubTab,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = selectedHubTab == 0,
                    onClick = { selectedHubTab = 0 },
                    text = { Text("Family Members", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedHubTab == 1,
                    onClick = { selectedHubTab = 1 },
                    text = { Text("Credit Cards Registry", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedHubTab == 0) {
                // ================== TAB 0: FAMILY MEMBERS ==================
                // Collapsible Form to adding Family Members
                AnimatedVisibility(
                    visible = inlineFormExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("add_family_form_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Add New Family Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = nameStr,
                                onValueChange = {
                                    nameStr = it
                                    if (it.trim().isNotEmpty()) nameValidationErr = null
                                },
                                label = { Text("Full Name") },
                                isError = nameValidationErr != null,
                                supportingText = { nameValidationErr?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("member_name_field"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            OutlinedTextField(
                                value = relationStr,
                                onValueChange = {
                                    relationStr = it
                                    if (it.trim().isNotEmpty()) relationValidationErr = null
                                },
                                label = { Text("Relationship (e.g. Mother, Brother)") },
                                isError = relationValidationErr != null,
                                supportingText = { relationValidationErr?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("member_relationship_field"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (nameStr.trim().isEmpty()) {
                                        nameValidationErr = "Name is required."
                                        return@Button
                                    }
                                    if (relationStr.trim().isEmpty()) {
                                        relationValidationErr = "Relationship description is required."
                                        return@Button
                                    }

                                    onAddNewMember(nameStr.trim(), relationStr.trim())
                                    nameStr = ""
                                    relationStr = ""
                                    inlineFormExpanded = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_member_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Add Account")
                            }
                        }
                    }
                }

                // Family Members List Grid
                if (familyMembers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No family accounts created yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).testTag("family_members_list")
                    ) {
                        items(familyMembers) { fm ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("family_member_item_${fm.id}")
                                    .clickable {
                                        onFamilyMemberClick(fm.id)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(fm.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(fm.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Text(fm.relationship, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    editNameStr = fm.name
                                                    editRelationStr = fm.relationship
                                                    editingMember = fm
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Member", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteFamilyMember(fm) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete account", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ================== TAB 1: CREDIT CARDS REGISTRY ==================
                AnimatedVisibility(
                    visible = cardFormExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("add_card_form_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Register Credit Card Ledger", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                            OutlinedTextField(
                                value = cardNameStr,
                                onValueChange = { cardNameStr = it },
                                label = { Text("Card Name/Issuer (e.g. SBI SimplySave)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Selector "Whose Card Is It" - Dropdown of family members + "Self"
                            Text("Whose Card Is It? (Tag Owner):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            var ownerMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { ownerMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(if (cardOwnerName.isEmpty()) "Assign Cardholder Owner" else cardOwnerName, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                                DropdownMenu(expanded = ownerMenuExpanded, onDismissRequest = { ownerMenuExpanded = false }) {
                                    DropdownMenuItem(text = { Text("My Cards (Self)") }, onClick = { cardOwnerName = "Self"; ownerMenuExpanded = false })
                                    familyMembers.forEach { m ->
                                        DropdownMenuItem(text = { Text(m.name) }, onClick = { cardOwnerName = m.name; ownerMenuExpanded = false })
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val owner = if (cardOwnerName.trim().isEmpty()) "Self" else cardOwnerName.trim()
                                    if (cardNameStr.trim().isNotBlank()) {
                                        viewModel.addCreditCard(cardNameStr.trim(), owner, 0.0)
                                        cardNameStr = ""
                                        cardLimitStr = ""
                                        cardOwnerName = ""
                                        cardFormExpanded = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Register Card Account")
                            }
                        }
                    }
                }

                // Cards Register list
                if (creditCards.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No credit cards added to the ledger yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Use the add button in the bottom right to register credit cards.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(creditCards) { card ->
                            val spentOnCard = transactions
                                .filter { it.sourceWallet == WalletType.CREDIT_CARD && it.creditCardId == card.id }
                                .sumOf { it.amount }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCreditCardClick(card.id)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(card.cardName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text("Owner: ${card.ownerName} • Total Spent: ${formatCurrency(spentOnCard)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            editCardNameStr = card.cardName
                                            editCardOwnerName = card.ownerName
                                            editingCard = card
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit card", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteCreditCard(card) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove card link", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
