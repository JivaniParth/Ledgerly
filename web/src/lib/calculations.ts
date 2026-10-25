import { Transaction } from './types';

export function calculateExpenseBalance(transactions: Transaction[]): number {
  let balance = 0.0;
  for (const t of transactions) {
    const amt = Math.abs(t.amount || 0);
    switch (t.type) {
      case 'PERSONAL_INCOME':
        if (t.source_wallet === 'EXPENSE_WALLET') {
          balance += amt;
        }
        break;
      case 'PERSONAL_EXPENSE':
        if (t.source_wallet === 'EXPENSE_WALLET') {
          balance -= amt;
        }
        break;
      case 'FAMILY_TRANSFER':
      case 'BORROW_LEND':
        if (t.source_wallet === 'EXPENSE_WALLET' || t.destination_wallet === 'EXPENSE_WALLET') {
          if (t.direction === 'RECEIVED_FROM_THEM' || t.direction === 'BORROWED') {
            balance += amt;
          } else if (t.direction === 'GIVEN_TO_THEM' || t.direction === 'LENT') {
            balance -= amt;
          }
        }
        break;
      case 'WALLET_TRANSFER':
        if (t.source_wallet === 'EXPENSE_WALLET') {
          balance -= amt;
        }
        if (t.destination_wallet === 'EXPENSE_WALLET') {
          balance += amt;
        }
        break;
    }
  }
  return Math.round(balance * 100) / 100;
}

export function calculateSavingsBalance(transactions: Transaction[]): number {
  let balance = 0.0;
  for (const t of transactions) {
    const amt = Math.abs(t.amount || 0);
    switch (t.type) {
      case 'PERSONAL_INCOME':
        if (t.source_wallet === 'SAVINGS_WALLET') {
          balance += amt;
        }
        break;
      case 'PERSONAL_EXPENSE':
        if (t.source_wallet === 'SAVINGS_WALLET') {
          balance -= amt;
        }
        break;
      case 'FAMILY_TRANSFER':
      case 'BORROW_LEND':
        if (t.source_wallet === 'SAVINGS_WALLET' || t.destination_wallet === 'SAVINGS_WALLET') {
          if (t.direction === 'RECEIVED_FROM_THEM' || t.direction === 'BORROWED') {
            balance += amt;
          } else if (t.direction === 'GIVEN_TO_THEM' || t.direction === 'LENT') {
            balance -= amt;
          }
        }
        break;
      case 'WALLET_TRANSFER':
        if (t.source_wallet === 'SAVINGS_WALLET') {
          balance -= amt;
        }
        if (t.destination_wallet === 'SAVINGS_WALLET') {
          balance += amt;
        }
        break;
    }
  }
  return Math.round(balance * 100) / 100;
}

export function calculateCustomWalletBalance(walletId: number, transactions: Transaction[]): number {
  let balance = 0.0;
  for (const t of transactions) {
    const amt = Math.abs(t.amount || 0);
    // As source
    if (t.custom_wallet_id === walletId) {
      if (t.type === 'PERSONAL_INCOME') {
        balance += amt;
      } else if (t.type === 'PERSONAL_EXPENSE') {
        balance -= amt;
      } else if (t.type === 'FAMILY_TRANSFER' || t.type === 'BORROW_LEND') {
        if (t.direction === 'RECEIVED_FROM_THEM' || t.direction === 'BORROWED') {
          balance += amt;
        } else {
          balance -= amt;
        }
      } else if (t.type === 'WALLET_TRANSFER') {
        balance -= amt;
      }
    }
    // As destination
    if (t.destination_custom_wallet_id === walletId) {
      balance += amt;
    }
  }
  return Math.round(balance * 100) / 100;
}

/**
 * Calculates current month's credit card usage / spend.
 * Resets to 0.00 at the start of each month regardless of bill repayments,
 * allowing the user to view exactly how much amount was used from this credit card in the current month.
 */
