import React, { useState, useMemo } from 'react';
import {
  Search,
  Filter,
  Download,
  Trash2,
  Edit2,
  Divide,
  ArrowUpRight,
  ArrowDownLeft,
  ChevronDown,
  X,
} from 'lucide-react';
import * as XLSX from 'xlsx';
import {
  Transaction,
  FamilyMember,
  CreditCard,
  CustomWallet,
  Contact,
  TransactionSplit,
  WalletType,
  TransactionType,
} from '../../lib/types';
import { formatCurrency, formatIndianDate, formatIndianTime, formatIndianDateTime } from '../../lib/calculations';

interface TransactionsViewProps {
  transactions: Transaction[];
  familyMembers: FamilyMember[];
  creditCards: CreditCard[];
  customWallets: CustomWallet[];
  contacts: Contact[];
  splits: TransactionSplit[];
  expenseCategories: string[];
  incomeCategories: string[];
  onEditTransaction: (tx: Transaction) => void;
  onDeleteTransaction: (id: number) => Promise<any>;
  onSplitTransaction: (
    txId: number,
    splits: Omit<TransactionSplit, 'id' | 'transaction_id'>[]
  ) => Promise<any>;
  initialFamilyMemberFilter?: number | null;
  initialContactFilter?: number | null;
}

