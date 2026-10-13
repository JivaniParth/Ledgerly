/**
 * Utility functions for formatting Indian currency, evaluating math expressions,
 * and converting numeric amounts to Indian English words (Rupees & Paise).
 * Full feature parity with Android's AmountUtils.kt & CalculatorKeyboard.kt
 */

export function numberToWords(number: bigint): string {
  if (number === 0n) return '';

  const units = [
    '',
    'One',
    'Two',
    'Three',
    'Four',
    'Five',
    'Six',
    'Seven',
    'Eight',
    'Nine',
    'Ten',
    'Eleven',
    'Twelve',
    'Thirteen',
    'Fourteen',
    'Fifteen',
    'Sixteen',
    'Seventeen',
    'Eighteen',
    'Nineteen',
  ];

  const tens = [
    '',
    '',
    'Twenty',
    'Thirty',
    'Forty',
    'Fifty',
    'Sixty',
    'Seventy',
    'Eighty',
    'Ninety',
  ];

  if (number < 20n) return units[Number(number)];
  if (number < 100n) {
    const rem = number % 10n;
    return tens[Number(number / 10n)] + (rem !== 0n ? ' ' + units[Number(rem)] : '');
  }
  if (number < 1000n) {
    const rem = number % 100n;
    return units[Number(number / 100n)] + ' Hundred' + (rem !== 0n ? ' ' + numberToWords(rem) : '');
  }
  if (number < 100000n) {
    const rem = number % 1000n;
    return numberToWords(number / 1000n) + ' Thousand' + (rem !== 0n ? ' ' + numberToWords(rem) : '');
  }
  if (number < 10000000n) {
    const rem = number % 100000n;
    return numberToWords(number / 100000n) + ' Lakh' + (rem !== 0n ? ' ' + numberToWords(rem) : '');
  }

  const rem = number % 10000000n;
  return numberToWords(number / 10000000n) + ' Crore' + (rem !== 0n ? ' ' + numberToWords(rem) : '');
}

/**
 * Converts a numeric amount to words in Indian format (e.g. 5250.50 -> "Five Thousand Two Hundred Fifty Rupees and Fifty Paise")
 */
export function convertAmountToWords(amount: number): string {
  if (isNaN(amount) || amount === 0) return 'Zero Rupees';
  if (amount < 0) return 'Minus ' + convertAmountToWords(-amount);

  const integerPart = BigInt(Math.floor(amount));
  const decimalPart = Math.round((amount - Math.floor(amount)) * 100);

  const rupeesInWords = numberToWords(integerPart);
  let result = rupeesInWords ? `${rupeesInWords} Rupees` : '';

  if (decimalPart > 0) {
    const paiseInWords = numberToWords(BigInt(decimalPart));
    result += (result ? ' and ' : '') + `${paiseInWords} Paise`;
  }

  return result || 'Zero Rupees';
}

/**
 * Applies Indian number grouping (last 3 digits, then groups of 2)
 */
export function formatIndianCurrency(amountStr: string): string {
  if (!amountStr) return '';
  const cleanString = amountStr.replace(/,/g, '');

  const parts = cleanString.split('.');
  let integerPart = parts[0];
  const fractionalPart = parts.length > 1 ? `.${parts[1]}` : '';

  if (!integerPart) integerPart = '0';

  const isNegative = integerPart.startsWith('-');
  if (isNegative) {
    integerPart = integerPart.slice(1);
    if (!integerPart) integerPart = '0';
  }

  let formattedIntegerPart = '';
  if (integerPart.length > 3) {
    const lastThree = integerPart.slice(-3);
    const remaining = integerPart.slice(0, -3);

    const chunks: string[] = [];
    let i = remaining.length;
    while (i > 0) {
      const start = Math.max(0, i - 2);
      chunks.unshift(remaining.slice(start, i));
      i -= 2;
    }
    formattedIntegerPart = chunks.join(',') + ',' + lastThree;
  } else {
    formattedIntegerPart = integerPart;
  }

  const sign = isNegative ? '-' : '';
  return `${sign}${formattedIntegerPart}${fractionalPart}`;
}

/**
 * Evaluates standard arithmetic expressions (+, -, *, /) respecting operator precedence
 */
export function evaluateMathExpression(expr: string): string {
  try {
    let clean = (expr || '').replace(/,/g, '').trim();
    while (clean.length > 0 && '+-*/.'.includes(clean[clean.length - 1])) {
      clean = clean.slice(0, -1).trim();
    }
    if (!clean) return '';

    // Tokenize numbers and operators
    const tokens: string[] = [];
    let cur = '';
    for (let i = 0; i < clean.length; i++) {
      const ch = clean[i];
      if ('+-*/'.includes(ch)) {
        if (cur !== '') {
          tokens.push(cur);
          cur = '';
        }
        tokens.push(ch);
      } else if (ch !== ' ') {
        cur += ch;
      }
    }
    if (cur !== '') tokens.push(cur);
    if (tokens.length === 0) return '';

    // Handle leading negative number
    if (tokens[0] === '-') {
      tokens.shift();
      tokens[0] = '-' + tokens[0];
    }

    // BEDMAS: Multiplication and Division first
    let i = 0;
    while (i < tokens.length) {
      if (tokens[i] === '*' || tokens[i] === '/') {
        const a = parseFloat(tokens[i - 1]);
        const b = parseFloat(tokens[i + 1]);
        if (isNaN(a) || isNaN(b)) return clean;
        const res = tokens[i] === '*' ? a * b : b === 0 ? 0 : a / b;
        tokens[i - 1] = res.toString();
        tokens.splice(i, 2);
        i--;
      }
      i++;
    }

    // Addition and Subtraction
    i = 0;
    while (i < tokens.length) {
      if (tokens[i] === '+' || tokens[i] === '-') {
        const a = parseFloat(tokens[i - 1]);
        const b = parseFloat(tokens[i + 1]);
        if (isNaN(a) || isNaN(b)) return clean;
        const res = tokens[i] === '+' ? a + b : a - b;
        tokens[i - 1] = res.toString();
        tokens.splice(i, 2);
        i--;
      }
      i++;
    }

    const num = parseFloat(tokens[0]);
    if (isNaN(num)) return clean;
    // Format to 2 decimal places if needed or clean integer
    return (Math.round(num * 100) / 100).toString();
  } catch {
    return expr;
  }
}