export function calculateCreditCardSpend(
  cardId: number,
  transactions: Transaction[],
  monthOnly: boolean = true
): number {
  const now = new Date();
  const currentMonthStart = new Date(now.getFullYear(), now.getMonth(), 1).getTime();

  let charges = 0.0;

  for (const t of transactions) {
    const txDate = typeof t.date === 'number' ? t.date : new Date(t.date).getTime();
    if (monthOnly && txDate < currentMonthStart) {
      continue;
    }

    const amt = Math.abs(t.amount || 0);
    if (t.credit_card_id === cardId && t.source_wallet === 'CREDIT_CARD') {
      if (
        t.type === 'PERSONAL_EXPENSE' ||
        (t.type === 'FAMILY_TRANSFER' && t.direction === 'GIVEN_TO_THEM') ||
        (t.type === 'BORROW_LEND' && t.direction === 'LENT')
      ) {
        charges += amt;
      }
    }
  }

  return charges;
}

export function calculateCreditCardAllTimeSpend(
  cardId: number,
  transactions: Transaction[]
): number {
  return calculateCreditCardSpend(cardId, transactions, false);
}

export function calculate90DayOutflows(transactions: Transaction[], now: number = Date.now()): {
  totalSpending90Days: number;
  totalCreditCardSpending: number;
  monthlyProjection: number;
} {
  const ninetyDaysAgo = now - 90 * 24 * 60 * 60 * 1000;

  const expenseOutflows = transactions.filter((t) => {
    return (
      t.date >= ninetyDaysAgo &&
      ((t.type === 'PERSONAL_EXPENSE' &&
        (t.source_wallet === 'EXPENSE_WALLET' || t.source_wallet === 'CREDIT_CARD')) ||
        (t.type === 'FAMILY_TRANSFER' &&
          t.source_wallet === 'EXPENSE_WALLET' &&
          t.direction === 'GIVEN_TO_THEM'))
    );
  });

  const creditCardOutflows = transactions.filter((t) => {
    return t.date >= ninetyDaysAgo && t.source_wallet === 'CREDIT_CARD';
  });

  const totalSpending90Days = expenseOutflows.reduce((sum, t) => sum + Math.abs(t.amount || 0), 0);
  const totalCreditCardSpending = creditCardOutflows.reduce((sum, t) => sum + Math.abs(t.amount || 0), 0);
  const monthlyProjection = totalSpending90Days / 3.0;

  return {
    totalSpending90Days,
    totalCreditCardSpending,
    monthlyProjection,
  };
}

export function calculateStreak(transactions: Transaction[]): number {
  if (!transactions.length) return 0;

  // Convert to local dates YYYY-MM-DD
  const loggedDays = new Set<string>();
  for (const tx of transactions) {
    const d = new Date(tx.date);
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    loggedDays.add(key);
  }

  const now = new Date();
  const formatKey = (d: Date) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

  const todayKey = formatKey(now);
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  const yesterdayKey = formatKey(yesterday);

  let checkDate = new Date(now);

  if (loggedDays.has(todayKey)) {
    // start from today
  } else if (loggedDays.has(yesterdayKey)) {
    checkDate = yesterday;
  } else {
    return 0;
  }

  let streak = 0;
  while (true) {
    const key = formatKey(checkDate);
    if (!loggedDays.has(key)) break;
    streak++;
    checkDate.setDate(checkDate.getDate() - 1);
  }

  return streak;
}

export function formatCurrency(amount: number, currencySymbol: string = '₹'): string {
  const isNegative = amount < 0;
  const abs = Math.abs(amount);
  const formatted = abs.toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  return `${isNegative ? '-' : ''}${currencySymbol}${formatted}`;
}

/**
 * Formats epoch timestamp or Date to Indian date format DD/MM/YYYY (e.g. 08/10/2026)
 */
export function formatIndianDate(date: number | string | Date): string {
  if (!date) return '';
  const d = new Date(date);
  if (isNaN(d.getTime())) return '';
  return d.toLocaleDateString('en-IN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

/**
 * Formats epoch timestamp or Date to 12-hour Indian time format (e.g. 01:15 PM)
 */
export function formatIndianTime(date: number | string | Date): string {
  if (!date) return '';
  const d = new Date(date);
  if (isNaN(d.getTime())) return '';
  return d.toLocaleTimeString('en-IN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
  });
}

/**
 * Formats epoch timestamp or Date to Indian date & time (e.g. 08/10/2026, 01:15 PM)
 */
export function formatIndianDateTime(date: number | string | Date): string {
  if (!date) return '';
  const d = new Date(date);
  if (isNaN(d.getTime())) return '';
  return `${formatIndianDate(d)}, ${formatIndianTime(d)}`;
}

