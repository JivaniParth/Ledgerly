import React, { useState } from 'react';
import { Plus, RefreshCw, Sun, Moon, Flame, CheckCircle2, Calculator } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { CalculatorModal } from '../common/CalculatorModal';

interface HeaderProps {
  streak: number;
  syncing: boolean;
  onRefresh: () => void;
  onOpenTransactionModal: () => void;
  title: string;
}

export const Header: React.FC<HeaderProps> = ({
  streak,
  syncing,
  onRefresh,
  onOpenTransactionModal,
  title,
}) => {
  const { isDark, setTheme } = useTheme();
  const { userName } = useAuth();
  const [showCalculator, setShowCalculator] = useState(false);

  return (
    <header className="h-16 px-6 bg-white/80 dark:bg-darkSurface/80 backdrop-blur-md border-b border-lightBorder dark:border-darkBorder flex items-center justify-between sticky top-0 z-20">
      <div className="flex items-center gap-3">
        <h1 className="text-xl font-bold tracking-tight text-gray-900 dark:text-white">
          {title}
        </h1>

        {/* Streak Badge */}
        {streak >= 2 ? (
          <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-600 dark:text-amber-400 text-xs font-semibold">
            <Flame size={14} className="fill-amber-500 text-amber-500 animate-pulse" />
            <span>{streak} day streak!</span>
          </div>
        ) : streak === 1 ? (
          <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs font-medium">
            <CheckCircle2 size={13} />
            <span>Logged today!</span>
          </div>
        ) : null}
      </div>

      <div className="flex items-center gap-3">
        {/* Financial Calculator */}
        <button
          onClick={() => setShowCalculator(true)}
          className="p-2 rounded-xl text-gray-500 hover:text-gray-900 dark:text-darkMuted dark:hover:text-darkText hover:bg-gray-100 dark:hover:bg-darkCard transition-colors"
          title="Open Financial Calculator"
        >
          <Calculator size={17} />
        </button>

        {/* Sync status */}
        <button
          onClick={onRefresh}
          disabled={syncing}
          className="p-2 rounded-xl text-gray-500 hover:text-gray-900 dark:text-darkMuted dark:hover:text-darkText hover:bg-gray-100 dark:hover:bg-darkCard transition-colors disabled:opacity-50"
          title={syncing ? 'Syncing with Supabase...' : 'Sync with cloud'}
        >
          <RefreshCw size={17} className={syncing ? 'animate-spin text-finance-blue' : ''} />
        </button>

        {/* Dark/Light mode toggle */}
        <button
          onClick={() => setTheme(isDark ? 'light' : 'dark')}
          className="p-2 rounded-xl text-gray-500 hover:text-gray-900 dark:text-darkMuted dark:hover:text-darkText hover:bg-gray-100 dark:hover:bg-darkCard transition-colors"
          title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
        >
          {isDark ? <Sun size={17} /> : <Moon size={17} />}
        </button>

        {/* Quick Add Transaction Button */}
        <button
          onClick={onOpenTransactionModal}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-finance-blue hover:bg-finance-blueDark text-white text-sm font-semibold shadow-sm transition-all transform active:scale-95"
        >
          <Plus size={16} className="stroke-[2.5]" />
          <span>Log Transaction</span>
        </button>

        {/* User initials circle */}
        <div className="w-8 h-8 rounded-full bg-finance-blue text-white flex items-center justify-center font-bold text-xs ring-2 ring-finance-blue/20">
          {userName.charAt(0).toUpperCase()}
        </div>
      </div>

      {/* Financial Calculator Modal */}
      <CalculatorModal
        isOpen={showCalculator}
        onClose={() => setShowCalculator(false)}
        title="Ledgerly Financial Calculator"
      />
    </header>
  );
};
