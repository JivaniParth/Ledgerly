export type TransactionType =
  | 'PERSONAL_EXPENSE'
  | 'PERSONAL_INCOME'
  | 'FAMILY_TRANSFER'
  | 'WALLET_TRANSFER'
  | 'BORROW_LEND';

export type WalletType =
  | 'EXPENSE_WALLET'
  | 'SAVINGS_WALLET'
  | 'CREDIT_CARD'
  | 'TEMPORARY_WALLET';

export type TransactionDirection =
  | 'RECEIVED_FROM_THEM'
  | 'GIVEN_TO_THEM'
  | 'BORROWED'
  | 'LENT';

export interface Transaction {
  id: number;
  amount: number;
  date: number; // Unix epoch in ms
  notes: string;
  type: TransactionType;
  source_wallet: WalletType;
  destination_wallet?: WalletType | null;
  family_member_id?: number | null;
  direction?: TransactionDirection | null;
  category: string;
  credit_card_id?: number | null;
  recurring_transaction_id?: number | null;
  custom_wallet_id?: number | null;
  destination_custom_wallet_id?: number | null;
  contact_id?: number | null;
}

export interface FamilyMember {
  id: number;
  name: string;
  relationship: string;
}

export interface CreditCard {
  id: number;
  card_name: string;
  owner_name: string;
  credit_limit: number;
  color: string;
}

export interface CustomWallet {
  id: number;
  name: string;
  color: string;
}

export interface SavingsGoal {
  id: number;
  name: string;
  target_amount: number;
  current_amount: number;
  category: string;
}

export interface BudgetGoal {
  category: string;
  limit_amount: number;
}

export interface RecurringTransaction {
  id: number;
  amount: number;
  notes: string;
  type: TransactionType;
  source_wallet: WalletType;
  category: string;
  interval_type: 'Daily' | 'Weekly' | 'Monthly' | 'Yearly';
  last_applied_date: number;
  next_due_date: number;
  is_active: boolean;
}

export interface Category {
  name: string;
  type: 'expense' | 'income';
}

export interface Contact {
  id: number;
  name: string;
}

export interface TransactionSplit {
  id: number;
  transaction_id: number;
  amount: number;
  contact_id?: number | null;
  family_member_id?: number | null;
  is_settled: boolean;
}

// Ledger entity combining Contacts and Family members for the debt ledger
export interface LedgerEntity {
  id: number;
  name: string;
  isFamilyMember: boolean;
  relationship?: string;
  balance: number; // Positive: they owe you, Negative: you owe them
  memberObj?: FamilyMember;
  contactObj?: Contact;
}
