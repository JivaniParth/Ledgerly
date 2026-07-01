package com.parthjivani.hisabtracker.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parthjivani.hisabtracker.utils.AmountUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var showCalculator by remember { mutableStateOf(false) }
    var currentExpression by remember { mutableStateOf(value) }
    
    LaunchedEffect(value) {
        if (!showCalculator) {
            currentExpression = value
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            currentExpression = value
            showCalculator = true
        }
    }

    Column(modifier = modifier) {
        val displayValue = if (value.isNotEmpty() && !value.any { "+-*/".contains(it) }) {
            AmountUtils.formatIndianCurrency(value)
        } else {
            value
        }

        OutlinedTextField(
            value = displayValue,
            onValueChange = {}, // Read-only from physical keyboard
            label = label,
            readOnly = true,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            singleLine = true
        )

        val numericValue = value.toDoubleOrNull()
        if (numericValue != null && numericValue > 0) {
            Text(
                text = AmountUtils.convertAmountToWords(numericValue),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
            )
        }
    }

    if (showCalculator) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { 
                showCalculator = false
            },
            sheetState = sheetState
        ) {
            CalculatorKeyboard(
                currentExpression = currentExpression,
                onExpressionChange = { newExpr ->
                    currentExpression = newExpr
                    onValueChange(newExpr)
                },
                onDone = {
                    showCalculator = false
                }
            )
        }
    }
}
