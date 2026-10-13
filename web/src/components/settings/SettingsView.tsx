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
  RefreshCw,
  FileSpreadsheet,
  AlertTriangle,
  LogOut,
  ChevronRight,
  Calendar,
  Wallet,
  TrendingDown,
  TrendingUp,
} from 'lucide-react';
import {
  BudgetGoal,
  SavingsGoal,
  RecurringTransaction,
  WalletType,
  TransactionType,
} from '../../lib/types';
import { formatCurrency, formatIndianDate, formatIndianDateTime } from '../../lib/calculations';
import { AmountInputField } from '../common/AmountInputField';
import { evaluateMathExpression } from '../../lib/amountUtils';
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
  onNavigateToExcel?: () => void;
  onClearAllData?: () => Promise<any>;
  onLogout?: () => void;
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
  onNavigateToExcel,
  onClearAllData,
  onLogout,
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
  const [budgetCat, setBudgetCat] = useState('Overall');
  const [budgetLimit, setBudgetLimit] = useState('');
  const [editingBudgetGoal, setEditingBudgetGoal] = useState<BudgetGoal | null>(null);
  const [editBudgetLimit, setEditBudgetLimit] = useState('');

  // Savings Goal State
  const [showAddSavings, setShowAddSavings] = useState(false);
  const [savingName, setSavingName] = useState('');
  const [savingTarget, setSavingTarget] = useState('');
  const [savingCurrent, setSavingCurrent] = useState('');
  const [savingCategory, setSavingCategory] = useState('Overall');
  const [editingSavings, setEditingSavings] = useState<SavingsGoal | null>(null);
  const [editSavingName, setEditSavingName] = useState('');
  const [editSavingTarget, setEditSavingTarget] = useState('');
  const [editSavingCurrent, setEditSavingCurrent] = useState('');
  const [editSavingCategory, setEditSavingCategory] = useState('Overall');

  // Recurring Transaction State
  const [showAddRecurring, setShowAddRecurring] = useState(false);
  const [recAmount, setRecAmount] = useState('');
  const [recNotes, setRecNotes] = useState('');
  const [recCategory, setRecCategory] = useState(expenseCategories[0] || 'Bills');
  const [recType, setRecType] = useState<TransactionType>('PERSONAL_EXPENSE');
  const [recWallet, setRecWallet] = useState<WalletType>('EXPENSE_WALLET');
  const [recInterval, setRecInterval] = useState<'Daily' | 'Weekly' | 'Monthly' | 'Yearly'>('Monthly');
  const [recNextDueDate, setRecNextDueDate] = useState<string>(
    new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
  );
  const [editingRecurring, setEditingRecurring] = useState<RecurringTransaction | null>(null);
  const [editRecAmount, setEditRecAmount] = useState('');
  const [editRecNotes, setEditRecNotes] = useState('');
  const [editRecInterval, setEditRecInterval] = useState<'Daily' | 'Weekly' | 'Monthly' | 'Yearly'>('Monthly');
  const [editRecNextDue, setEditRecNextDue] = useState('');

  // Data Wipe State
  const [showWipeConfirm, setShowWipeConfirm] = useState(false);
  const [wiping, setWiping] = useState(false);

  // Sorted Budget Goals (Overall first)
  const sortedBudgetGoals = [...budgetGoals].sort((a, b) => {
    if (a.category.toLowerCase() === 'overall') return -1;
    if (b.category.toLowerCase() === 'overall') return 1;
    return a.category.localeCompare(b.category);
  });

  return (
    <div className="space-y-6">
      {/* Title */}
      <div>
        <h2 className="text-xl font-bold text-gray-900 dark:text-white">Settings & Customization</h2>
        <p className="text-xs text-gray-500 dark:text-darkMuted mt-0.5">
          Configure categories, monthly budget caps, savings targets, subscriptions, and themes.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* ── 1. Data Utilities Section ──────────────────────────────── */}
        <div className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-4">
          <div className="flex items-center gap-2">
            <FileSpreadsheet size={18} className="text-finance-blue" />
            <h3 className="font-bold text-sm text-gray-900 dark:text-white">Data Utilities</h3>
          </div>

          <div className="space-y-3">
            {/* Bulk Import from Excel */}
            {onNavigateToExcel && (
              <div
                onClick={onNavigateToExcel}
                className="p-3.5 rounded-xl bg-gray-50 dark:bg-darkCard/60 border border-lightBorder dark:border-darkBorder/60 hover:border-finance-blue dark:hover:border-finance-blue transition-all cursor-pointer flex items-center justify-between"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-xl bg-blue-50 dark:bg-blue-950/40 text-finance-blue">
                    <FileSpreadsheet size={18} />
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-gray-900 dark:text-white">
                      Bulk Import from Excel
                    </h4>
                    <p className="text-[11px] text-gray-500 dark:text-darkMuted">
                      Load income & expense records from an Excel workbook (.xlsx / .csv)
                    </p>
                  </div>
                </div>
                <ChevronRight size={16} className="text-gray-400 shrink-0" />
              </div>
            )}

            {/* Wipe All Local Data */}
            <div
              onClick={() => setShowWipeConfirm(true)}
              className="p-3.5 rounded-xl bg-red-50/50 dark:bg-red-950/20 border border-red-200 dark:border-red-900/40 hover:border-red-500 transition-all cursor-pointer flex items-center justify-between"
            >
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-xl bg-red-100 dark:bg-red-900/40 text-red-600 dark:text-red-400">
                  <Trash2 size={18} />
                </div>
                <div>
                  <h4 className="text-xs font-bold text-red-600 dark:text-red-400">
                    Wipe All Local & Cloud Data
                  </h4>
                  <p className="text-[11px] text-gray-500 dark:text-darkMuted">
                    Permanently delete all transactions, members, and cards to start completely fresh
                  </p>
                </div>
              </div>
              <ChevronRight size={16} className="text-red-400 shrink-0" />
            </div>
          </div>
        </div>

        {/* ── 2. Category Management Section ─────────────────────────── */}
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
                onKeyDown={async (e) => {
                  if (e.key === 'Enter' && newExpenseCat.trim()) {
                    await onAddCategory(newExpenseCat.trim(), 'expense');
                    setNewExpenseCat('');
                  }
                }}
                className="flex-1 px-3 py-1.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-medium"
              />
              <button
                onClick={async () => {
                  if (!newExpenseCat.trim()) return;
                  await onAddCategory(newExpenseCat.trim(), 'expense');
                  setNewExpenseCat('');
                }}
                className="px-3.5 py-1.5 rounded-xl bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark"
              >
                Add
              </button>
            </div>
            <div className="flex flex-wrap gap-1.5 pt-1 max-h-24 overflow-y-auto">
              {expenseCategories.map((cat) => (
                <span
                  key={cat}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-gray-100 dark:bg-darkCard text-xs font-medium text-gray-800 dark:text-gray-200 border border-lightBorder dark:border-darkBorder"
                >
                  <span>{cat}</span>
                  <button
                    onClick={() => onDeleteCategory(cat, 'expense')}
                    className="text-gray-400 hover:text-red-500 font-bold"
                  >
                    ×
                  </button>
                </span>
              ))}
            </div>
          </div>

          {/* Income Categories */}
          <div className="space-y-2 pt-2 border-t border-lightBorder dark:border-darkBorder">
            <label className="text-xs font-semibold text-gray-700 dark:text-gray-300">
              Income Categories
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                placeholder="New income category..."
                value={newIncomeCat}
                onChange={(e) => setNewIncomeCat(e.target.value)}
                onKeyDown={async (e) => {
                  if (e.key === 'Enter' && newIncomeCat.trim()) {
                    await onAddCategory(newIncomeCat.trim(), 'income');
                    setNewIncomeCat('');
                  }
                }}
                className="flex-1 px-3 py-1.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-medium"
              />
              <button
                onClick={async () => {
                  if (!newIncomeCat.trim()) return;
                  await onAddCategory(newIncomeCat.trim(), 'income');
                  setNewIncomeCat('');
                }}
                className="px-3.5 py-1.5 rounded-xl bg-emerald-600 text-white text-xs font-bold shadow hover:bg-emerald-700"
              >
                Add
              </button>
            </div>
            <div className="flex flex-wrap gap-1.5 pt-1 max-h-24 overflow-y-auto">
              {incomeCategories.map((cat) => (
                <span
                  key={cat}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-emerald-50 dark:bg-emerald-950/30 text-emerald-800 dark:text-emerald-300 text-xs font-medium border border-emerald-200 dark:border-emerald-800/40"
                >
                  <span>{cat}</span>
                  <button
                    onClick={() => onDeleteCategory(cat, 'income')}
                    className="text-emerald-600 dark:text-emerald-400 hover:text-red-500 font-bold"
                  >
                    ×
                  </button>
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* ── 3. Monthly Category Budget Caps ────────────────────────── */}
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
              {['Overall', ...expenseCategories.filter((c) => c.toLowerCase() !== 'overall')].map((c) => (
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
              onKeyDown={async (e) => {
                if (e.key === 'Enter') {
                  const lim = parseFloat(evaluateMathExpression(budgetLimit) || budgetLimit);
                  if (isNaN(lim) || lim <= 0) return;
                  await onAddBudgetGoal(budgetCat, lim);
                  setBudgetLimit('');
                }
              }}
              className="w-28 px-3 py-1.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
            />
            <button
              onClick={async () => {
                const lim = parseFloat(evaluateMathExpression(budgetLimit) || budgetLimit);
                if (isNaN(lim) || lim <= 0) return;
                await onAddBudgetGoal(budgetCat, lim);
                setBudgetLimit('');
              }}
              className="px-3.5 py-1.5 rounded-xl bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark whitespace-nowrap"
            >
              Set Cap
            </button>
          </div>

          <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
            {sortedBudgetGoals.length === 0 ? (
              <p className="text-xs text-gray-500 dark:text-darkMuted py-3 text-center">
                No monthly budget caps configured.
              </p>
            ) : (
              sortedBudgetGoals.map((bg) => (
                <div
                  key={bg.category}
                  className="flex items-center justify-between p-2.5 rounded-xl bg-gray-50 dark:bg-darkCard border border-lightBorder dark:border-darkBorder text-xs group hover:border-finance-blue/40 transition-colors"
                >
                  <div
                    onClick={() => {
                      setEditingBudgetGoal(bg);
                      setEditBudgetLimit(bg.limit_amount.toString());
                    }}
                    className="cursor-pointer flex-1"
                  >
                    <div className="flex items-center gap-1.5">
                      <span className="font-semibold text-gray-800 dark:text-gray-200">
                        {bg.category}
                      </span>
                      {bg.category.toLowerCase() === 'overall' && (
                        <span className="px-1.5 py-0.2 rounded text-[9px] bg-blue-100 text-finance-blue dark:bg-blue-950/60 font-bold">
                          PRIMARY
                        </span>
                      )}
                    </div>
                    <span className="block text-[11px] text-gray-500 dark:text-darkMuted">
                      Monthly Cap: {formatCurrency(bg.limit_amount)} • <span className="text-finance-blue font-medium">Click to edit</span>
                    </span>
                  </div>
                  <button
                    onClick={() => onDeleteBudgetGoal(bg.category)}
                    className="p-1 rounded text-gray-400 hover:text-red-500"
                    title="Delete Goal"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              ))
            )}
          </div>
        </div>

        {/* ── 4. Savings Goals Manager (Full Android Parity) ─────────── */}
        <div className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <PiggyBank size={18} className="text-emerald-500" />
              <h3 className="font-bold text-sm text-gray-900 dark:text-white">Savings Goals</h3>
            </div>
            <button
              onClick={() => setShowAddSavings(true)}
              className="px-3 py-1 rounded-xl bg-emerald-600 text-white text-xs font-bold shadow hover:bg-emerald-700 flex items-center gap-1"
            >
              <Plus size={13} />
              <span>Add Goal</span>
            </button>
          </div>

          <div className="space-y-2.5 max-h-56 overflow-y-auto pr-1">
            {savingsGoals.length === 0 ? (
              <div className="text-center py-6">
                <PiggyBank size={28} className="mx-auto text-gray-300 dark:text-darkMuted mb-1" />
                <p className="text-xs text-gray-500 dark:text-darkMuted">
                  No savings goals created yet.
                </p>
                <button
                  onClick={() => setShowAddSavings(true)}
                  className="mt-2 text-xs font-semibold text-emerald-600 dark:text-emerald-400 hover:underline"
                >
                  Create your first goal (e.g. Vacation, Bike, Emergency Reserve)
                </button>
              </div>
            ) : (
              savingsGoals.map((sg) => {
                const pct =
                  sg.target_amount > 0
                    ? Math.min(100, Math.round((sg.current_amount / sg.target_amount) * 100))
                    : 0;

                return (
                  <div
                    key={sg.id}
                    className="p-3 rounded-xl bg-gray-50 dark:bg-darkCard border border-lightBorder dark:border-darkBorder space-y-2 group hover:border-emerald-500/40 transition-colors"
                  >
                    <div className="flex items-center justify-between text-xs">
                      <div
                        onClick={() => {
                          setEditingSavings(sg);
                          setEditSavingName(sg.name);
                          setEditSavingTarget(sg.target_amount.toString());
                          setEditSavingCurrent(sg.current_amount.toString());
                          setEditSavingCategory(sg.category || 'Overall');
                        }}
                        className="cursor-pointer flex-1"
                      >
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-gray-900 dark:text-white">{sg.name}</span>
                          <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 font-semibold">
                            {sg.category}
                          </span>
                        </div>
                        <span className="text-[11px] text-gray-500 dark:text-darkMuted">
                          Saved: <strong className="text-emerald-600 dark:text-emerald-400">{formatCurrency(sg.current_amount)}</strong> of {formatCurrency(sg.target_amount)} • <span className="text-finance-blue">Edit</span>
                        </span>
                      </div>

                      <div className="flex items-center gap-2">
                        <span className="font-extrabold text-emerald-600 dark:text-emerald-400 text-xs">
                          {pct}%
                        </span>
                        <button
                          onClick={() => onDeleteSavingsGoal(sg.id)}
                          className="p-1 text-gray-400 hover:text-red-500 transition-colors"
                          title="Delete Goal"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>

                    {/* Progress Bar */}
                    <div className="w-full bg-gray-200 dark:bg-darkBorder h-2 rounded-full overflow-hidden">
                      <div
                        style={{ width: `${pct}%` }}
                        className="h-full bg-emerald-500 rounded-full transition-all duration-300"
                      />
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* ── 5. Recurring Subscriptions Scheduler ──────────────────── */}
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
              className="px-3 py-1 rounded-xl bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark flex items-center gap-1"
            >
              <Plus size={13} />
              <span>Add Bill</span>
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
                  className="flex items-center justify-between p-2.5 rounded-xl bg-gray-50 dark:bg-darkCard border border-lightBorder dark:border-darkBorder text-xs group hover:border-finance-teal/40 transition-colors"
                >
                  <div
                    onClick={() => {
                      setEditingRecurring(rec);
                      setEditRecAmount(rec.amount.toString());
                      setEditRecNotes(rec.notes || '');
                      setEditRecInterval(rec.interval_type as any || 'Monthly');
                      setEditRecNextDue(new Date(rec.next_due_date).toISOString().slice(0, 10));
                    }}
                    className="cursor-pointer flex-1"
                  >
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-gray-800 dark:text-gray-200">
                        {rec.notes || rec.category}
                      </span>
                      <span className="text-[10px] px-1.5 py-0.2 rounded bg-gray-200 dark:bg-darkBorder font-medium">
                        {rec.interval_type}
                      </span>
                    </div>
                    <span className="block text-[11px] text-gray-500 dark:text-darkMuted">
                      {formatCurrency(rec.amount)} • Next Due:{' '}
                      <strong className="text-gray-700 dark:text-gray-300">
                        {formatIndianDate(rec.next_due_date)}
                      </strong>{' '}
                      • {rec.category} • <span className="text-finance-blue">Edit</span>
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={rec.is_active}
                      onChange={async (e) => {
                        await onUpdateRecurring({ ...rec, is_active: e.target.checked });
                      }}
                      className="w-4 h-4 rounded text-finance-blue cursor-pointer"
                      title="Toggle Active"
                    />
                    <button
                      onClick={() => onDeleteRecurring(rec.id)}
                      className="p-1 text-gray-400 hover:text-red-500"
                      title="Delete Schedule"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* ── 6. Theme & Custom Wallet Appearance ────────────────────── */}
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
                        ? 'bg-finance-blue text-white border-finance-blue shadow'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted hover:bg-gray-50 dark:hover:bg-darkCard'
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
                <div className="flex items-center gap-2">
                  <input
                    type="color"
                    value={expenseWalletColor}
                    onChange={(e) => setExpenseWalletColor(e.target.value)}
                    className="w-10 h-10 p-1 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard cursor-pointer shrink-0"
                  />
                  <span className="text-xs font-mono text-gray-600 dark:text-darkMuted uppercase">
                    {expenseWalletColor}
                  </span>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Default Credit Card Color
                </label>
                <div className="flex items-center gap-2">
                  <input
                    type="color"
                    value={creditCardColor}
                    onChange={(e) => setCreditCardColor(e.target.value)}
                    className="w-10 h-10 p-1 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard cursor-pointer shrink-0"
                  />
                  <span className="text-xs font-mono text-gray-600 dark:text-darkMuted uppercase">
                    {creditCardColor}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* ── 7. Supabase Real-Time Cloud Sync ────────────────────────── */}
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
                Direct encrypted connection to PostgreSQL backend. All records and goals are backed up automatically.
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

        {/* ── 8. Log Out Action (Mobile Parity) ───────────────────────── */}
        {onLogout && (
          <div className="col-span-full">
            <button
              onClick={onLogout}
              className="w-full py-3 rounded-xl bg-red-50 dark:bg-red-950/20 text-red-600 dark:text-red-400 border border-red-200 dark:border-red-900/40 font-bold text-sm hover:bg-red-100 dark:hover:bg-red-900/40 transition-colors flex items-center justify-center gap-2 shadow-sm"
            >
              <LogOut size={16} />
              <span>Log Out of Ledgerly</span>
            </button>
          </div>
        )}
      </div>

      {/* ── MODALS ─────────────────────────────────────────────────── */}

      {/* Add Savings Goal Modal */}
      {showAddSavings && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <div className="flex items-center gap-2">
              <PiggyBank size={20} className="text-emerald-500" />
              <h3 className="font-bold text-base text-gray-900 dark:text-white">
                New Savings Goal
              </h3>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="font-semibold block mb-1">Goal Name</label>
                <input
                  type="text"
                  placeholder="e.g. Royal Enfield, Vacation, MacBook"
                  value={savingName}
                  onChange={(e) => setSavingName(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard font-medium"
                />
              </div>

              <AmountInputField
                value={savingTarget}
                onChange={setSavingTarget}
                label="Target Amount"
                required
              />

              <AmountInputField
                value={savingCurrent}
                onChange={setSavingCurrent}
                label="Current Saved Amount (Optional)"
              />

              <div>
                <label className="font-semibold block mb-1">Category (Overall or Specific)</label>
                <select
                  value={savingCategory}
                  onChange={(e) => setSavingCategory(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard font-medium"
                >
                  {['Overall', ...expenseCategories.filter((c) => c.toLowerCase() !== 'overall')].map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setShowAddSavings(false)}
                className="px-3.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  const t = parseFloat(evaluateMathExpression(savingTarget) || savingTarget);
                  const c = parseFloat(evaluateMathExpression(savingCurrent) || savingCurrent) || 0;
                  if (!savingName.trim() || isNaN(t) || t <= 0) return;
                  await onAddSavingsGoal({
                    name: savingName.trim(),
                    target_amount: t,
                    current_amount: c,
                    category: savingCategory,
                  });
                  setShowAddSavings(false);
                  setSavingName('');
                  setSavingTarget('');
                  setSavingCurrent('');
                }}
                className="px-4 py-1.5 rounded-lg bg-emerald-600 text-white text-xs font-bold shadow hover:bg-emerald-700"
              >
                Save Goal
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Savings Goal Modal */}
      {editingSavings && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <h3 className="font-bold text-base text-gray-900 dark:text-white">
              Edit Savings Goal
            </h3>

            <div className="space-y-3 text-xs">
              <div>
                <label className="font-semibold block mb-1">Goal Name</label>
                <input
                  type="text"
                  value={editSavingName}
                  onChange={(e) => setEditSavingName(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard font-medium"
                />
              </div>

              <AmountInputField
                value={editSavingTarget}
                onChange={setEditSavingTarget}
                label="Target Amount"
                required
              />

              <AmountInputField
                value={editSavingCurrent}
                onChange={setEditSavingCurrent}
                label="Current Saved"
              />

              <div>
                <label className="font-semibold block mb-1">Category</label>
                <select
                  value={editSavingCategory}
                  onChange={(e) => setEditSavingCategory(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard font-medium"
                >
                  {['Overall', ...expenseCategories.filter((c) => c.toLowerCase() !== 'overall')].map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setEditingSavings(null)}
                className="px-3.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  const t = parseFloat(evaluateMathExpression(editSavingTarget) || editSavingTarget);
                  const c = parseFloat(evaluateMathExpression(editSavingCurrent) || editSavingCurrent) || 0;
                  if (!editSavingName.trim() || isNaN(t) || t <= 0) return;
                  await onUpdateSavingsGoal({
                    ...editingSavings,
                    name: editSavingName.trim(),
                    target_amount: t,
                    current_amount: c,
                    category: editSavingCategory,
                  });
                  setEditingSavings(null);
                }}
                className="px-4 py-1.5 rounded-lg bg-emerald-600 text-white text-xs font-bold shadow hover:bg-emerald-700"
              >
                Update Goal
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Budget Goal Limit Modal */}
      {editingBudgetGoal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <h3 className="font-bold text-base text-gray-900 dark:text-white">
              Edit Budget Cap: {editingBudgetGoal.category}
            </h3>

            <div className="space-y-3 text-xs">
              <AmountInputField
                value={editBudgetLimit}
                onChange={setEditBudgetLimit}
                label="Monthly Limit"
                required
                autoFocus
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setEditingBudgetGoal(null)}
                className="px-3.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  const lim = parseFloat(evaluateMathExpression(editBudgetLimit) || editBudgetLimit);
                  if (isNaN(lim) || lim <= 0) return;
                  await onAddBudgetGoal(editingBudgetGoal.category, lim);
                  setEditingBudgetGoal(null);
                }}
                className="px-4 py-1.5 rounded-lg bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark"
              >
                Save Cap
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add Recurring Bill Modal */}
      {showAddRecurring && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <h3 className="font-bold text-base text-gray-900 dark:text-white">
              Schedule Recurring Bill
            </h3>
            <div className="space-y-3 text-xs">
              <AmountInputField
                value={recAmount}
                onChange={setRecAmount}
                label="Amount"
                required
              />

              <div>
                <label className="font-semibold block mb-1">Description / Bill Name</label>
                <input
                  type="text"
                  placeholder="e.g. Netflix, Wifi, Electricity Bill"
                  value={recNotes}
                  onChange={(e) => setRecNotes(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard font-medium"
                />
              </div>

              {/* Transaction Type */}
              <div>
                <label className="font-semibold block mb-1">Flow Type</label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setRecType('PERSONAL_EXPENSE')}
                    className={`py-1.5 rounded-lg font-bold border transition-all ${
                      recType === 'PERSONAL_EXPENSE'
                        ? 'bg-finance-red text-white border-finance-red'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    Expense
                  </button>
                  <button
                    type="button"
                    onClick={() => setRecType('PERSONAL_INCOME')}
                    className={`py-1.5 rounded-lg font-bold border transition-all ${
                      recType === 'PERSONAL_INCOME'
                        ? 'bg-finance-green text-white border-finance-green'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    Income
                  </button>
                </div>
              </div>

              {/* Target Wallet */}
              <div>
                <label className="font-semibold block mb-1">Target Wallet</label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setRecWallet('EXPENSE_WALLET')}
                    className={`py-1.5 rounded-lg font-bold border transition-all ${
                      recWallet === 'EXPENSE_WALLET'
                        ? 'bg-finance-blue text-white border-finance-blue'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    Expense Wallet
                  </button>
                  <button
                    type="button"
                    onClick={() => setRecWallet('SAVINGS_WALLET')}
                    className={`py-1.5 rounded-lg font-bold border transition-all ${
                      recWallet === 'SAVINGS_WALLET'
                        ? 'bg-emerald-600 text-white border-emerald-600'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    Savings Wallet
                  </button>
                </div>
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

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="font-semibold block mb-1">Frequency</label>
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

                <div>
                  <label className="font-semibold block mb-1">Next Due Date</label>
                  <input
                    type="date"
                    value={recNextDueDate}
                    onChange={(e) => setRecNextDueDate(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard"
                  />
                </div>
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
                  const amt = parseFloat(evaluateMathExpression(recAmount) || recAmount);
                  if (isNaN(amt) || amt <= 0 || !recNotes.trim()) return;
                  const nextDueMs = recNextDueDate ? new Date(recNextDueDate).getTime() : Date.now() + 30 * 24 * 60 * 60 * 1000;
                  await onAddRecurring({
                    amount: amt,
                    notes: recNotes.trim(),
                    category: recCategory,
                    type: recType,
                    source_wallet: recWallet,
                    interval_type: recInterval,
                    last_applied_date: Date.now(),
                    next_due_date: nextDueMs,
                    is_active: true,
                  });
                  setShowAddRecurring(false);
                  setRecAmount('');
                  setRecNotes('');
                }}
                className="px-4 py-1.5 rounded-lg bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark"
              >
                Save Schedule
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Recurring Bill Modal */}
      {editingRecurring && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <h3 className="font-bold text-base text-gray-900 dark:text-white">
              Edit Recurring Schedule
            </h3>
            <div className="space-y-3 text-xs">
              <AmountInputField
                value={editRecAmount}
                onChange={setEditRecAmount}
                label="Amount"
                required
              />

              <div>
                <label className="font-semibold block mb-1">Description / Bill Name</label>
                <input
                  type="text"
                  value={editRecNotes}
                  onChange={(e) => setEditRecNotes(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard font-medium"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="font-semibold block mb-1">Frequency</label>
                  <select
                    value={editRecInterval}
                    onChange={(e) => setEditRecInterval(e.target.value as any)}
                    className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard"
                  >
                    <option value="Daily">Daily</option>
                    <option value="Weekly">Weekly</option>
                    <option value="Monthly">Monthly</option>
                    <option value="Yearly">Yearly</option>
                  </select>
                </div>

                <div>
                  <label className="font-semibold block mb-1">Next Due Date</label>
                  <input
                    type="date"
                    value={editRecNextDue}
                    onChange={(e) => setEditRecNextDue(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard"
                  />
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setEditingRecurring(null)}
                className="px-3.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  const amt = parseFloat(evaluateMathExpression(editRecAmount) || editRecAmount);
                  if (isNaN(amt) || amt <= 0) return;
                  const nextDueMs = editRecNextDue ? new Date(editRecNextDue).getTime() : editingRecurring.next_due_date;
                  await onUpdateRecurring({
                    ...editingRecurring,
                    amount: amt,
                    notes: editRecNotes,
                    interval_type: editRecInterval,
                    next_due_date: nextDueMs,
                  });
                  setEditingRecurring(null);
                }}
                className="px-4 py-1.5 rounded-lg bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark"
              >
                Update Schedule
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Wipe Data Confirmation Modal */}
      {showWipeConfirm && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-red-300 dark:border-red-900/60 rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <div className="flex items-center gap-2 text-red-600 dark:text-red-400">
              <AlertTriangle size={22} />
              <h3 className="font-bold text-base">Confirm Permanent Wipeout?</h3>
            </div>
            <p className="text-xs text-gray-600 dark:text-darkMuted leading-relaxed">
              This will completely delete all transactions, family members, credit cards, custom sub-wallets, budget goals, savings goals, and recurring entries. This action is permanent and cannot be undone.
            </p>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setShowWipeConfirm(false)}
                disabled={wiping}
                className="px-3.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                disabled={wiping}
                onClick={async () => {
                  if (!onClearAllData) return;
                  setWiping(true);
                  await onClearAllData();
                  setWiping(false);
                  setShowWipeConfirm(false);
                }}
                className="px-4 py-1.5 rounded-lg bg-red-600 hover:bg-red-700 text-white text-xs font-bold shadow disabled:opacity-50 flex items-center gap-1.5"
              >
                {wiping && <RefreshCw size={13} className="animate-spin" />}
                <span>{wiping ? 'Wiping...' : 'Wipe Everything'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
