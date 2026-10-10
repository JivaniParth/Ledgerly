import React, { useState } from 'react';
import {
  Tag,
  Target,
  PiggyBank,
  Repeat,
  Palette,
  Cloud,
  Plus,
  Trash2,
  Edit2,
  Check,
  RefreshCw,
} from 'lucide-react';
import {
  BudgetGoal,
  SavingsGoal,
  RecurringTransaction,
  WalletType,
  TransactionType,
} from '../../lib/types';
import { formatCurrency } from '../../lib/calculations';
import { useTheme } from '../../context/ThemeContext';

interface SettingsViewProps {
  expenseCategories: string[];
  incomeCategories: string[];
  budgetGoals: BudgetGoal[];
  savingsGoals: SavingsGoal[];
  recurringTransactions: RecurringTransaction[];
  onAddCategory: (name: string, type: 'expense' | 'income') => Promise<any>;
  onDeleteCategory: (name: string, type: 'expense' | 'income') => Promise<any>;
  onAddBudgetGoal: (category: string, limit: number) => Promise<any>;
  onDeleteBudgetGoal: (category: string) => Promise<any>;
  onAddSavingsGoal: (goal: Omit<SavingsGoal, 'id'>) => Promise<any>;
  onUpdateSavingsGoal: (goal: SavingsGoal) => Promise<any>;
  onDeleteSavingsGoal: (id: number) => Promise<any>;
  onAddRecurring: (rec: Omit<RecurringTransaction, 'id'>) => Promise<any>;
  onUpdateRecurring: (rec: RecurringTransaction) => Promise<any>;
  onDeleteRecurring: (id: number) => Promise<any>;
  onRefreshData: () => Promise<any>;
  syncing: boolean;
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  expenseCategories,
  incomeCategories,
  budgetGoals,
  savingsGoals,
  recurringTransactions,
  onAddCategory,
  onDeleteCategory,
  onAddBudgetGoal,
  onDeleteBudgetGoal,
  onAddSavingsGoal,
  onUpdateSavingsGoal,
  onDeleteSavingsGoal,
  onAddRecurring,
  onUpdateRecurring,
  onDeleteRecurring,
  onRefreshData,
  syncing,
}) => {
  const {
    theme,
    setTheme,
    expenseWalletColor,
    setExpenseWalletColor,
    creditCardColor,
    setCreditCardColor,
  } = useTheme();

  // Category State
  const [newExpenseCat, setNewExpenseCat] = useState('');
  const [newIncomeCat, setNewIncomeCat] = useState('');

  // Budget Goal State
  const [budgetCat, setBudgetCat] = useState(expenseCategories[0] || 'Groceries');
  const [budgetLimit, setBudgetLimit] = useState('');

  // Savings Goal State
  const [showAddSavings, setShowAddSavings] = useState(false);
  const [editingSavings, setEditingSavings] = useState<SavingsGoal | null>(null);
  const [savingName, setSavingName] = useState('');
  const [savingTarget, setSavingTarget] = useState('');
  const [savingCurrent, setSavingCurrent] = useState('');
  const [savingCategory, setSavingCategory] = useState('Overall');

  // Recurring Transaction State
  const [showAddRecurring, setShowAddRecurring] = useState(false);
  const [recAmount, setRecAmount] = useState('');
  const [recNotes, setRecNotes] = useState('');
  const [recCategory, setRecCategory] = useState(expenseCategories[0] || 'Bills');
  const [recType, setRecType] = useState<TransactionType>('PERSONAL_EXPENSE');
  const [recWallet, setRecWallet] = useState<WalletType>('EXPENSE_WALLET');
  const [recInterval, setRecInterval] = useState<'Daily' | 'Weekly' | 'Monthly' | 'Yearly'>('Monthly');

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-gray-900 dark:text-white">Settings & Customization</h2>
        <p className="text-xs text-gray-500 dark:text-darkMuted mt-0.5">
          Configure categories, monthly budget caps, savings targets, subscriptions, and themes.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* ── Category Manager ───────────────────────────────────────── */}
        <div className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-4">
          <div className="flex items-center gap-2">
            <Tag size={18} className="text-finance-blue" />
            <h3 className="font-bold text-sm text-gray-900 dark:text-white">Category Management</h3>
          </div>

          {/* Expense Categories */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-gray-700 dark:text-gray-300">
              Expense Categories
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="New expense category..."
                value={newExpenseCat}
                onChange={(e) => setNewExpenseCat(e.target.value)}
                className="flex-1 px-3 py-1.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-medium"
              />
              <button
                onClick={async () => {
                  if (!newExpenseCat.trim()) return;
                  await onAddCategory(newExpenseCat.trim(), 'expense');
                  setNewExpenseCat('');
                }}
                className="px-3 py-1.5 rounded-xl bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark"
              >
                Add
              </button>
            </div>
            <div className="flex flex-wrap gap-1.5 pt-1">
              {expenseCategories.map((cat) => (
                <span
                  key={cat}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-gray-100 dark:bg-darkCard text-xs font-medium text-gray-800 dark:text-gray-200"
                >
                  <span>{cat}</span>
                  <button
                    onClick={() => onDeleteCategory(cat, 'expense')}
                    className="text-gray-400 hover:text-red-500"
                  >
                    ×
                  </button>
                </span>
              ))}
            </div>
          </div>

          {/* Income Categories */}
          <div className="space-y-2 pt-2 border-t border-lightBorder dark:border-darkBorder/60">
            <label className="text-xs font-semibold text-gray-700 dark:text-gray-300">
              Income Categories
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="New income category..."
                value={newIncomeCat}
                onChange={(e) => setNewIncomeCat(e.target.value)}
                className="flex-1 px-3 py-1.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-medium"
              />
              <button
                onClick={async () => {
                  if (!newIncomeCat.trim()) return;
                  await onAddCategory(newIncomeCat.trim(), 'income');
                  setNewIncomeCat('');
                }}
                className="px-3 py-1.5 rounded-xl bg-finance-green text-white text-xs font-bold shadow hover:bg-finance-greenDark"
              >
                Add
              </button>
            </div>
            <div className="flex flex-wrap gap-1.5 pt-1">
              {incomeCategories.map((cat) => (
                <span
                  key={cat}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 text-xs font-medium text-finance-green"
                >
                  <span>{cat}</span>
                  <button
                    onClick={() => onDeleteCategory(cat, 'income')}
                    className="text-gray-400 hover:text-red-500"
                  >
                    ×
                  </button>
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* ── Monthly Budget Goals Manager ───────────────────────────── */}
        <div className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-4">
          <div className="flex items-center gap-2">
            <Target size={18} className="text-finance-gold" />
            <h3 className="font-bold text-sm text-gray-900 dark:text-white">
              Category Budget Caps
            </h3>
          </div>

          <div className="flex gap-2">
            <select
              value={budgetCat}
              onChange={(e) => setBudgetCat(e.target.value)}
              className="flex-1 px-3 py-1.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-medium"
            >
              {expenseCategories.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
            <input
              type="number"
              placeholder="Limit (₹)"
              value={budgetLimit}
              onChange={(e) => setBudgetLimit(e.target.value)}
              className="w-28 px-3 py-1.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
            />
            <button
              onClick={async () => {
                const lim = parseFloat(budgetLimit);
                if (isNaN(lim) || lim <= 0) return;
                await onAddBudgetGoal(budgetCat, lim);
                setBudgetLimit('');
              }}
              className="px-3.5 py-1.5 rounded-xl bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark"
            >
              Set Cap
            </button>
          </div>

          <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
            {budgetGoals.map((bg) => (
              <div
                key={bg.category}
                className="flex items-center justify-between p-2.5 rounded-xl bg-gray-50 dark:bg-darkCard border border-lightBorder dark:border-darkBorder text-xs"
              >
                <div>
                  <span className="font-semibold text-gray-800 dark:text-gray-200">
                    {bg.category}
                  </span>
                  <span className="block text-[11px] text-gray-500 dark:text-darkMuted">
                    Monthly Cap: {formatCurrency(bg.limit_amount)}
                  </span>
                </div>
                <button
                  onClick={() => onDeleteBudgetGoal(bg.category)}
                  className="p-1 rounded text-gray-400 hover:text-red-500"
                >
                  <Trash2 size={14} />
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* ── Recurring Subscriptions Scheduler ─────────────────────── */}
        <div className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Repeat size={18} className="text-finance-teal" />
              <h3 className="font-bold text-sm text-gray-900 dark:text-white">
                Recurring Subscriptions & Bills
              </h3>
            </div>
            <button
              onClick={() => setShowAddRecurring(true)}
              className="px-3 py-1 rounded-xl bg-finance-blue text-white text-xs font-bold shadow"
            >
              + Add Bill
            </button>
          </div>

          <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
            {recurringTransactions.length === 0 ? (
              <p className="text-xs text-gray-500 dark:text-darkMuted py-4 text-center">
                No active recurring bills configured.
              </p>
            ) : (
              recurringTransactions.map((rec) => (
                <div
                  key={rec.id}
                  className="flex items-center justify-between p-2.5 rounded-xl bg-gray-50 dark:bg-darkCard border border-lightBorder dark:border-darkBorder text-xs"
                >
                  <div>
                    <span className="font-semibold text-gray-800 dark:text-gray-200">
                      {rec.notes || rec.category}
                    </span>
                    <span className="block text-[11px] text-gray-500 dark:text-darkMuted">
                      {formatCurrency(rec.amount)} • {rec.interval_type} • Next:{' '}
                      {new Date(rec.next_due_date).toLocaleDateString()}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={rec.is_active}
                      onChange={async (e) => {
                        await onUpdateRecurring({ ...rec, is_active: e.target.checked });
                      }}
                      className="w-4 h-4 rounded text-finance-blue"
                      title="Toggle Active"
                    />
                    <button
                      onClick={() => onDeleteRecurring(rec.id)}
                      className="p-1 text-gray-400 hover:text-red-500"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* ── Theme & Custom Wallet Appearance ───────────────────────── */}
        <div className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-4">
          <div className="flex items-center gap-2">
            <Palette size={18} className="text-purple-500" />
            <h3 className="font-bold text-sm text-gray-900 dark:text-white">
              Theme & Color Personalization
            </h3>
          </div>

          <div className="space-y-3">
            <div>
              <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                Display Theme
              </label>
              <div className="grid grid-cols-3 gap-2">
                {(['dark', 'light', 'system'] as const).map((mode) => (
                  <button
                    key={mode}
                    onClick={() => setTheme(mode)}
                    className={`py-1.5 px-3 rounded-xl text-xs font-semibold border capitalize transition-all ${
                      theme === mode
                        ? 'bg-finance-blue text-white border-finance-blue'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    {mode}
                  </button>
                ))}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-2">
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Expense Wallet Gradient Color
                </label>
                <input
                  type="color"
                  value={expenseWalletColor}
                  onChange={(e) => setExpenseWalletColor(e.target.value)}
                  className="w-full h-10 p-1 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard cursor-pointer"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Default Credit Card Theme Color
                </label>
                <input
                  type="color"
                  value={creditCardColor}
                  onChange={(e) => setCreditCardColor(e.target.value)}
                  className="w-full h-10 p-1 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard cursor-pointer"
                />
              </div>
            </div>
          </div>
        </div>

        {/* ── Supabase Cloud Diagnostic ───────────────────────────────── */}
        <div className="col-span-full p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-3 rounded-xl bg-blue-50 dark:bg-blue-950/40 text-finance-blue">
              <Cloud size={22} />
            </div>
            <div>
              <h4 className="font-bold text-sm text-gray-900 dark:text-white">
                Supabase Real-Time Cloud Sync
              </h4>
              <p className="text-xs text-gray-500 dark:text-darkMuted">
                Direct encrypted connection to PostgreSQL backend. All records are backed up automatically.
              </p>
            </div>
          </div>

          <button
            onClick={onRefreshData}
            disabled={syncing}
            className="px-4 py-2 rounded-xl bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark flex items-center gap-2 whitespace-nowrap disabled:opacity-50"
          >
            <RefreshCw size={14} className={syncing ? 'animate-spin' : ''} />
            <span>{syncing ? 'Syncing...' : 'Sync Cloud Data Now'}</span>
          </button>
        </div>
      </div>

      {/* ── Add Recurring Bill Modal ─────────────────────────────────── */}
      {showAddRecurring && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <h3 className="font-bold text-base text-gray-900 dark:text-white">
              Schedule Recurring Bill
            </h3>
            <div className="space-y-3 text-xs">
              <div>
                <label className="font-semibold block mb-1">Amount (₹)</label>
                <input
                  type="number"
                  placeholder="0.00"
                  value={recAmount}
                  onChange={(e) => setRecAmount(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard font-semibold"
                />
              </div>

              <div>
                <label className="font-semibold block mb-1">Description / Bill Name</label>
                <input
                  type="text"
                  placeholder="e.g. Netflix, Electricity Bill"
                  value={recNotes}
                  onChange={(e) => setRecNotes(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard font-medium"
                />
              </div>

              <div>
                <label className="font-semibold block mb-1">Category</label>
                <select
                  value={recCategory}
                  onChange={(e) => setRecCategory(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard"
                >
                  {expenseCategories.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="font-semibold block mb-1">Frequency Interval</label>
                <select
                  value={recInterval}
                  onChange={(e) => setRecInterval(e.target.value as any)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard"
                >
                  <option value="Daily">Daily</option>
                  <option value="Weekly">Weekly</option>
                  <option value="Monthly">Monthly</option>
                  <option value="Yearly">Yearly</option>
                </select>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setShowAddRecurring(false)}
                className="px-3.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  const amt = parseFloat(recAmount);
                  if (isNaN(amt) || amt <= 0) return;
                  await onAddRecurring({
                    amount: amt,
                    notes: recNotes,
                    category: recCategory,
                    type: recType,
                    source_wallet: recWallet,
                    interval_type: recInterval,
                    last_applied_date: Date.now(),
                    next_due_date: Date.now() + 30 * 24 * 60 * 60 * 1000,
                    is_active: true,
                  });
                  setShowAddRecurring(false);
                  setRecAmount('');
                  setRecNotes('');
                }}
                className="px-4 py-1.5 rounded-lg bg-finance-blue text-white text-xs font-bold shadow"
              >
                Save Schedule
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
