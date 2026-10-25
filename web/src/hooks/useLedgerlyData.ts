import { useState, useEffect, useCallback, useMemo } from 'react';
import { supabase } from '../lib/supabase';
import {
  Transaction,
  FamilyMember,
  CreditCard,
  CustomWallet,
  SavingsGoal,
  BudgetGoal,
  RecurringTransaction,
  Category,
  Contact,
  TransactionSplit,
  LedgerEntity,
  WalletType,
  TransactionDirection,
} from '../lib/types';
import {
  calculateExpenseBalance,
  calculateSavingsBalance,
  calculate90DayOutflows,
  calculateStreak,
} from '../lib/calculations';
import { useAuth } from '../context/AuthContext';

export function useLedgerlyData() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Core Data States
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [familyMembers, setFamilyMembers] = useState<FamilyMember[]>([]);
  const [creditCards, setCreditCards] = useState<CreditCard[]>([]);
  const [customWallets, setCustomWallets] = useState<CustomWallet[]>([]);
  const [savingsGoals, setSavingsGoals] = useState<SavingsGoal[]>([]);
  const [budgetGoals, setBudgetGoals] = useState<BudgetGoal[]>([]);
  const [recurringTransactions, setRecurringTransactions] = useState<RecurringTransaction[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [splits, setSplits] = useState<TransactionSplit[]>([]);

  // Default Categories if remote is empty
  const defaultExpenseCategories = ['Groceries', 'Entertainment', 'Bills', 'Travel', 'Medical', 'Shopping', 'Dining', 'Other'];
  const defaultIncomeCategories = ['Salary', 'Business', 'Investment', 'Freelance', 'Gift', 'Other'];

  const expenseCategories = useMemo(() => {
    const list = categories.filter((c) => c.type === 'expense').map((c) => c.name);
    return list.length ? list : defaultExpenseCategories;
  }, [categories]);

  const incomeCategories = useMemo(() => {
    const list = categories.filter((c) => c.type === 'income').map((c) => c.name);
    return list.length ? list : defaultIncomeCategories;
  }, [categories]);

  // Fetch all data from Supabase
  const fetchData = useCallback(async () => {
    if (!user) {
      setLoading(false);
      return;
    }
    setSyncing(true);
    setError(null);

    try {
      // 1. Transactions
      const { data: txsData, error: txsErr } = await supabase
        .from('transactions')
        .select('*')
        .order('date', { ascending: false });
      if (txsErr) throw txsErr;

      // 2. Family Members
      const { data: famData, error: famErr } = await supabase.from('family_members').select('*');
      if (famErr) throw famErr;

      // 3. Credit Cards
      const { data: ccData, error: ccErr } = await supabase.from('credit_cards').select('*');
      if (ccErr) throw ccErr;

      // 4. Custom Wallets
      const { data: cwData, error: cwErr } = await supabase.from('custom_wallets').select('*');
      if (cwErr) throw cwErr;

      // 5. Savings Goals
      const { data: sgData, error: sgErr } = await supabase.from('savings_goals').select('*');
      if (sgErr) throw sgErr;

      // 6. Budget Goals
      const { data: bgData, error: bgErr } = await supabase.from('budget_goals').select('*');
      if (bgErr) throw bgErr;

      // 7. Recurring Transactions
      const { data: recData, error: recErr } = await supabase.from('recurring_transactions').select('*');
      if (recErr) throw recErr;

      // 8. Categories
      const { data: catData, error: catErr } = await supabase.from('categories').select('*');
      if (catErr) throw catErr;

      // 9. Contacts (if table exists)
      let contactsList: Contact[] = [];
      try {
        const { data: conData } = await supabase.from('contacts').select('*');
        if (conData) contactsList = conData;
      } catch {
        // Table might not exist yet in Postgres
      }

      // 10. Splits (if table exists)
      let splitsList: TransactionSplit[] = [];
      try {
        const { data: splData } = await supabase.from('transaction_splits').select('*');
        if (splData) splitsList = splData;
      } catch {
        // Table might not exist yet in Postgres
      }

      setTransactions((txsData as Transaction[]) || []);
      setFamilyMembers((famData as FamilyMember[]) || []);
      setCreditCards((ccData as CreditCard[]) || []);
      setCustomWallets((cwData as CustomWallet[]) || []);
      setSavingsGoals((sgData as SavingsGoal[]) || []);
      setBudgetGoals((bgData as BudgetGoal[]) || []);
      setRecurringTransactions((recData as RecurringTransaction[]) || []);
      setCategories((catData as Category[]) || []);
      setContacts(contactsList);
      setSplits(splitsList);
    } catch (err: any) {
      console.error('Error fetching Supabase data:', err);
      setError(err.message || 'Failed to sync with Supabase');
    } finally {
      setLoading(false);
      setSyncing(false);
    }
  }, [user]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Real-time calculated balances & metrics
  const expenseBalance = useMemo(() => calculateExpenseBalance(transactions), [transactions]);
  const savingsBalance = useMemo(() => calculateSavingsBalance(transactions), [transactions]);
  const streak = useMemo(() => calculateStreak(transactions), [transactions]);
  const { totalSpending90Days, totalCreditCardSpending, monthlyProjection } = useMemo(
    () => calculate90DayOutflows(transactions),
    [transactions]
  );

  // Ledger Entities (Contacts & Family Members with net debts/credits)
  const ledgerEntities = useMemo<LedgerEntity[]>(() => {
    const list: LedgerEntity[] = [];

    // Add Family Members ONLY if they have borrow/lend transactions or unsettled splits
    for (const fm of familyMembers) {
      const borrowLendTxs = transactions.filter(
        (t) => t.family_member_id === fm.id && t.type === 'BORROW_LEND'
      );
      const memberSplits = splits.filter(
        (s) => s.family_member_id === fm.id && !s.is_settled
      );

      // Only include family members in the Borrow & Lend Ledger if they have borrow/lend transactions or splits
      if (borrowLendTxs.length === 0 && memberSplits.length === 0) {
        continue;
      }

      let balance = 0;
      for (const t of borrowLendTxs) {
        if (t.direction === 'GIVEN_TO_THEM' || t.direction === 'LENT') {
          balance += t.amount; // They owe you
        } else if (t.direction === 'RECEIVED_FROM_THEM' || t.direction === 'BORROWED') {
          balance -= t.amount; // You owe them
        }
      }
      for (const s of memberSplits) {
        balance += s.amount;
      }

      list.push({
        id: fm.id,
        name: fm.name,
        isFamilyMember: true,
        relationship: fm.relationship,
        balance,
        memberObj: fm,
      });
    }

    // Add Contacts
    for (const c of contacts) {
      const contactTxs = transactions.filter(
        (t) => t.contact_id === c.id && t.type === 'BORROW_LEND'
      );
      let balance = 0;
      for (const t of contactTxs) {
        if (t.direction === 'GIVEN_TO_THEM' || t.direction === 'LENT') {
          balance += t.amount;
        } else if (t.direction === 'RECEIVED_FROM_THEM' || t.direction === 'BORROWED') {
          balance -= t.amount;
        }
      }
      // Add unsettled splits
      const contactSplits = splits.filter((s) => s.contact_id === c.id && !s.is_settled);
      for (const s of contactSplits) {
        balance += s.amount;
      }

      list.push({
        id: c.id,
        name: c.name,
        isFamilyMember: false,
        balance,
        contactObj: c,
      });
    }

    return list.sort((a, b) => {
      const diff = Math.abs(b.balance) - Math.abs(a.balance);
      if (diff !== 0) return diff;
      return a.name.localeCompare(b.name);
    });
  }, [familyMembers, contacts, transactions, splits]);

  // ── CRUD OPERATIONS ───────────────────────────────────────────────

  // 1. Transactions
  const addTransaction = async (
    txData: Omit<Transaction, 'id'>,
    splitsData?: Omit<TransactionSplit, 'id' | 'transaction_id'>[]
  ) => {
    const id = Date.now() * 1000 + Math.floor(Math.random() * 1000);
    const newTx = { ...txData, id };
    
    // Optimistic update
    setTransactions((prev) => [newTx, ...prev]);

    const { error: insertErr } = await supabase.from('transactions').insert([newTx]);
    if (insertErr) {
      console.error('Insert transaction error:', insertErr);
      fetchData();
      return { success: false, error: insertErr.message };
    }

    if (splitsData && splitsData.length > 0) {
      const formattedSplits = splitsData.map((s) => ({
        ...s,
        transaction_id: id,
      }));
      setSplits((prev) => [...prev, ...(formattedSplits as TransactionSplit[])]);
      try {
        await supabase.from('transaction_splits').insert(formattedSplits);
      } catch (e) {
        console.warn('Splits insert skipped (table may be local-only):', e);
      }
    }

    return { success: true };
  };

  const updateTransaction = async (tx: Transaction) => {
    setTransactions((prev) => prev.map((t) => (t.id === tx.id ? tx : t)));
    const { error } = await supabase.from('transactions').update(tx).eq('id', tx.id);
    if (error) {
      console.error('Update transaction error:', error);
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const deleteTransaction = async (id: number) => {
    setTransactions((prev) => prev.filter((t) => t.id !== id));
    const { error } = await supabase.from('transactions').delete().eq('id', id);
    if (error) {
      console.error('Delete transaction error:', error);
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const batchInsertTransactions = async (txs: Omit<Transaction, 'id'>[]) => {
    let startId = Date.now();
    const fullTxs: Transaction[] = txs.map((t, idx) => ({
      ...t,
      id: startId + idx,
    }));

    setTransactions((prev) => [...fullTxs, ...prev]);

    // Batch in chunks of 500
    const chunkSize = 500;
    for (let i = 0; i < fullTxs.length; i += chunkSize) {
      const chunk = fullTxs.slice(i, i + chunkSize);
      const { error } = await supabase.from('transactions').upsert(chunk);
      if (error) {
        console.error('Batch insert error:', error);
        fetchData();
        return { success: false, error: error.message };
      }
    }
    return { success: true, count: fullTxs.length };
  };

  const splitTransactionRetroactively = async (
    transactionId: number,
    newSplits: Omit<TransactionSplit, 'id' | 'transaction_id'>[]
  ) => {
    const formatted = newSplits.map((s, idx) => ({
      ...s,
      id: Date.now() * 1000 + Math.floor(Math.random() * 1000) + idx,
      transaction_id: transactionId,
    }));
    setSplits((prev) => [...prev, ...(formatted as TransactionSplit[])]);
    try {
      await supabase.from('transaction_splits').insert(formatted);
    } catch (e) {
      console.warn('Splits table update:', e);
    }
    return { success: true };
  };

  // 2. Family Members
  const addFamilyMember = async (name: string, relationship: string) => {
    const newMember: FamilyMember = { id: Date.now() * 1000 + Math.floor(Math.random() * 1000), name, relationship };
    setFamilyMembers((prev) => [...prev, newMember]);
    const { error } = await supabase.from('family_members').insert([newMember]);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const updateFamilyMember = async (member: FamilyMember) => {
    setFamilyMembers((prev) => prev.map((m) => (m.id === member.id ? member : m)));
    const { error } = await supabase.from('family_members').update(member).eq('id', member.id);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const deleteFamilyMember = async (id: number) => {
    setFamilyMembers((prev) => prev.filter((m) => m.id !== id));
    const { error } = await supabase.from('family_members').delete().eq('id', id);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  // 3. Credit Cards
  const addCreditCard = async (card: Omit<CreditCard, 'id'>) => {
    const newCard: CreditCard = { ...card, id: Date.now() * 1000 + Math.floor(Math.random() * 1000) };
    setCreditCards((prev) => [...prev, newCard]);
    const { error } = await supabase.from('credit_cards').insert([newCard]);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const updateCreditCard = async (card: CreditCard) => {
    setCreditCards((prev) => prev.map((c) => (c.id === card.id ? card : c)));
    const { error } = await supabase.from('credit_cards').update(card).eq('id', card.id);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const deleteCreditCard = async (id: number) => {
    setCreditCards((prev) => prev.filter((c) => c.id !== id));
    const { error } = await supabase.from('credit_cards').delete().eq('id', id);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  // 4. Custom Wallets
  const addCustomWallet = async (wallet: Omit<CustomWallet, 'id'>) => {
    const newWallet: CustomWallet = { ...wallet, id: Date.now() * 1000 + Math.floor(Math.random() * 1000) };
    setCustomWallets((prev) => [...prev, newWallet]);
    const { error } = await supabase.from('custom_wallets').insert([newWallet]);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const updateCustomWallet = async (wallet: CustomWallet) => {
    setCustomWallets((prev) => prev.map((w) => (w.id === wallet.id ? wallet : w)));
    const { error } = await supabase.from('custom_wallets').update(wallet).eq('id', wallet.id);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const deleteCustomWallet = async (id: number) => {
    setCustomWallets((prev) => prev.filter((w) => w.id !== id));
    const { error } = await supabase.from('custom_wallets').delete().eq('id', id);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  // 5. Savings Goals
  const addSavingsGoal = async (goal: Omit<SavingsGoal, 'id'>) => {
    const newGoal: SavingsGoal = { ...goal, id: Date.now() * 1000 + Math.floor(Math.random() * 1000) };
    setSavingsGoals((prev) => [...prev, newGoal]);
    const { error } = await supabase.from('savings_goals').insert([newGoal]);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const updateSavingsGoal = async (goal: SavingsGoal) => {
    const oldGoal = savingsGoals.find((g) => g.id === goal.id);
    setSavingsGoals((prev) => prev.map((g) => (g.id === goal.id ? goal : g)));
    const { error } = await supabase.from('savings_goals').update(goal).eq('id', goal.id);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }

    if (oldGoal && oldGoal.current_amount !== goal.current_amount) {
      const diff = Math.round((goal.current_amount - oldGoal.current_amount) * 100) / 100;
      if (diff > 0) {
        await addTransaction({
          amount: diff,
          date: Date.now(),
          notes: `Savings Goal Deposit: ${goal.name}`,
          type: 'WALLET_TRANSFER',
          source_wallet: 'EXPENSE_WALLET',
          destination_wallet: 'SAVINGS_WALLET',
          category: 'Savings',
        });
      } else if (diff < 0) {
        await addTransaction({
          amount: Math.abs(diff),
          date: Date.now(),
          notes: `Savings Goal Withdrawal: ${goal.name}`,
          type: 'WALLET_TRANSFER',
          source_wallet: 'SAVINGS_WALLET',
          destination_wallet: 'EXPENSE_WALLET',
          category: 'Savings',
        });
      }
    }
    return { success: true };
  };

  const deleteSavingsGoal = async (id: number) => {
    setSavingsGoals((prev) => prev.filter((g) => g.id !== id));
    const { error } = await supabase.from('savings_goals').delete().eq('id', id);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  // 6. Budget Goals
  const addBudgetGoal = async (category: string, limit_amount: number) => {
    const newGoal: BudgetGoal = { category, limit_amount };
    setBudgetGoals((prev) => [...prev.filter((g) => g.category !== category), newGoal]);
    const { error } = await supabase.from('budget_goals').upsert([newGoal]);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const deleteBudgetGoal = async (category: string) => {
    setBudgetGoals((prev) => prev.filter((g) => g.category !== category));
    const { error } = await supabase.from('budget_goals').delete().eq('category', category);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  // 7. Recurring Transactions
  const addRecurring = async (rec: Omit<RecurringTransaction, 'id'>) => {
    const newRec: RecurringTransaction = { ...rec, id: Date.now() * 1000 + Math.floor(Math.random() * 1000) };
    setRecurringTransactions((prev) => [...prev, newRec]);
    const { error } = await supabase.from('recurring_transactions').insert([newRec]);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const updateRecurring = async (rec: RecurringTransaction) => {
    setRecurringTransactions((prev) => prev.map((r) => (r.id === rec.id ? rec : r)));
    const { error } = await supabase.from('recurring_transactions').update(rec).eq('id', rec.id);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const deleteRecurring = async (id: number) => {
    setRecurringTransactions((prev) => prev.filter((r) => r.id !== id));
    const { error } = await supabase.from('recurring_transactions').delete().eq('id', id);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  // 8. Categories
  const addCategory = async (name: string, type: 'expense' | 'income') => {
    const newCat: Category = { name, type };
    setCategories((prev) => [...prev, newCat]);
    const { error } = await supabase.from('categories').insert([newCat]);
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  const deleteCategory = async (name: string, type: 'expense' | 'income') => {
    setCategories((prev) => prev.filter((c) => !(c.name === name && c.type === type)));
    const { error } = await supabase.from('categories').delete().match({ name, type });
    if (error) {
      fetchData();
      return { success: false, error: error.message };
    }
    return { success: true };
  };

  // 9. Contacts
  const addContact = async (name: string) => {
    const newContact: Contact = { id: Date.now() * 1000 + Math.floor(Math.random() * 1000), name };
    setContacts((prev) => [...prev, newContact]);
    try {
      await supabase.from('contacts').insert([newContact]);
    } catch (e) {
      console.warn('Contact insert (table might be local):', e);
    }
    return { success: true };
  };

  const updateContact = async (contact: Contact) => {
    setContacts((prev) => prev.map((c) => (c.id === contact.id ? contact : c)));
    try {
      await supabase.from('contacts').update(contact).eq('id', contact.id);
    } catch (e) {
      console.warn('Contact update:', e);
    }
    return { success: true };
  };

  const deleteContact = async (id: number) => {
    setContacts((prev) => prev.filter((c) => c.id !== id));
    try {
      await supabase.from('contacts').delete().eq('id', id);
    } catch (e) {
      console.warn('Contact delete:', e);
    }
    return { success: true };
  };

  // 10. Settle Up (Ledger Debts)
  const settleUpEntity = async (
    entity: LedgerEntity,
    walletToReceive: WalletType = 'EXPENSE_WALLET',
    customWalletId: number | null = null
  ) => {
    const amt = Math.abs(entity.balance);
    if (amt <= 0) return { success: true };

    // Record a reconciling transaction:
    // If they owed you (balance > 0) -> we received repayment (BORROWED)
    // If you owed them (balance < 0) -> we gave repayment (LENT)
    const direction: TransactionDirection = entity.balance > 0 ? 'BORROWED' : 'LENT';

    const tx: Omit<Transaction, 'id'> = {
      amount: amt,
      date: Date.now(),
      notes: `Settled debt with ${entity.name}`,
      type: 'BORROW_LEND',
      source_wallet: walletToReceive,
      direction,
      category: 'Other',
      family_member_id: entity.isFamilyMember ? entity.id : null,
      contact_id: !entity.isFamilyMember ? entity.id : null,
      custom_wallet_id: customWalletId,
    };

    // Mark relevant splits as settled
    setSplits((prev) =>
      prev.map((s) => {
        if (entity.isFamilyMember && s.family_member_id === entity.id) {
          return { ...s, is_settled: true };
        }
        if (!entity.isFamilyMember && s.contact_id === entity.id) {
          return { ...s, is_settled: true };
        }
        return s;
      })
    );

    return await addTransaction(tx);
  };

  // 11. Wipe / Clear All Data (Client state only)
  const clearAllData = async () => {
    setTransactions([]);
    setSplits([]);
    setFamilyMembers([]);
    setCreditCards([]);
    setCustomWallets([]);
    setSavingsGoals([]);
    setBudgetGoals([]);
    setRecurringTransactions([]);
    setContacts([]);
    return { success: true };
  };

  return {
    loading,
    syncing,
    error,
    refreshData: fetchData,

    // Data lists
    transactions,
    familyMembers,
    creditCards,
    customWallets,
    savingsGoals,
    budgetGoals,
    recurringTransactions,
    categories,
    expenseCategories,
    incomeCategories,
    contacts,
    splits,
    ledgerEntities,

    // Computed metrics
    expenseBalance,
    savingsBalance,
    streak,
    totalSpending90Days,
    totalCreditCardSpending,
    monthlyProjection,

    // CRUD
    addTransaction,
    updateTransaction,
    deleteTransaction,
    batchInsertTransactions,
    splitTransactionRetroactively,
    addFamilyMember,
    updateFamilyMember,
    deleteFamilyMember,
    addCreditCard,
    updateCreditCard,
    deleteCreditCard,
    addCustomWallet,
    updateCustomWallet,
    deleteCustomWallet,
    addSavingsGoal,
    updateSavingsGoal,
    deleteSavingsGoal,
    addBudgetGoal,
    deleteBudgetGoal,
    addRecurring,
    updateRecurring,
    deleteRecurring,
    addCategory,
    deleteCategory,
    addContact,
    updateContact,
    deleteContact,
    settleUpEntity,
    clearAllData,
  };
}