export const TransactionsView: React.FC<TransactionsViewProps> = ({
  transactions,
  familyMembers,
  creditCards,
  customWallets,
  contacts,
  splits,
  expenseCategories,
  incomeCategories,
  onEditTransaction,
  onDeleteTransaction,
  onSplitTransaction,
  initialFamilyMemberFilter,
  initialContactFilter,
}) => {
  // Search and Filter States
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedWallet, setSelectedWallet] = useState<string>('ALL');
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [selectedMember, setSelectedMember] = useState<string>(
    initialFamilyMemberFilter ? String(initialFamilyMemberFilter) : 'ALL'
  );
  const [selectedContact, setSelectedContact] = useState<string>(
    initialContactFilter ? String(initialContactFilter) : 'ALL'
  );
  const [datePeriod, setDatePeriod] = useState<string>('ALL'); // ALL, TODAY, WEEK, MONTH, 90DAYS

  // Deletion Confirm state
  const [deletingId, setDeletingId] = useState<number | null>(null);

  // Retroactive Split state
  const [splittingTx, setSplittingTx] = useState<Transaction | null>(null);
  const [retroEqualSplit, setRetroEqualSplit] = useState<boolean>(true);
  const [selectedRetroSplitEntities, setSelectedRetroSplitEntities] = useState<Record<string, boolean>>({});
  const [customRetroSplitAmounts, setCustomRetroSplitAmounts] = useState<Record<string, string>>({});
  const [retroSubmitting, setRetroSubmitting] = useState(false);

  // Filter Logic
  const filteredTransactions = useMemo(() => {
    const now = Date.now();
    const oneDay = 24 * 60 * 60 * 1000;

    return transactions.filter((t) => {
      // Search
      if (searchTerm) {
        const term = searchTerm.toLowerCase();
        const matchesNote = (t.notes || '').toLowerCase().includes(term);
        const matchesCat = (t.category || '').toLowerCase().includes(term);
        const matchesAmt = String(t.amount).includes(term);
        if (!matchesNote && !matchesCat && !matchesAmt) return false;
      }

      // Wallet
      if (selectedWallet !== 'ALL') {
        if (selectedWallet.startsWith('CC_')) {
          const cardId = parseInt(selectedWallet.replace('CC_', ''), 10);
          if (t.credit_card_id !== cardId) return false;
        } else if (selectedWallet.startsWith('CW_')) {
          const wId = parseInt(selectedWallet.replace('CW_', ''), 10);
          if (t.custom_wallet_id !== wId && t.destination_custom_wallet_id !== wId) return false;
        } else {
          if (t.source_wallet !== selectedWallet && t.destination_wallet !== selectedWallet) {
            return false;
          }
        }
      }

      // Type
      if (selectedType !== 'ALL' && t.type !== selectedType) {
        return false;
      }

      // Category
      if (selectedCategory !== 'ALL' && t.category !== selectedCategory) {
        return false;
      }

      // Member
      if (selectedMember !== 'ALL') {
        const mId = parseInt(selectedMember, 10);
        if (t.family_member_id !== mId) return false;
      }

      // Contact
      if (selectedContact !== 'ALL') {
        const cId = parseInt(selectedContact, 10);
        if (t.contact_id !== cId) return false;
      }

      // Date Range
      if (datePeriod === 'TODAY') {
        const startOfDay = new Date().setHours(0, 0, 0, 0);
        if (t.date < startOfDay) return false;
      } else if (datePeriod === 'WEEK') {
        const startOfWeek = now - 7 * oneDay;
        if (t.date < startOfWeek) return false;
      } else if (datePeriod === 'MONTH') {
        const startOfMonth = new Date(new Date().getFullYear(), new Date().getMonth(), 1).getTime();
        if (t.date < startOfMonth) return false;
      } else if (datePeriod === '90DAYS') {
        const startOf90Days = now - 90 * oneDay;
        if (t.date < startOf90Days) return false;
      }

      return true;
    });
  }, [
    transactions,
    searchTerm,
    selectedWallet,
    selectedType,
    selectedCategory,
    selectedMember,
    selectedContact,
    datePeriod,
  ]);

  // Totals for current filtered list
  const filteredInflow = useMemo(() => {
    return filteredTransactions
      .filter(
        (t) =>
          t.type === 'PERSONAL_INCOME' ||
          (t.type === 'FAMILY_TRANSFER' && t.direction === 'RECEIVED_FROM_THEM') ||
          (t.type === 'BORROW_LEND' && t.direction === 'BORROWED')
      )
      .reduce((sum, t) => sum + Math.abs(t.amount || 0), 0);
  }, [filteredTransactions]);

  const filteredOutflow = useMemo(() => {
    return filteredTransactions
      .filter(
        (t) =>
          t.type === 'PERSONAL_EXPENSE' ||
          (t.type === 'FAMILY_TRANSFER' && t.direction === 'GIVEN_TO_THEM') ||
          (t.type === 'BORROW_LEND' && t.direction === 'LENT')
      )
      .reduce((sum, t) => sum + Math.abs(t.amount || 0), 0);
  }, [filteredTransactions]);

  // Export to Excel / CSV
  const handleExport = (format: 'xlsx' | 'csv') => {
    const exportData = filteredTransactions.map((t) => ({
      ID: t.id,
      Date: formatIndianDateTime(t.date),
      Type: t.type,
      Category: t.category,
      Amount: t.amount,
      Wallet: t.source_wallet,
      Notes: t.notes || '',
      Direction: t.direction || '',
    }));

    const worksheet = XLSX.utils.json_to_sheet(exportData);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Transactions');

    if (format === 'csv') {
      XLSX.writeFile(workbook, `Ledgerly_Transactions_${new Date().toISOString().slice(0, 10)}.csv`);
    } else {
      XLSX.writeFile(workbook, `Ledgerly_Transactions_${new Date().toISOString().slice(0, 10)}.xlsx`);
    }
  };

  const handleExecuteRetroactiveSplit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!splittingTx) return;

    setRetroSubmitting(true);
    const activeKeys = Object.keys(selectedRetroSplitEntities).filter(
      (k) => selectedRetroSplitEntities[k]
    );

    if (activeKeys.length > 0) {
      const totalPeople = activeKeys.length + 1;
      const equalShare = splittingTx.amount / totalPeople;

      const splitsData: Omit<TransactionSplit, 'id' | 'transaction_id'>[] = activeKeys.map((key) => {
        const isContact = key.startsWith('C_');
        const entityId = parseInt(key.slice(2), 10);
        const splitAmt = retroEqualSplit
          ? equalShare
          : parseFloat(customRetroSplitAmounts[key] || '0') || equalShare;

        return {
          amount: splitAmt,
          contact_id: isContact ? entityId : null,
          family_member_id: !isContact ? entityId : null,
          is_settled: false,
        };
      });

      await onSplitTransaction(splittingTx.id, splitsData);
    }

    setRetroSubmitting(false);
    setSplittingTx(null);
    setSelectedRetroSplitEntities({});
    setCustomRetroSplitAmounts({});
  };

  return (
    <div className="space-y-5">
      {/* ── Summary KPI Strip for Current Filters ─────────────────────── */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="p-4 rounded-xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder">
          <p className="text-xs text-gray-500 dark:text-darkMuted font-medium">Filtered Inflows</p>
          <p className="text-lg font-bold text-finance-green">{formatCurrency(filteredInflow)}</p>
        </div>
        <div className="p-4 rounded-xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder">
          <p className="text-xs text-gray-500 dark:text-darkMuted font-medium">Filtered Outflows</p>
          <p className="text-lg font-bold text-finance-red">{formatCurrency(filteredOutflow)}</p>
        </div>
        <div className="p-4 rounded-xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder flex items-center justify-between">
          <div>
            <p className="text-xs text-gray-500 dark:text-darkMuted font-medium">Total Records</p>
            <p className="text-lg font-bold text-gray-900 dark:text-white">
              {filteredTransactions.length}
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => handleExport('xlsx')}
              className="p-2 rounded-lg bg-gray-100 dark:bg-darkCard hover:bg-gray-200 dark:hover:bg-darkBorder text-gray-700 dark:text-gray-300 text-xs font-semibold flex items-center gap-1.5 transition-colors"
              title="Export Excel"
            >
              <Download size={15} />
              <span>Excel</span>
            </button>
            <button
              onClick={() => handleExport('csv')}
              className="p-2 rounded-lg bg-gray-100 dark:bg-darkCard hover:bg-gray-200 dark:hover:bg-darkBorder text-gray-700 dark:text-gray-300 text-xs font-semibold flex items-center gap-1.5 transition-colors"
              title="Export CSV"
            >
              <Download size={15} />
              <span>CSV</span>
            </button>
          </div>
        </div>
      </div>

      {/* ── Filter & Search Toolbar ──────────────────────────────────── */}
      <div className="p-4 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder space-y-3">
        {/* Search Input */}
        <div className="relative">
          <Search size={17} className="absolute left-3.5 top-3 text-gray-400" />
          <input
            type="text"
            placeholder="Search transactions by note, category, or amount..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-medium text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-finance-blue"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute right-3.5 top-2.5 text-gray-400 hover:text-gray-600"
            >
              <X size={15} />
            </button>
          )}
        </div>

        {/* Filter Dropdowns Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2 pt-1 text-xs">
          {/* Wallet Filter */}
          <select
            value={selectedWallet}
            onChange={(e) => setSelectedWallet(e.target.value)}
            className="px-2.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-700 dark:text-gray-300 font-medium"
          >
            <option value="ALL">All Wallets</option>
            <option value="EXPENSE_WALLET">Expense Wallet</option>
            <option value="SAVINGS_WALLET">Savings Wallet</option>
            <optgroup label="Credit Cards">
              {creditCards.map((c) => (
                <option key={c.id} value={`CC_${c.id}`}>
                  {c.card_name}
                </option>
              ))}
            </optgroup>
            <optgroup label="Custom Wallets">
              {customWallets.map((cw) => (
                <option key={cw.id} value={`CW_${cw.id}`}>
                  {cw.name}
                </option>
              ))}
            </optgroup>
          </select>

          {/* Type Filter */}
          <select
            value={selectedType}
            onChange={(e) => setSelectedType(e.target.value)}
            className="px-2.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-700 dark:text-gray-300 font-medium"
          >
            <option value="ALL">All Types</option>
            <option value="PERSONAL_EXPENSE">Personal Expense</option>
            <option value="PERSONAL_INCOME">Personal Income</option>
            <option value="FAMILY_TRANSFER">Family Transfer</option>
            <option value="WALLET_TRANSFER">Wallet Transfer</option>
            <option value="BORROW_LEND">Borrow / Lend</option>
          </select>

          {/* Category Filter */}
          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="px-2.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-700 dark:text-gray-300 font-medium"
          >
            <option value="ALL">All Categories</option>
            {Array.from(new Set([...expenseCategories, ...incomeCategories])).map((cat) => (
              <option key={cat} value={cat}>
                {cat}
              </option>
            ))}
          </select>

          {/* Family Member Filter */}
          <select
            value={selectedMember}
            onChange={(e) => setSelectedMember(e.target.value)}
            className="px-2.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-700 dark:text-gray-300 font-medium"
          >
            <option value="ALL">All Members</option>
            {familyMembers.map((fm) => (
              <option key={fm.id} value={String(fm.id)}>
                {fm.name}
              </option>
            ))}
          </select>

          {/* Contact Filter */}
          <select
            value={selectedContact}
            onChange={(e) => setSelectedContact(e.target.value)}
            className="px-2.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-700 dark:text-gray-300 font-medium"
          >
            <option value="ALL">All Contacts</option>
            {contacts.map((c) => (
              <option key={c.id} value={String(c.id)}>
                {c.name}
              </option>
            ))}
          </select>

          {/* Date Period Preset */}
          <select
            value={datePeriod}
            onChange={(e) => setDatePeriod(e.target.value)}
            className="px-2.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-700 dark:text-gray-300 font-medium"
          >
            <option value="ALL">All Time</option>
            <option value="TODAY">Today</option>
            <option value="WEEK">Last 7 Days</option>
            <option value="MONTH">This Month</option>
            <option value="90DAYS">Last 90 Days</option>
          </select>
        </div>
      </div>

      {/* ── Transactions Data Table ──────────────────────────────────── */}
      <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-gray-50 dark:bg-darkCard/60 border-b border-lightBorder dark:border-darkBorder text-gray-500 dark:text-darkMuted uppercase font-semibold text-[11px]">
              <tr>
                <th className="py-3 px-4">Date & Time</th>
                <th className="py-3 px-4">Notes / Details</th>
                <th className="py-3 px-4">Category</th>
                <th className="py-3 px-4">Type</th>
                <th className="py-3 px-4">Wallet</th>
                <th className="py-3 px-4 text-right">Amount</th>
                <th className="py-3 px-4 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-lightBorder dark:divide-darkBorder/60">
              {filteredTransactions.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-gray-500 dark:text-darkMuted">
                    No transactions match your current filters.
                  </td>
                </tr>
              ) : (
                filteredTransactions.map((tx) => {
                  const isIncome =
                    tx.type === 'PERSONAL_INCOME' ||
                    (tx.type === 'FAMILY_TRANSFER' && tx.direction === 'RECEIVED_FROM_THEM') ||
                    (tx.type === 'BORROW_LEND' && tx.direction === 'BORROWED');

                  const splitsForThis = splits.filter((s) => s.transaction_id === tx.id);

                  return (
                    <tr
                      key={tx.id}
                      className="hover:bg-gray-50 dark:hover:bg-darkCard/40 transition-colors"
                    >
                      <td className="py-3 px-4 text-gray-600 dark:text-darkMuted whitespace-nowrap">
                        <span className="font-semibold text-gray-900 dark:text-gray-100 block">
                          {formatIndianDate(tx.date)}
                        </span>
                        <span className="text-[10px] block opacity-70">
                          {formatIndianTime(tx.date)}
                        </span>
                      </td>

                      <td className="py-3 px-4 font-semibold text-gray-900 dark:text-white max-w-xs truncate">
                        {tx.notes || tx.category}
                        {splitsForThis.length > 0 && (
                          <span className="ml-2 inline-flex items-center px-1.5 py-0.5 rounded text-[10px] bg-blue-50 text-finance-blue dark:bg-blue-950/40">
                            Split ({splitsForThis.length})
                          </span>
                        )}
                      </td>

                      <td className="py-3 px-4 whitespace-nowrap">
                        <span className="px-2 py-0.5 rounded-full bg-gray-100 dark:bg-darkCard text-gray-700 dark:text-gray-300 font-medium">
                          {tx.category}
                        </span>
                      </td>

                      <td className="py-3 px-4 whitespace-nowrap">
                        <span
                          className={`px-2 py-0.5 rounded-full font-medium ${
                            tx.type === 'PERSONAL_EXPENSE'
                              ? 'bg-red-50 text-finance-red dark:bg-red-950/40'
                              : tx.type === 'PERSONAL_INCOME'
                              ? 'bg-emerald-50 text-finance-green dark:bg-emerald-950/40'
                              : tx.type === 'WALLET_TRANSFER'
                              ? 'bg-blue-50 text-finance-blue dark:bg-blue-950/40'
                              : 'bg-purple-50 text-purple-600 dark:bg-purple-950/40'
                          }`}
                        >
                          {tx.type.replace('_', ' ')}
                        </span>
                      </td>

                      <td className="py-3 px-4 whitespace-nowrap text-gray-600 dark:text-darkMuted">
                        {tx.source_wallet.replace('_WALLET', '')}
                      </td>

                      <td
                        className={`py-3 px-4 text-right font-bold text-sm whitespace-nowrap ${
                          isIncome ? 'text-finance-green' : 'text-finance-red'
                        }`}
                      >
                        {isIncome ? '+' : '-'}
                        {formatCurrency(tx.amount)}
                      </td>

                      <td className="py-3 px-4 text-center whitespace-nowrap">
                        <div className="flex items-center justify-center gap-1.5">
                          {tx.type === 'PERSONAL_EXPENSE' && splitsForThis.length === 0 && (
                            <button
                              onClick={() => {
                                setSplittingTx(tx);
                                setSelectedRetroSplitEntities({});
                                setCustomRetroSplitAmounts({});
                              }}
                              className="p-1 rounded text-gray-400 hover:text-finance-blue hover:bg-gray-100 dark:hover:bg-darkCard"
                              title="Split Retroactively"
                            >
                              <Divide size={15} />
                            </button>
                          )}
                          <button
                            onClick={() => onEditTransaction(tx)}
                            className="p-1 rounded text-gray-400 hover:text-finance-blue hover:bg-gray-100 dark:hover:bg-darkCard"
                            title="Edit"
                          >
                            <Edit2 size={15} />
                          </button>
                          <button
                            onClick={() => setDeletingId(tx.id)}
                            className="p-1 rounded text-gray-400 hover:text-red-500 hover:bg-gray-100 dark:hover:bg-darkCard"
                            title="Delete"
                          >
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Deletion Confirmation Modal ───────────────────────────────── */}
      {deletingId && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <h3 className="font-bold text-base text-gray-900 dark:text-white">Delete Transaction</h3>
            <p className="text-xs text-gray-500 dark:text-darkMuted">
              Are you sure you want to permanently delete this transaction? This action will remove
              it from cloud storage and adjust your wallet balance.
            </p>
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setDeletingId(null)}
                className="px-3.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  await onDeleteTransaction(deletingId);
                  setDeletingId(null);
                }}
                className="px-4 py-1.5 rounded-lg bg-finance-red text-white text-xs font-bold shadow hover:bg-finance-redDark"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Retroactive Split Modal ───────────────────────────────────── */}
      {splittingTx && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-base text-gray-900 dark:text-white">
                Split Expense Retroactively
              </h3>
              <button onClick={() => setSplittingTx(null)}>
                <X size={18} />
              </button>
            </div>

            <p className="text-xs text-gray-500 dark:text-darkMuted">
              Splitting {formatCurrency(splittingTx.amount)} ({splittingTx.notes || splittingTx.category})
            </p>

            <form onSubmit={handleExecuteRetroactiveSplit} className="space-y-4">
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setRetroEqualSplit(true)}
                  className={`flex-1 py-1.5 text-xs font-semibold rounded-lg border ${
                    retroEqualSplit
                      ? 'bg-finance-blue text-white border-finance-blue'
                      : 'border-lightBorder dark:border-darkBorder text-gray-600 dark:text-darkMuted'
                  }`}
                >
                  Split Equally
                </button>
                <button
                  type="button"
                  onClick={() => setRetroEqualSplit(false)}
                  className={`flex-1 py-1.5 text-xs font-semibold rounded-lg border ${
                    !retroEqualSplit
                      ? 'bg-finance-blue text-white border-finance-blue'
                      : 'border-lightBorder dark:border-darkBorder text-gray-600 dark:text-darkMuted'
                  }`}
                >
                  Custom Amounts
                </button>
              </div>

              <div className="max-h-52 overflow-y-auto space-y-2 pr-1">
                {contacts.map((c) => {
                  const key = `C_${c.id}`;
                  const isChecked = !!selectedRetroSplitEntities[key];
                  return (
                    <div
                      key={key}
                      className="flex items-center justify-between p-2 rounded-lg bg-gray-50 dark:bg-darkCard border border-lightBorder dark:border-darkBorder text-xs"
                    >
                      <label className="flex items-center gap-2 cursor-pointer flex-1">
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={(e) =>
                            setSelectedRetroSplitEntities({
                              ...selectedRetroSplitEntities,
                              [key]: e.target.checked,
                            })
                          }
                          className="w-3.5 h-3.5 rounded text-finance-blue"
                        />
                        <span>{c.name} (Contact)</span>
                      </label>
                      {isChecked && !retroEqualSplit && (
                        <input
                          type="number"
                          placeholder="Amount"
                          value={customRetroSplitAmounts[key] || ''}
                          onChange={(e) =>
                            setCustomRetroSplitAmounts({
                              ...customRetroSplitAmounts,
                              [key]: e.target.value,
                            })
                          }
                          className="w-24 px-2 py-1 rounded border border-lightBorder dark:border-darkBorder bg-white dark:bg-darkSurface text-xs font-semibold"
                        />
                      )}
                    </div>
                  );
                })}

                {familyMembers.map((fm) => {
                  const key = `F_${fm.id}`;
                  const isChecked = !!selectedRetroSplitEntities[key];
                  return (
                    <div
                      key={key}
                      className="flex items-center justify-between p-2 rounded-lg bg-gray-50 dark:bg-darkCard border border-lightBorder dark:border-darkBorder text-xs"
                    >
                      <label className="flex items-center gap-2 cursor-pointer flex-1">
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={(e) =>
                            setSelectedRetroSplitEntities({
                              ...selectedRetroSplitEntities,
                              [key]: e.target.checked,
                            })
                          }
                          className="w-3.5 h-3.5 rounded text-finance-blue"
                        />
                        <span>
                          {fm.name} ({fm.relationship})
                        </span>
                      </label>
                      {isChecked && !retroEqualSplit && (
                        <input
                          type="number"
                          placeholder="Amount"
                          value={customRetroSplitAmounts[key] || ''}
                          onChange={(e) =>
                            setCustomRetroSplitAmounts({
                              ...customRetroSplitAmounts,
                              [key]: e.target.value,
                            })
                          }
                          className="w-24 px-2 py-1 rounded border border-lightBorder dark:border-darkBorder bg-white dark:bg-darkSurface text-xs font-semibold"
                        />
                      )}
                    </div>
                  );
                })}
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setSplittingTx(null)}
                  className="px-3 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={retroSubmitting}
                  className="px-4 py-1.5 rounded-lg bg-finance-blue text-white text-xs font-bold shadow disabled:opacity-50"
                >
                  {retroSubmitting ? 'Splitting...' : 'Apply Split'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
