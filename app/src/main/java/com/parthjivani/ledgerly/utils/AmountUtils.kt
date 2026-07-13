package com.parthjivani.ledgerly.utils

object AmountUtils {
    
    fun formatIndianCurrency(amountStr: String): String {
        if (amountStr.isEmpty()) return ""
        val cleanString = amountStr.replace(",", "")
        
        // Split into integer and fractional parts
        val parts = cleanString.split(".")
        var integerPart = parts[0]
        val fractionalPart = if (parts.size > 1) ".${parts[1]}" else ""

        if (integerPart.isEmpty()) {
            integerPart = "0"
        }

        // Handle negative numbers
        val isNegative = integerPart.startsWith("-")
        if (isNegative) {
            integerPart = integerPart.substring(1)
            if (integerPart.isEmpty()) integerPart = "0"
        }

        // Apply Indian formatting to integer part
        val formattedIntegerPart = buildString {
            if (integerPart.length > 3) {
                // Take last 3 digits
                val lastThree = integerPart.substring(integerPart.length - 3)
                // The remaining digits will be grouped by 2
                val remaining = integerPart.substring(0, integerPart.length - 3)
                
                // Group by 2 digits
                var i = remaining.length
                while (i > 0) {
                    val start = maxOf(0, i - 2)
                    insert(0, remaining.substring(start, i))
                    if (start > 0) insert(0, ",")
                    i -= 2
                }
                append(",")
                append(lastThree)
            } else {
                append(integerPart)
            }
        }

        val sign = if (isNegative) "-" else ""
        return "$sign$formattedIntegerPart$fractionalPart"
    }

    fun convertAmountToWords(amount: Double): String {
        if (amount == 0.0) return "Zero Rupees"
        if (amount < 0) return "Minus " + convertAmountToWords(-amount)
        
        val longAmount = amount.toLong()
        val paise = Math.round((amount - longAmount) * 100).toInt()

        val rupeesInWords = numberToWords(longAmount)
        
        var result = if (rupeesInWords.isNotBlank()) "$rupeesInWords Rupees" else ""
        if (paise > 0) {
            val paiseInWords = numberToWords(paise.toLong())
            result += if (result.isNotEmpty()) " and $paiseInWords Paise" else "$paiseInWords Paise"
        }

        return result.ifEmpty { "Zero Rupees" }
    }

    private fun numberToWords(number: Long): String {
        if (number == 0L) return ""

        val units = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
        )
        val tens = arrayOf(
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        )

        return when {
            number < 20 -> units[number.toInt()]
            number < 100 -> tens[(number / 10).toInt()] + if (number % 10 != 0L) " " + units[(number % 10).toInt()] else ""
            number < 1000 -> units[(number / 100).toInt()] + " Hundred" + if (number % 100 != 0L) " " + numberToWords(number % 100) else ""
            number < 100000 -> numberToWords(number / 1000) + " Thousand" + if (number % 1000 != 0L) " " + numberToWords(number % 1000) else ""
            number < 10000000 -> numberToWords(number / 100000) + " Lakh" + if (number % 100000 != 0L) " " + numberToWords(number % 100000) else ""
            else -> numberToWords(number / 10000000) + " Crore" + if (number % 10000000 != 0L) " " + numberToWords(number % 10000000) else ""
        }
    }
}
