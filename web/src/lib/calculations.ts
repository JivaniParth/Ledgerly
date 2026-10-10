import { Transaction, TransactionSplit } from './types';

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
  return balance;
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
  return balance;
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
  return balance;
}

export function calculateCreditCardSpend(cardId: number, transactions: Transaction[]): number {
  let charges = 0.0;
  let repayments = 0.0;

  for (const t of transactions) {
    const amt = Math.abs(t.amount || 0);
    if (t.credit_card_id === cardId) {
      if (t.source_wallet === 'CREDIT_CARD') {
        if (t.type === 'PERSONAL_EXPENSE') {
          charges += amt;
        } else if (t.type === 'FAMILY_TRANSFER' || t.type === 'BORROW_LEND') {
          if (t.direction === 'GIVEN_TO_THEM' || t.direction === 'LENT') {
            charges += amt;
          }
        }
      }
      if (t.type === 'WALLET_TRANSFER' && t.destination_wallet === 'CREDIT_CARD') {
        repayments += amt;
      }
    }
  }
  return Math.max(0, charges - repayments);
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

  let startKey = '';
  let checkDate = new Date(now);

  if (loggedDays.has(todayKey)) {
    startKey = todayKey;
  } else if (loggedDays.has(yesterdayKey)) {
    startKey = yesterdayKey;
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
