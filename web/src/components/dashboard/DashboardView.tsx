import React, { useState, useMemo } from 'react';
import {
  TrendingUp,
  TrendingDown,
  Wallet,
  CreditCard as CardIcon,
  PiggyBank,
  ArrowUpRight,
  ArrowDownLeft,
  ChevronRight,
  Flame,
  PlusCircle,
  MinusCircle,
  Target,
} from 'lucide-react';
import {
  Transaction,
  SavingsGoal,
  BudgetGoal,
  CreditCard,
} from '../../lib/types';
import { formatCurrency } from '../../lib/calculations';
import { useTheme } from '../../context/ThemeContext';

interface DashboardViewProps {
  expenseBalance: number;
  savingsBalance: number;
  transactions: Transaction[];
  savingsGoals: SavingsGoal[];
  budgetGoals: BudgetGoal[];
  creditCards: CreditCard[];
  monthlyProjection: number;
  streak: number;
  onOpenTransactionModal: () => void;
  onNavigateToTransactions: () => void;
  onNavigateToAnalytics: () => void;
  onUpdateSavingsGoal: (goal: SavingsGoal) => Promise<any>;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  expenseBalance,
  savingsBalance,
  transactions,
  savingsGoals,
  budgetGoals,
  creditCards,
  monthlyProjection,
  streak,
  onOpenTransactionModal,
  onNavigateToTransactions,
  onNavigateToAnalytics,
  onUpdateSavingsGoal,
}) => {
  const { expenseWalletColor } = useTheme();

  // Savings Quick Fund Dialog state
  const [selectedSavingsGoal, setSelectedSavingsGoal] = useState<SavingsGoal | null>(null);
  const [fundAmount, setFundAmount] = useState<string>('');
  const [isDeposit, setIsDeposit] = useState<boolean>(true);
  const [fundSubmitting, setFundSubmitting] = useState<boolean>(false);

  // Helper to reliably parse transaction date
  const getTxDateMs = (t: Transaction): number => {
    if (typeof t.date === 'number') return t.date;
    const parsed = new Date(t.date).getTime();
    return isNaN(parsed) ? 0 : parsed;
  };

  // Current Month Calculations
  const now = new Date();
  const currentMonthStart = new Date(now.getFullYear(), now.getMonth(), 1).getTime();

  const currentMonthTxs = transactions.filter((t) => getTxDateMs(t) >= currentMonthStart);
  const monthlyExpense = currentMonthTxs
    .filter(
      (t) =>
        (t.type === 'PERSONAL_EXPENSE' && t.source_wallet !== 'TEMPORARY_WALLET') ||
        (t.type === 'FAMILY_TRANSFER' && t.direction === 'GIVEN_TO_THEM' && t.source_wallet !== 'TEMPORARY_WALLET')
    )
    .reduce((sum, t) => sum + Math.abs(t.amount || 0), 0);
  const monthlyIncome = currentMonthTxs
    .filter(
      (t) =>
        t.type === 'PERSONAL_INCOME' ||
        (t.type === 'FAMILY_TRANSFER' && t.direction === 'RECEIVED_FROM_THEM')
    )
    .reduce((sum, t) => sum + Math.abs(t.amount || 0), 0);
  const netSavings = monthlyIncome - monthlyExpense;

  const sortedBudgetGoals = useMemo(() => {
    return [...budgetGoals].sort((a, b) => {
      if (a.category.toLowerCase() === 'overall') return -1;
      if (b.category.toLowerCase() === 'overall') return 1;
      return a.category.localeCompare(b.category);
    });
  }, [budgetGoals]);

  const overallBudgetGoal = useMemo(() => {
    return budgetGoals.find((b) => b.category?.toLowerCase() === 'overall');
  }, [budgetGoals]);

  // Credit Card with highest spend
  const topCard = creditCards.length > 0
    ? creditCards.reduce((top, card) => {
        const spend = transactions
          .filter((t) => t.source_wallet === 'CREDIT_CARD' && t.credit_card_id === card.id)
          .reduce((sum, t) => sum + Math.abs(t.amount || 0), 0);
        return spend > (top.spend || 0) ? { card, spend } : top;
      }, { card: creditCards[0], spend: 0 })
    : null;

  // Recent 5 transactions
  const recentTxs = transactions.slice(0, 6);

  const handleFundSavings = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSavingsGoal) return;
    const val = parseFloat(fundAmount);
    if (isNaN(val) || val <= 0) return;

    setFundSubmitting(true);
    const newAmount = isDeposit
      ? selectedSavingsGoal.current_amount + val
      : Math.max(0, selectedSavingsGoal.current_amount - val);

    await onUpdateSavingsGoal({
      ...selectedSavingsGoal,
      current_amount: newAmount,
    });
    setFundSubmitting(false);
    setSelectedSavingsGoal(null);
    setFundAmount('');
  };

  return (
    <div className="space-y-6">
      {/* ── Top Dual Wallet Cards ────────────────────────────────────── */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {/* Expense Wallet Card */}
        <div
          style={{
            background: `linear-gradient(135deg, ${expenseWalletColor || '#1A56DB'}, #1240A8)`,
          }}
          className="p-6 rounded-2xl text-white shadow-lg relative overflow-hidden flex flex-col justify-between h-44"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold tracking-wider uppercase text-blue-100">
              Expense Wallet
            </span>
            <div className="w-8 h-8 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center font-bold text-sm">
              E
            </div>
          </div>
          <div>
            <div className="text-2xl lg:text-3xl font-extrabold tracking-tight">
              {formatCurrency(expenseBalance)}
            </div>
            <p className="text-xs text-blue-100 mt-1">Available operational funds</p>
          </div>
          <div className="flex items-center justify-between text-[11px] text-blue-200">
            <span>Primary Wallet</span>
            <span>Synced</span>
          </div>
        </div>

        {/* Second Card: Top Credit Card or Savings Wallet */}
        {topCard && creditCards.length > 0 ? (
          <div
            style={{
              background: `linear-gradient(135deg, ${topCard.card.color || '#0097A7'}, #006978)`,
            }}
            className="p-6 rounded-2xl text-white shadow-lg relative overflow-hidden flex flex-col justify-between h-44"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold tracking-wider uppercase text-teal-100">
                Credit Card Expenses
              </span>
              <div className="w-8 h-8 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center font-bold text-sm">
                C
              </div>
            </div>
            <div>
              <div className="text-2xl lg:text-3xl font-extrabold tracking-tight">
                {formatCurrency(topCard.spend)}
              </div>
              <p className="text-xs text-teal-100 mt-1">
                {topCard.card.card_name} • {topCard.card.owner_name}
              </p>
            </div>
            <div className="flex items-center justify-between text-[11px] text-teal-200">
              <span>Limit: {formatCurrency(topCard.card.credit_limit)}</span>
              <span>
                {Math.min(
                  100,
                  Math.round((topCard.spend / (topCard.card.credit_limit || 1)) * 100)
                )}
                % Used
              </span>
            </div>
          </div>
        ) : (
          <div className="p-6 rounded-2xl text-white bg-gradient-to-br from-finance-green to-finance-greenDark shadow-lg relative overflow-hidden flex flex-col justify-between h-44">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold tracking-wider uppercase text-emerald-100">
                Savings Wallet
              </span>
              <div className="w-8 h-8 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center font-bold text-sm">
                S
              </div>
            </div>
            <div>
              <div className="text-2xl lg:text-3xl font-extrabold tracking-tight">
                {formatCurrency(savingsBalance)}
              </div>
              <p className="text-xs text-emerald-100 mt-1">Accumulated reserve balance</p>
            </div>
            <div className="flex items-center justify-between text-[11px] text-emerald-200">
              <span>Secured Funds</span>
              <span>Active</span>
            </div>
          </div>
        )}

        {/* 90-Day Cost Projection Gauge Card */}
        <div
          onClick={onNavigateToAnalytics}
          className="p-6 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm hover:border-finance-blue dark:hover:border-finance-blue transition-all cursor-pointer flex flex-col justify-between h-44"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-finance-blue uppercase tracking-wider">
              Cost Projection
            </span>
            <span className="text-[11px] px-2 py-0.5 rounded-full bg-blue-50 dark:bg-blue-950/40 text-finance-blue font-medium">
              Based on 90 Days
            </span>
          </div>
          <div>
            <div className="text-2xl lg:text-3xl font-extrabold tracking-tight text-gray-900 dark:text-white">
              {formatCurrency(monthlyProjection)}
            </div>
            {overallBudgetGoal && overallBudgetGoal.limit_amount > 0 ? (
              <p className="text-xs text-gray-500 dark:text-darkMuted mt-1">
                {monthlyProjection > overallBudgetGoal.limit_amount
                  ? `📈 ${(((monthlyProjection - overallBudgetGoal.limit_amount) / overallBudgetGoal.limit_amount) * 100).toFixed(0)}% over Overall cap (${formatCurrency(overallBudgetGoal.limit_amount)})`
                  : `📉 ${(((overallBudgetGoal.limit_amount - monthlyProjection) / overallBudgetGoal.limit_amount) * 100).toFixed(0)}% under Overall cap (${formatCurrency(overallBudgetGoal.limit_amount)})`}
              </p>
            ) : (
              <p className="text-xs text-gray-500 dark:text-darkMuted mt-1">
                Estimated expenses / month
              </p>
            )}
          </div>
          <div className="flex items-center justify-between text-xs text-finance-blue font-medium">
            <span>View Projection Details</span>
            <ChevronRight size={14} />
          </div>
        </div>
      </div>

      {/* ── Monthly KPI Quick Summary ─────────────────────────────────── */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-4 rounded-xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 text-finance-green">
            <TrendingUp size={20} />
          </div>
          <div>
            <p className="text-xs text-gray-500 dark:text-darkMuted font-medium">Month Income</p>
            <p className="text-base font-bold text-gray-900 dark:text-white">
              {formatCurrency(monthlyIncome)}
            </p>
          </div>
        </div>

        <div className="p-4 rounded-xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-red-50 dark:bg-red-950/40 text-finance-red">
            <TrendingDown size={20} />
          </div>
          <div>
            <p className="text-xs text-gray-500 dark:text-darkMuted font-medium">Month Expenses</p>
            <p className="text-base font-bold text-gray-900 dark:text-white">
              {formatCurrency(monthlyExpense)}
            </p>
          </div>
        </div>

        <div className="p-4 rounded-xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-blue-50 dark:bg-blue-950/40 text-finance-blue">
            <Wallet size={20} />
          </div>
          <div>
            <p className="text-xs text-gray-500 dark:text-darkMuted font-medium">Net Cashflow</p>
            <p
              className={`text-base font-bold ${
                netSavings >= 0 ? 'text-finance-green' : 'text-finance-red'
              }`}
            >
              {formatCurrency(netSavings)}
            </p>
          </div>
        </div>

        <div className="p-4 rounded-xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-amber-50 dark:bg-amber-950/40 text-finance-gold">
            <Flame size={20} />
          </div>
          <div>
            <p className="text-xs text-gray-500 dark:text-darkMuted font-medium">Daily Streak</p>
            <p className="text-base font-bold text-gray-900 dark:text-white">
              {streak} Days Active
            </p>
          </div>
        </div>
      </div>

      {/* ── Budget Goals Carousel / Grid ─────────────────────────────── */}
      {sortedBudgetGoals.length > 0 && (
        <div className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Target size={18} className="text-finance-blue" />
              <h3 className="text-sm font-bold text-gray-900 dark:text-white">
                Monthly Category Budgets
              </h3>
            </div>
            <span className="text-xs text-gray-500 dark:text-darkMuted">
              {sortedBudgetGoals.length} Categories Tracked
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {sortedBudgetGoals.map((bg) => {
              const isOverall = bg.category?.toLowerCase() === 'overall';
              const spent = transactions
                .filter((t) => {
                  const txDate = getTxDateMs(t);
                  if (txDate < currentMonthStart) return false;
                  if (t.source_wallet === 'TEMPORARY_WALLET') return false;

                  const matchesCategory =
                    isOverall || (t.category && t.category.toLowerCase() === bg.category.toLowerCase());
                  if (!matchesCategory) return false;

                  return (
                    t.type === 'PERSONAL_EXPENSE' ||
                    (t.type === 'FAMILY_TRANSFER' && t.direction === 'GIVEN_TO_THEM')
                  );
                })
                .reduce((sum, t) => sum + Math.abs(t.amount || 0), 0);
              const pct = bg.limit_amount > 0 ? Math.round((spent / bg.limit_amount) * 100) : 0;
              const isOver = pct > 100;
              const isNear = pct >= 80 && pct <= 100;

              return (
                <div
                  key={bg.category}
                  className="p-3.5 rounded-xl bg-gray-50 dark:bg-darkCard/50 border border-lightBorder dark:border-darkBorder/60 space-y-2"
                >
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-semibold text-gray-800 dark:text-gray-200">
                      {bg.category}
                    </span>
                    <span
                      className={`font-bold ${
                        isOver
                          ? 'text-finance-red'
                          : isNear
                          ? 'text-finance-gold'
                          : 'text-finance-green'
                      }`}
                    >
                      {pct}%
                    </span>
                  </div>
                  <div className="w-full bg-gray-200 dark:bg-darkBorder h-2 rounded-full overflow-hidden">
                    <div
                      style={{ width: `${Math.min(100, pct)}%` }}
                      className={`h-full rounded-full transition-all ${
                        isOver
                          ? 'bg-finance-red'
                          : isNear
                          ? 'bg-finance-gold'
                          : 'bg-finance-green'
                      }`}
                    />
                  </div>
                  <div className="flex justify-between text-[11px] text-gray-500 dark:text-darkMuted">
                    <span>{formatCurrency(spent)}</span>
                    <span>Cap: {formatCurrency(bg.limit_amount)}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ── Savings Goals Cards ──────────────────────────────────────── */}
      {savingsGoals.length > 0 && (
        <div className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <PiggyBank size={18} className="text-finance-green" />
              <h3 className="text-sm font-bold text-gray-900 dark:text-white">Savings Goals</h3>
            </div>
            <span className="text-xs text-gray-500 dark:text-darkMuted">
              {savingsGoals.length} Goals Active
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {savingsGoals.map((sg) => {
              const pct =
                sg.target_amount > 0
                  ? Math.min(100, Math.round((sg.current_amount / sg.target_amount) * 100))
                  : 0;

              return (
                <div
                  key={sg.id}
                  className="p-4 rounded-xl bg-gray-50 dark:bg-darkCard/50 border border-lightBorder dark:border-darkBorder/60 flex flex-col justify-between space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <h4 className="font-semibold text-sm text-gray-900 dark:text-white">
                        {sg.name}
                      </h4>
                      <p className="text-[11px] text-gray-500 dark:text-darkMuted">{sg.category}</p>
                    </div>
                    <span className="text-xs font-bold text-finance-green">{pct}%</span>
                  </div>

                  <div className="w-full bg-gray-200 dark:bg-darkBorder h-2 rounded-full overflow-hidden">
                    <div
                      style={{ width: `${pct}%` }}
                      className="h-full bg-finance-green rounded-full transition-all"
                    />
                  </div>

                  <div className="flex items-center justify-between pt-1">
                    <div className="text-xs">
                      <span className="font-bold text-gray-900 dark:text-white">
                        {formatCurrency(sg.current_amount)}
                      </span>
                      <span className="text-gray-500 dark:text-darkMuted text-[10px]">
                        {' '}
                        / {formatCurrency(sg.target_amount)}
                      </span>
                    </div>

                    <button
                      onClick={() => {
                        setSelectedSavingsGoal(sg);
                        setFundAmount('');
                        setIsDeposit(true);
                      }}
                      className="px-2.5 py-1 rounded-lg bg-finance-green/10 text-finance-green hover:bg-finance-green/20 text-xs font-semibold transition-colors"
                    >
                      Fund Goal
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ── Recent Activity Feed ─────────────────────────────────────── */}
      <div className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold text-gray-900 dark:text-white">Recent Transactions</h3>
          <button
            onClick={onNavigateToTransactions}
            className="text-xs text-finance-blue font-semibold hover:underline flex items-center gap-1"
          >
            <span>View All</span>
            <ChevronRight size={14} />
          </button>
        </div>

        {recentTxs.length === 0 ? (
          <p className="text-xs text-gray-500 dark:text-darkMuted py-4 text-center">
            No transactions logged yet. Click "+ Log Transaction" to add your first record!
          </p>
        ) : (
          <div className="divide-y divide-lightBorder dark:divide-darkBorder/60">
            {recentTxs.map((tx) => {
              const isIncome =
                tx.type === 'PERSONAL_INCOME' ||
                (tx.type === 'FAMILY_TRANSFER' && tx.direction === 'RECEIVED_FROM_THEM') ||
                (tx.type === 'BORROW_LEND' && tx.direction === 'BORROWED');

              return (
                <div
                  key={tx.id}
                  className="py-3 flex items-center justify-between gap-4 text-xs hover:bg-gray-50 dark:hover:bg-darkCard/40 px-2 rounded-lg transition-colors"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <div
                      className={`p-2 rounded-xl shrink-0 ${
                        isIncome
                          ? 'bg-emerald-50 text-finance-green dark:bg-emerald-950/40'
                          : 'bg-red-50 text-finance-red dark:bg-red-950/40'
                      }`}
                    >
                      {isIncome ? <ArrowDownLeft size={16} /> : <ArrowUpRight size={16} />}
                    </div>
                    <div className="truncate">
                      <p className="font-semibold text-gray-900 dark:text-white truncate">
                        {tx.notes || tx.category}
                      </p>
                      <p className="text-[11px] text-gray-500 dark:text-darkMuted">
                        {new Date(tx.date).toLocaleDateString()} • {tx.category} •{' '}
                        {tx.source_wallet.replace('_WALLET', '')}
                      </p>
                    </div>
                  </div>

                  <span
                    className={`font-bold shrink-0 text-sm ${
                      isIncome ? 'text-finance-green' : 'text-finance-red'
                    }`}
                  >
                    {isIncome ? '+' : '-'}
                    {formatCurrency(tx.amount)}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* ── Quick Savings Goal Fund Modal ────────────────────────────── */}
      {selectedSavingsGoal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <h3 className="font-bold text-base text-gray-900 dark:text-white">
              Fund Savings Goal: {selectedSavingsGoal.name}
            </h3>
            <p className="text-xs text-gray-500 dark:text-darkMuted">
              Current Balance: {formatCurrency(selectedSavingsGoal.current_amount)} /{' '}
              {formatCurrency(selectedSavingsGoal.target_amount)}
            </p>

            <form onSubmit={handleFundSavings} className="space-y-4">
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setIsDeposit(true)}
                  className={`flex-1 py-1.5 text-xs font-semibold rounded-lg border ${
                    isDeposit
                      ? 'bg-finance-green text-white border-finance-green'
                      : 'border-lightBorder dark:border-darkBorder text-gray-600 dark:text-darkMuted'
                  }`}
                >
                  Deposit
                </button>
                <button
                  type="button"
                  onClick={() => setIsDeposit(false)}
                  className={`flex-1 py-1.5 text-xs font-semibold rounded-lg border ${
                    !isDeposit
                      ? 'bg-finance-red text-white border-finance-red'
                      : 'border-lightBorder dark:border-darkBorder text-gray-600 dark:text-darkMuted'
                  }`}
                >
                  Withdraw
                </button>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Amount (₹)
                </label>
                <input
                  type="number"
                  step="0.01"
                  required
                  placeholder="0.00"
                  value={fundAmount}
                  onChange={(e) => setFundAmount(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-sm font-semibold"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setSelectedSavingsGoal(null)}
                  className="px-3 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={fundSubmitting}
                  className="px-4 py-1.5 rounded-lg bg-finance-green text-white text-xs font-bold shadow disabled:opacity-50"
                >
                  {fundSubmitting ? 'Updating...' : isDeposit ? 'Deposit Funds' : 'Withdraw Funds'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
