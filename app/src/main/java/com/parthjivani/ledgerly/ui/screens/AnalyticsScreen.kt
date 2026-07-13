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

// ANALYTICS SCREEN: Monthly Budget Estimator/Projector
@Composable
fun AnalyticsScreen(
    transactions: List<Transaction>,
    projectedAverage: Double,
    viewModel: LedgerlyViewModel
) {
    // Collect specific Expense Outflows occurring in last 90 days (including operational and credit cards as requested).
    val currentMillis = System.currentTimeMillis()
    val ninetyDaysAgo = currentMillis - (90L * 24L * 60L * 60L * 1000L)

    val savingsGoals by viewModel.allSavingsGoals.collectAsState()

    val expenseOutflows = transactions.filter { t ->
        t.date >= ninetyDaysAgo && (
                (t.type == TransactionType.PERSONAL_EXPENSE && (t.sourceWallet == WalletType.EXPENSE_WALLET || t.sourceWallet == WalletType.CREDIT_CARD)) ||
                        (t.type == TransactionType.FAMILY_TRANSFER && t.sourceWallet == WalletType.EXPENSE_WALLET && t.direction == TransactionDirection.GIVEN_TO_THEM)
                )
    }

    // Explicit credit card spent block
    val creditCardOutflows = transactions.filter { t ->
        t.date >= ninetyDaysAgo && t.sourceWallet == WalletType.CREDIT_CARD
    }

    val totalSpending90Days = expenseOutflows.sumOf { it.amount }
    val totalCreditCardSpending = creditCardOutflows.sumOf { it.amount }

    // Display
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_screen")
            .padding(16.dp)
    ) {
        item {
            // Core Header
        AppLogo()
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Estimation Insight",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Large Estimated Projection gauge card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("projected_monthly_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Cost Projection",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "Based on 3 months",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val correctedProjection = totalSpending90Days / 3.0
                    Text(
                        text = formatCurrency(correctedProjection),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Light,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "est. expenses / mo",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress gauge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.65f) // Styled as 65% visually
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Credit Cards specific spending indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = "Credit Card Spent",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Credit Cards Spending (90 Days)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Total payments recorded via card wallets", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(formatCurrency(totalCreditCardSpending), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Spent breakdown detail card list
        Text(
            text = "Calculation Transparency Audit",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Outflows (90 Days)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(formatCurrency(totalSpending90Days), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Included Credit Card Outflow", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(totalCreditCardSpending), fontWeight = FontWeight.Normal, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Time Frame Span", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("3 Months (90 days)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Projected math", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("${formatCurrency(totalSpending90Days)} / 3", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }



        // Items audited details lists
        Text(
            text = "Expense Outflows Audited (${expenseOutflows.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        } // Close item { block

        if (expenseOutflows.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No qualifying expense outflows in last 90 days.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(expenseOutflows) { item ->
                TransactionRow(
                    transaction = item,
                    familyMembers = emptyList() // Details can be simplified or complete
                )
            }
        }
    }
}
