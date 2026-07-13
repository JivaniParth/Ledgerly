package com.parthjivani.ledgerly.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parthjivani.ledgerly.data.CreditCard
import com.parthjivani.ledgerly.data.Transaction
import com.parthjivani.ledgerly.data.TransactionDirection
import com.parthjivani.ledgerly.data.TransactionType
import com.parthjivani.ledgerly.data.WalletType
import java.text.DecimalFormat
import java.util.Locale

enum class TableImportType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    CREDIT_CARD_SPEND("Credit Card"),
    IGNORE("Ignore Table")
}

data class TableSelectionState(
    val importType: TableImportType,
    val targetWallet: WalletType,
    val defaultCategory: String,
    val selectedCreditCardId: Long?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: LedgerlyViewModel
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    
    // ViewModel states
    val categoriesList by viewModel.categories.collectAsState()
    val incomeCategoriesList by viewModel.incomeCategories.collectAsState()
    val creditCards by viewModel.allCreditCards.collectAsState()
    
    val fallbackCategories = listOf("Groceries", "Entertainment", "Bills", "Travel", "Medical", "Other")
    val displayCategories = categoriesList.ifEmpty { fallbackCategories }
    
    val fallbackIncomeCategories = listOf("Salary", "Business", "Investment", "Freelance", "Gift", "Other")
    val displayIncomeCategories = incomeCategoriesList.ifEmpty { fallbackIncomeCategories }

    // Upload & Parser states
    var uploadedFileName by remember { mutableStateOf<String?>(null) }
    var extractedSheets by remember { mutableStateOf<List<ParsedExcelSheet>>(emptyList()) }
    var extractedTables by remember { mutableStateOf<List<ExtractedTable>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    // Selection states: Key is "sheetName_title_headerIdx"
    val tableConfigs = remember { mutableStateMapOf<String, TableSelectionState>() }
    // Preview toggle states: Key of Table
    val tablePreviewsExpanded = remember { mutableStateMapOf<String, Boolean>() }

    // File Picker Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isAnalyzing = true
            errorMessage = null
            try {
                // Fetch display name
                var name = "Excel File"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIdx)
                    }
                }
                uploadedFileName = name

                // Read bytes
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val sheets = ExcelParser.parseExcelZip(bytes)
                    val tables = ExcelParser.extractTablesFromSheets(sheets)
                    extractedSheets = sheets
                    extractedTables = tables
                    
                    if (tables.isEmpty()) {
                        errorMessage = "No transaction tables found. Please verify sheets content contains 'Date', 'Amount' and 'Description' headers."
                    } else {
                        // Pre-populate configs based on sheet or table titles
                        tables.forEachIndexed { index, table ->
                            val key = "${index}_${table.sheetName}_${table.title}_${table.headerRowIndex}"
                            val lowerTitle = table.title.lowercase()
                            val lowerSheet = table.sheetName.lowercase()
                            
                            val detectedType = when {
                                lowerTitle.contains("income") || lowerTitle.contains("salary") || lowerTitle.contains("earning") || lowerTitle.contains("revenue") || lowerTitle.contains("received") || lowerTitle.contains("deposit") || lowerTitle.contains("inflow") ||
                                lowerSheet.contains("income") || lowerSheet.contains("salary") || lowerSheet.contains("earning") || lowerSheet.contains("received") || lowerSheet.contains("deposit") -> TableImportType.INCOME
                                lowerTitle.contains("credit") || lowerTitle.contains("cc ") || lowerTitle.contains("card") || lowerTitle.contains("hdfc") || lowerTitle.contains("sbi") || lowerTitle.contains("icici") || lowerTitle.contains("axis") || lowerTitle.contains("amex") || lowerTitle.contains("onecard") || lowerTitle.contains("bill") ||
                                lowerSheet.contains("credit") || lowerSheet.contains("card") || lowerSheet.contains("hdfc") || lowerSheet.contains("sbi") || lowerSheet.contains("icici") || lowerSheet.contains("axis") || lowerSheet.contains("onecard") -> TableImportType.CREDIT_CARD_SPEND
                                else -> TableImportType.EXPENSE
                            }
                            
                            val defaultCat = if (detectedType == TableImportType.INCOME) {
                                displayIncomeCategories.firstOrNull() ?: "Salary"
                            } else {
                                displayCategories.firstOrNull() ?: "Groceries"
                            }
                            
                            tableConfigs[key] = TableSelectionState(
                                importType = detectedType,
                                targetWallet = if (detectedType == TableImportType.INCOME) WalletType.SAVINGS_WALLET else WalletType.EXPENSE_WALLET,
                                defaultCategory = defaultCat,
                                selectedCreditCardId = creditCards.firstOrNull()?.id
                            )
                        }
                    }
                } else {
                    errorMessage = "Failed to load spreadsheet content."
                }
            } catch (e: Exception) {
                errorMessage = "Parsing Error: ${e.localizedMessage ?: "Invalid file structure"}"
            } finally {
                isAnalyzing = false
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("excel_import_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bulk Importer Wizard",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Map tables from April to March financial sheets",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("excel_import_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    if (extractedTables.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                // Clear all states
                                uploadedFileName = null
                                extractedSheets = emptyList()
                                extractedTables = emptyList()
                                tableConfigs.clear()
                                tablePreviewsExpanded.clear()
                                errorMessage = null
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Decompressing & analyzing workbook...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (extractedTables.isEmpty()) {
                // Empty state or file picker
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilePresent,
                        contentDescription = "Excel",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Import Excel Records",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Upload your Indian Financial Year spreadsheet (covering months April to March). Our intelligent scanner will automatically locate separate tables for Income, Expenses, and Credit Card statements across all workbook sheets.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { launcher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .testTag("upload_excel_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Spreadsheet (.xlsx)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Table Mapping Interface
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Feedback header status
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = uploadedFileName ?: "Spreadsheet loaded",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "${extractedSheets.size} sheets, ${extractedTables.size} tables",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Classify Extracted Tables",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        itemsIndexed(extractedTables, key = { index, table -> "${index}_${table.sheetName}_${table.title}_${table.headerRowIndex}" }) { index, table ->
                            val key = "${index}_${table.sheetName}_${table.title}_${table.headerRowIndex}"
                            val config = tableConfigs[key] ?: TableSelectionState(TableImportType.IGNORE, WalletType.EXPENSE_WALLET, "Other", null)
                            val isExpanded = tablePreviewsExpanded[key] ?: false
                            
                            // Let's compute statistics for this card
                            val validRowsCount = table.rows.count { 
                                ExcelParser.parseAmount(it.rawAmount) != null 
                            }
                            val totalSum = table.rows.fold(0.0) { acc, row ->
                                acc + (ExcelParser.parseAmount(row.rawAmount) ?: 0.0)
                            }
                            val sumFormatter = DecimalFormat("#,##,##0.00")

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (config.importType == TableImportType.IGNORE) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    }
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (config.importType == TableImportType.IGNORE) {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    }
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Row 1: Sheet & Row Info
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = table.sheetName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Text(
                                                text = "Row ${table.headerRowIndex + 1}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Badge
                                        Surface(
                                            color = when (config.importType) {
                                                TableImportType.INCOME -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                                                TableImportType.EXPENSE -> Color(0xFFC62828).copy(alpha = 0.15f)
                                                TableImportType.CREDIT_CARD_SPEND -> Color(0xFF1565C0).copy(alpha = 0.15f)
                                                TableImportType.IGNORE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = config.importType.label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (config.importType) {
                                                    TableImportType.INCOME -> Color(0xFF2E7D32)
                                                    TableImportType.EXPENSE -> Color(0xFFC62828)
                                                    TableImportType.CREDIT_CARD_SPEND -> Color(0xFF1565C0)
                                                    TableImportType.IGNORE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Table title or title above headers
                                    Text(
                                        text = table.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Statistics row
                                    Text(
                                        text = "$validRowsCount rows mapped • Total ₹ ${sumFormatter.format(totalSum)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Select Import Classification Flow
                                    Text(
                                        text = "Import table as:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // Row Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        TableImportType.values().forEach { type ->
                                            val isSelected = config.importType == type
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) {
                                                    when (type) {
                                                        TableImportType.INCOME -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                                                        TableImportType.EXPENSE -> Color(0xFFC62828).copy(alpha = 0.15f)
                                                        TableImportType.CREDIT_CARD_SPEND -> Color(0xFF1565C0).copy(alpha = 0.15f)
                                                        TableImportType.IGNORE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                                    }
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                },
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = if (isSelected) {
                                                        when (type) {
                                                            TableImportType.INCOME -> Color(0xFF2E7D32)
                                                            TableImportType.EXPENSE -> Color(0xFFC62828)
                                                            TableImportType.CREDIT_CARD_SPEND -> Color(0xFF1565C0)
                                                            TableImportType.IGNORE -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    } else {
                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        val nextCat = if (type == TableImportType.INCOME) {
                                                            displayIncomeCategories.firstOrNull() ?: "Salary"
                                                        } else {
                                                            displayCategories.firstOrNull() ?: "Groceries"
                                                        }
                                                        tableConfigs[key] = config.copy(
                                                            importType = type,
                                                            defaultCategory = nextCat
                                                        )
                                                    }
                                            ) {
                                                Text(
                                                    text = type.label.substringBefore(" Table"),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    textAlign = TextAlign.Center,
                                                    color = if (isSelected) {
                                                        when (type) {
                                                            TableImportType.INCOME -> Color(0xFF1B5E20)
                                                            TableImportType.EXPENSE -> Color(0xFFB71C1C)
                                                            TableImportType.CREDIT_CARD_SPEND -> Color(0xFF0D47A1)
                                                            TableImportType.IGNORE -> MaterialTheme.colorScheme.onSurface
                                                        }
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Secondary Contextual Parameters
                                    AnimatedVisibility(
                                        visible = config.importType != TableImportType.IGNORE,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                            
                                            if (config.importType == TableImportType.EXPENSE || config.importType == TableImportType.INCOME) {
                                                // Wallet selection Row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = if (config.importType == TableImportType.INCOME) "Deposit to wallet:" else "Paid from wallet:",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        listOf(WalletType.EXPENSE_WALLET, WalletType.SAVINGS_WALLET).forEach { wallet ->
                                                            val walletIsSelected = config.targetWallet == wallet
                                                            val label = if (wallet == WalletType.EXPENSE_WALLET) "Expense Wallet" else "Savings Wallet"
                                                            
                                                            FilterChip(
                                                                selected = walletIsSelected,
                                                                onClick = { tableConfigs[key] = config.copy(targetWallet = wallet) },
                                                                label = { Text(label, fontSize = 10.sp) },
                                                                colors = FilterChipDefaults.filterChipColors(
                                                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                                ),
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            } else if (config.importType == TableImportType.CREDIT_CARD_SPEND) {
                                                // Credit card selector Row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Select Credit Card profile:",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    
                                                    if (creditCards.isEmpty()) {
                                                        Text("No credit cards set up", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                                    } else {
                                                        var ccMenuExpanded by remember { mutableStateOf(false) }
                                                        val selectedCard = creditCards.find { it.id == config.selectedCreditCardId } ?: creditCards.first()
                                                        
                                                        Box {
                                                            OutlinedCard(
                                                                onClick = { ccMenuExpanded = true },
                                                                shape = RoundedCornerShape(8.dp)
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Text(selectedCard.cardName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                                                }
                                                            }
                                                            DropdownMenu(
                                                                expanded = ccMenuExpanded,
                                                                onDismissRequest = { ccMenuExpanded = false }
                                                            ) {
                                                                creditCards.forEach { card ->
                                                                    DropdownMenuItem(
                                                                        text = { Text("${card.cardName} (${card.ownerName})", fontSize = 12.sp) },
                                                                        onClick = {
                                                                            tableConfigs[key] = config.copy(selectedCreditCardId = card.id)
                                                                            ccMenuExpanded = false
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Category selector Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Default Category if unmapped:",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                
                                                val categories = if (config.importType == TableImportType.INCOME) displayIncomeCategories else displayCategories
                                                var catMenuExpanded by remember { mutableStateOf(false) }
                                                
                                                Box {
                                                    OutlinedCard(
                                                        onClick = { catMenuExpanded = true },
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Text(config.defaultCategory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                    DropdownMenu(
                                                        expanded = catMenuExpanded,
                                                        onDismissRequest = { catMenuExpanded = false }
                                                    ) {
                                                        categories.forEach { cat ->
                                                            DropdownMenuItem(
                                                                text = { Text(cat, fontSize = 12.sp) },
                                                                onClick = {
                                                                    tableConfigs[key] = config.copy(defaultCategory = cat)
                                                                    catMenuExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Preview rows toggle button
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { tablePreviewsExpanded[key] = !isExpanded }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isExpanded) "Hide data preview" else "Show sample rows (${table.rows.size} found)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Preview expansion table list view
                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Preview row headers
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Date", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f))
                                                Text("Description", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(3.5f))
                                                Text("Category", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f))
                                                Text("Amount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                            }
                                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                            
                                            // Display first 4 items as a preview
                                            table.rows.take(4).forEach { row ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(row.rawDate, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(row.rawDesc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(3.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(row.rawCategory.ifEmpty { "-" }, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(row.rawAmount, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                                }
                                            }
                                            if (table.rows.size > 4) {
                                                Text(
                                                    text = "And ${table.rows.size - 4} more rows...",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(top = 4.dp),
                                                    fontWeight = FontWeight.Light
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Execute Import Container
                    val totalToImport = extractedTables.mapIndexed { index, table ->
                        val key = "${index}_${table.sheetName}_${table.title}_${table.headerRowIndex}"
                        val config = tableConfigs[key]
                        if (config != null && config.importType != TableImportType.IGNORE) {
                            table.rows.count { ExcelParser.parseAmount(it.rawAmount) != null }
                        } else 0
                    }.sum()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Bulk Action Summary", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$totalToImport transactions to load", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                
                                Button(
                                    enabled = totalToImport > 0,
                                    onClick = {
                                        val transactionsToInsert = mutableListOf<Transaction>()
                                        
                                        extractedTables.forEachIndexed { index, table ->
                                            val key = "${index}_${table.sheetName}_${table.title}_${table.headerRowIndex}"
                                            val config = tableConfigs[key] ?: return@forEachIndexed
                                            if (config.importType == TableImportType.IGNORE) return@forEachIndexed

                                            table.rows.forEach { row ->
                                                val amt = ExcelParser.parseAmount(row.rawAmount) ?: return@forEach
                                                val parsedDate = ExcelParser.parseExcelDate(row.rawDate, table.sheetName)
                                                
                                                val notes = row.rawDesc.ifEmpty { "Imported from Excel Row ${row.originalRowIndex + 1}" }
                                                
                                                // Resolve category: 
                                                // 1. If row had a non-empty rawCategory, check if it's in our categories/income list, or capitalize it
                                                var resolvedCategory = config.defaultCategory
                                                if (row.rawCategory.isNotEmpty()) {
                                                    resolvedCategory = row.rawCategory.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                                }

                                                val familyMembers = viewModel.allFamilyMembers.value
                                                val matchedMember = familyMembers.firstOrNull { member ->
                                                    val cleanNote = notes.lowercase()
                                                    val mName = member.name.lowercase()
                                                    cleanNote.contains(mName) || 
                                                    cleanNote.split("\\s+|,|\\-|\\(|\\)".toRegex()).any { it == mName }
                                                }

                                                val transaction = when (config.importType) {
                                                    TableImportType.INCOME -> {
                                                        if (matchedMember != null) {
                                                            Transaction(
                                                                amount = amt,
                                                                date = parsedDate,
                                                                notes = notes,
                                                                type = TransactionType.FAMILY_TRANSFER,
                                                                sourceWallet = config.targetWallet,
                                                                familyMemberId = matchedMember.id,
                                                                direction = TransactionDirection.RECEIVED_FROM_THEM,
                                                                category = "Family"
                                                            )
                                                        } else {
                                                            Transaction(
                                                                amount = amt,
                                                                date = parsedDate,
                                                                notes = notes,
                                                                type = TransactionType.PERSONAL_INCOME,
                                                                sourceWallet = config.targetWallet,
                                                                category = resolvedCategory
                                                            )
                                                        }
                                                    }
                                                    TableImportType.EXPENSE -> {
                                                        if (matchedMember != null) {
                                                            Transaction(
                                                                amount = amt,
                                                                date = parsedDate,
                                                                notes = notes,
                                                                type = TransactionType.FAMILY_TRANSFER,
                                                                sourceWallet = config.targetWallet,
                                                                familyMemberId = matchedMember.id,
                                                                direction = TransactionDirection.GIVEN_TO_THEM,
                                                                category = "Family"
                                                            )
                                                        } else {
                                                            Transaction(
                                                                amount = amt,
                                                                date = parsedDate,
                                                                notes = notes,
                                                                type = TransactionType.PERSONAL_EXPENSE,
                                                                sourceWallet = config.targetWallet,
                                                                category = resolvedCategory
                                                            )
                                                        }
                                                    }
                                                    TableImportType.CREDIT_CARD_SPEND -> {
                                                        if (matchedMember != null) {
                                                            Transaction(
                                                                amount = amt,
                                                                date = parsedDate,
                                                                notes = notes,
                                                                type = TransactionType.FAMILY_TRANSFER,
                                                                sourceWallet = WalletType.CREDIT_CARD,
                                                                familyMemberId = matchedMember.id,
                                                                direction = TransactionDirection.GIVEN_TO_THEM,
                                                                category = "Family",
                                                                creditCardId = config.selectedCreditCardId ?: creditCards.firstOrNull()?.id
                                                            )
                                                        } else {
                                                            Transaction(
                                                                amount = amt,
                                                                date = parsedDate,
                                                                notes = notes,
                                                                type = TransactionType.PERSONAL_EXPENSE,
                                                                sourceWallet = WalletType.CREDIT_CARD,
                                                                category = resolvedCategory,
                                                                creditCardId = config.selectedCreditCardId ?: creditCards.firstOrNull()?.id
                                                            )
                                                        }
                                                    }
                                                    else -> null
                                                }
                                                
                                                if (transaction != null) {
                                                    transactionsToInsert.add(transaction)
                                                }
                                            }
                                        }

                                        if (transactionsToInsert.isNotEmpty()) {
                                            viewModel.addTransactions(transactionsToInsert)
                                            Toast.makeText(context, "Successfully imported ${transactionsToInsert.size} transactions!", Toast.LENGTH_LONG).show()
                                            onNavigateBack()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                    modifier = Modifier.testTag("execute_import_button")
                                ) {
                                    Icon(Icons.Default.DoneAll, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
