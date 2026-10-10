import React, { useMemo } from 'react';
import {
  CreditCard as CardIcon,
  BarChart3,
  Calendar,
  AlertCircle,
  TrendingDown,
  PieChart as PieIcon,
} from 'lucide-react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
} from 'chart.js';
import { Bar, Doughnut } from 'react-chartjs-2';
import { Transaction } from '../../lib/types';
import { formatCurrency, calculate90DayOutflows } from '../../lib/calculations';
import { useTheme } from '../../context/ThemeContext';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, ArcElement);

interface AnalyticsViewProps {
  transactions: Transaction[];
  monthlyProjection: number;
}

export const AnalyticsView: React.FC<AnalyticsViewProps> = ({
  transactions,
  monthlyProjection,
}) => {
  const { isDark } = useTheme();

  const { totalSpending90Days, totalCreditCardSpending } = useMemo(
    () => calculate90DayOutflows(transactions),
    [transactions]
  );

  const operationalSpending = Math.max(0, totalSpending90Days - totalCreditCardSpending);

  // Monthly Cashflow Data (Last 6 Months)
  const monthlyChartData = useMemo(() => {
    const months: { label: string; year: number; month: number; income: number; expense: number }[] = [];
    const now = new Date();

    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const label = d.toLocaleString('default', { month: 'short' });
      months.push({
        label,
        year: d.getFullYear(),
        month: d.getMonth(),
        income: 0,
        expense: 0,
      });
    }

    for (const tx of transactions) {
      const txDate = new Date(tx.date);
      const m = months.find(
        (item) => item.year === txDate.getFullYear() && item.month === txDate.getMonth()
      );
      if (m) {
        if (
          tx.type === 'PERSONAL_INCOME' ||
          (tx.type === 'FAMILY_TRANSFER' && tx.direction === 'RECEIVED_FROM_THEM') ||
          (tx.type === 'BORROW_LEND' && tx.direction === 'BORROWED')
        ) {
          m.income += Math.abs(tx.amount || 0);
        } else if (
          tx.type === 'PERSONAL_EXPENSE' ||
          (tx.type === 'FAMILY_TRANSFER' && tx.direction === 'GIVEN_TO_THEM') ||
          (tx.type === 'BORROW_LEND' && tx.direction === 'LENT')
        ) {
          m.expense += Math.abs(tx.amount || 0);
        }
      }
    }

    return {
      labels: months.map((m) => m.label),
      datasets: [
        {
          label: 'Inflows (Income)',
          data: months.map((m) => m.income),
          backgroundColor: '#0D9E6A',
          borderRadius: 6,
        },
        {
          label: 'Outflows (Expenses)',
          data: months.map((m) => m.expense),
          backgroundColor: '#DE2027',
          borderRadius: 6,
        },
      ],
    };
  }, [transactions]);

  // Category Breakdown Data (Last 90 Days)
  const categoryChartData = useMemo(() => {
    const ninetyDaysAgo = Date.now() - 90 * 24 * 60 * 60 * 1000;
    const catMap: Record<string, number> = {};

    for (const tx of transactions) {
      if (tx.date >= ninetyDaysAgo && tx.type === 'PERSONAL_EXPENSE') {
        const cat = tx.category || 'Other';
        catMap[cat] = (catMap[cat] || 0) + Math.abs(tx.amount || 0);
      }
    }

    const labels = Object.keys(catMap);
    const data = Object.values(catMap);
    const colors = [
      '#1A56DB',
      '#0D9E6A',
      '#DE2027',
      '#F5A623',
      '#0097A7',
      '#8B5CF6',
      '#EC4899',
      '#64748B',
    ];

    return {
      labels: labels.length ? labels : ['No Expenses'],
      datasets: [
        {
          data: data.length ? data : [1],
          backgroundColor: colors.slice(0, labels.length || 1),
          borderWidth: 0,
        },
      ],
    };
  }, [transactions]);

  return (
    <div className="space-y-6">
      {/* ── Top Header ───────────────────────────────────────────────── */}
      <div>
        <h2 className="text-xl font-bold text-gray-900 dark:text-white">Estimation Insight</h2>
        <p className="text-xs text-gray-500 dark:text-darkMuted mt-0.5">
          90-Day expense trajectory, credit card velocity, and predictive monthly projection.
        </p>
      </div>

      {/* ── Large Monthly Cost Projection Gauge Card ─────────────────── */}
      <div className="p-6 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold text-finance-blue uppercase tracking-wider">
            Monthly Cost Projection
          </span>
          <span className="px-2.5 py-1 rounded-full bg-blue-50 dark:bg-blue-950/40 text-finance-blue text-xs font-semibold">
            Based on 3 months (90 days)
          </span>
        </div>

        <div className="flex items-baseline gap-3">
          <span className="text-4xl font-extrabold text-gray-900 dark:text-white tracking-tight">
            {formatCurrency(monthlyProjection)}
          </span>
          <span className="text-xs text-gray-500 dark:text-darkMuted font-medium">
            est. expenses / month
          </span>
        </div>

        {/* Progress Gauge */}
        <div className="w-full bg-gray-100 dark:bg-darkBorder h-2.5 rounded-full overflow-hidden">
          <div className="h-full bg-finance-blue rounded-full w-[65%] transition-all" />
        </div>
      </div>

      {/* ── Credit Cards 90-Day Spending Card ────────────────────────── */}
      <div className="p-5 rounded-2xl bg-red-500/5 dark:bg-red-500/10 border border-red-500/20 flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-red-500/10 text-finance-red">
            <CardIcon size={22} />
          </div>
          <div>
            <h4 className="font-bold text-sm text-gray-900 dark:text-white">
              Credit Cards Spending (90 Days)
            </h4>
            <p className="text-xs text-gray-500 dark:text-darkMuted">
              Total card payments recorded in the last 3 billing cycles
            </p>
          </div>
        </div>
        <span className="text-lg font-extrabold text-finance-red whitespace-nowrap">
          {formatCurrency(totalCreditCardSpending)}
        </span>
      </div>

      {/* ── Calculation Transparency Audit ───────────────────────────── */}
      <div className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-3">
        <h4 className="font-bold text-sm text-gray-900 dark:text-white">
          Calculation Transparency Audit
        </h4>
        <div className="divide-y divide-lightBorder dark:divide-darkBorder/60 text-xs">
          <div className="py-2.5 flex justify-between">
            <span className="text-gray-600 dark:text-darkMuted">Total Outflows (90 Days)</span>
            <span className="font-bold text-gray-900 dark:text-white">
              {formatCurrency(totalSpending90Days)}
            </span>
          </div>
          <div className="py-2.5 flex justify-between">
            <span className="text-gray-600 dark:text-darkMuted">Included Credit Card Outflows</span>
            <span className="font-semibold text-gray-700 dark:text-gray-300">
              {formatCurrency(totalCreditCardSpending)}
            </span>
          </div>
          <div className="py-2.5 flex justify-between">
            <span className="text-gray-600 dark:text-darkMuted">Included Operational Outflows</span>
            <span className="font-semibold text-gray-700 dark:text-gray-300">
              {formatCurrency(operationalSpending)}
            </span>
          </div>
          <div className="py-2.5 flex justify-between font-bold">
            <span className="text-finance-blue">Calculated Monthly Average (Outflows ÷ 3)</span>
            <span className="text-finance-blue">{formatCurrency(monthlyProjection)}</span>
          </div>
        </div>
      </div>

      {/* ── Visual Charts ────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Monthly Trend Chart */}
        <div className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-4">
          <div className="flex items-center gap-2">
            <BarChart3 size={18} className="text-finance-blue" />
            <h4 className="font-bold text-sm text-gray-900 dark:text-white">
              6-Month Cashflow Trajectory
            </h4>
          </div>
          <div className="h-64">
            <Bar
              data={monthlyChartData}
              options={{
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                  legend: {
                    labels: {
                      color: isDark ? '#8B949E' : '#57606A',
                      font: { size: 11 },
                    },
                  },
                },
                scales: {
                  x: {
                    ticks: { color: isDark ? '#8B949E' : '#57606A' },
                    grid: { display: false },
                  },
                  y: {
                    ticks: { color: isDark ? '#8B949E' : '#57606A' },
                    grid: { color: isDark ? '#21262D' : '#F0F3F6' },
                  },
                },
              }}
            />
          </div>
        </div>

        {/* Category Breakdown Donut */}
        <div className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-4">
          <div className="flex items-center gap-2">
            <PieIcon size={18} className="text-finance-gold" />
            <h4 className="font-bold text-sm text-gray-900 dark:text-white">
              Expense Category Breakdown (90 Days)
            </h4>
          </div>
          <div className="h-64 flex items-center justify-center">
            <Doughnut
              data={categoryChartData}
              options={{
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                  legend: {
                    position: 'right',
                    labels: {
                      color: isDark ? '#8B949E' : '#57606A',
                      font: { size: 11 },
                    },
                  },
                },
              }}
            />
          </div>
        </div>
      </div>
    </div>
  );
};
