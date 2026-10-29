package com.parthjivani.ledgerly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parthjivani.ledgerly.utils.AmountUtils

@Composable
fun CalculatorKeyboard(
    currentExpression: String,
    onExpressionChange: (String) -> Unit,
    onDone: () -> Unit
) {
    fun evaluate(expr: String): String {
        try {
            var cleanExpr = expr.trim()
            while (cleanExpr.isNotEmpty() && "+-*/.".contains(cleanExpr.last())) {
                cleanExpr = cleanExpr.dropLast(1)
            }
            if (cleanExpr.isEmpty()) return ""

            val tokens = ArrayList<String>()
            var currentNum = ""
            for (char in cleanExpr) {
                if ("+-*/".contains(char)) {
                    if (currentNum.isNotEmpty()) {
                        tokens.add(currentNum)
                        currentNum = ""
                    }
                    tokens.add(char.toString())
                } else {
                    currentNum += char
                }
            }
            if (currentNum.isNotEmpty()) {
                tokens.add(currentNum)
            }
            if (tokens.isEmpty()) return ""

            // Handle leading negative number: e.g. ["-", "50"] -> ["-50"]
            if (tokens.size >= 2 && tokens[0] == "-") {
                tokens[1] = "-" + tokens[1]
                tokens.removeAt(0)
            }

            // BEDMAS: Multiplication and Division first
            var i = 0
            while (i < tokens.size) {
                if (tokens[i] == "*" || tokens[i] == "/") {
                    if (i == 0 || i + 1 >= tokens.size) return ""
                    val a = tokens[i - 1].toDoubleOrNull() ?: return ""
                    val b = tokens[i + 1].toDoubleOrNull() ?: return ""
                    if (tokens[i] == "/" && b == 0.0) {
                        // Division by zero disallowed
                        return ""
                    }
                    val res = if (tokens[i] == "*") a * b else a / b
                    if (res.isInfinite() || res.isNaN()) return ""
                    tokens[i - 1] = res.toString()
                    tokens.removeAt(i) // remove operator
                    tokens.removeAt(i) // remove b
                    i--
                }
                i++
            }

            // Addition and Subtraction
            i = 0
            while (i < tokens.size) {
                if (tokens[i] == "+" || tokens[i] == "-") {
                    if (i == 0 || i + 1 >= tokens.size) return ""
                    val a = tokens[i - 1].toDoubleOrNull() ?: return ""
                    val b = tokens[i + 1].toDoubleOrNull() ?: return ""
                    val res = if (tokens[i] == "+") a + b else a - b
                    if (res.isInfinite() || res.isNaN()) return ""
                    tokens[i - 1] = res.toString()
                    tokens.removeAt(i) // remove operator
                    tokens.removeAt(i) // remove b
                    i--
                }
                i++
            }

            val result = tokens[0].toDoubleOrNull() ?: return ""
            if (result.isInfinite() || result.isNaN()) return ""
            val rounded = Math.round(result * 100.0) / 100.0
            return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else String.format(java.util.Locale.US, "%.2f", rounded)
        } catch (e: Exception) {
            return ""
        }
    }

    val handleKeyPress: (String) -> Unit = { key ->
        when (key) {
            "C" -> onExpressionChange("")
            "<" -> if (currentExpression.isNotEmpty()) onExpressionChange(currentExpression.dropLast(1))
            "=" -> {
                onExpressionChange(evaluate(currentExpression))
            }
            "DONE" -> {
                val evaluated = evaluate(currentExpression)
                onExpressionChange(evaluated)
                onDone()
            }
            else -> {
                // Prevent multiple operators
                if ("+-*/".contains(key) && currentExpression.isNotEmpty() && "+-*/".contains(currentExpression.last())) {
                    onExpressionChange(currentExpression.dropLast(1) + key)
                } else {
                    onExpressionChange(currentExpression + key)
                }
            }
        }
    }

    val buttons = listOf(
        listOf("C", "/", "*", "<"),
        listOf("7", "8", "9", "-"),
        listOf("4", "5", "6", "+"),
        listOf("1", "2", "3", "="),
        listOf("0", "00", ".", "DONE")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Display Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(horizontalAlignment = Alignment.End) {
                // Extract just the numbers for formatting, keep operators as is
                // Actually, just showing the raw expression is fine during calculation.
                // Or we format the final number. If it contains operators, we just show it.
                val hasOperator = currentExpression.any { "+-*/".contains(it) }
                val displayText = if (hasOperator) {
                    currentExpression
                } else {
                    if (currentExpression.isEmpty()) "0" else AmountUtils.formatIndianCurrency(currentExpression)
                }

                Text(
                    text = displayText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                
                val evaluatedNum = evaluate(currentExpression).toDoubleOrNull()
                if (evaluatedNum != null && evaluatedNum > 0) {
                    Text(
                        text = AmountUtils.convertAmountToWords(evaluatedNum),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("Zero Rupees", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0f)) // Placeholder for height
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Keypad
        for (row in buttons) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (btn in row) {
                    val isAction = listOf("C", "<", "/", "*", "-", "+", "=").contains(btn)
                    val isDone = btn == "DONE"
                    val bgColor = when {
                        isDone -> MaterialTheme.colorScheme.primary
                        isAction -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val contentColor = when {
                        isDone -> MaterialTheme.colorScheme.onPrimary
                        isAction -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .clickable { handleKeyPress(btn) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (btn == "<") {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = contentColor)
                        } else if (btn == "DONE") {
                            Icon(Icons.Default.Check, contentDescription = "Done", tint = contentColor)
                        } else {
                            Text(
                                text = btn,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp)) // Bottom padding for navigation bar
    }
}
