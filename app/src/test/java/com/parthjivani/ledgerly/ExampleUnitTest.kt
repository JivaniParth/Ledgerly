package com.parthjivani.ledgerly

import com.parthjivani.ledgerly.utils.AmountUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testFormatIndianCurrency() {
    assertEquals("1,000", AmountUtils.formatIndianCurrency("1000"))
    assertEquals("10,000", AmountUtils.formatIndianCurrency("10000"))
    assertEquals("1,00,000", AmountUtils.formatIndianCurrency("100000"))
    assertEquals("10,00,000", AmountUtils.formatIndianCurrency("1000000"))
    assertEquals("1,00,00,000", AmountUtils.formatIndianCurrency("10000000"))
    assertEquals("-5,000", AmountUtils.formatIndianCurrency("-5000"))
    assertEquals("1,234.56", AmountUtils.formatIndianCurrency("1234.56"))
    assertEquals("", AmountUtils.formatIndianCurrency(""))
  }

  @Test
  fun testConvertAmountToWords() {
    assertEquals("Zero Rupees", AmountUtils.convertAmountToWords(0.0))
    assertEquals("One Hundred Rupees", AmountUtils.convertAmountToWords(100.0))
    assertEquals("One Thousand Rupees", AmountUtils.convertAmountToWords(1000.0))
    assertEquals("One Lakh Rupees", AmountUtils.convertAmountToWords(100000.0))
    assertEquals("One Crore Rupees", AmountUtils.convertAmountToWords(10000000.0))
    assertEquals("One Thousand Two Hundred Thirty Four Rupees and Fifty Paise", AmountUtils.convertAmountToWords(1234.50))
  }
}
