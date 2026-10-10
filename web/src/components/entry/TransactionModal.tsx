import React, { useState, useEffect } from 'react';
import { X, CreditCard as CardIcon, ArrowRightLeft, Users, UserPlus, Divide } from 'lucide-react';
import {
  Transaction,
  TransactionType,
  WalletType,
  TransactionDirection,
  FamilyMember,
  CreditCard,
  CustomWallet,
  Contact,
  TransactionSplit,
} from '../../lib/types';
import { formatCurrency } from '../../lib/calculations';

interface TransactionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (
    txData: Omit<Transaction, 'id'>,
    splitsData?: Omit<TransactionSplit, 'id' | 'transaction_id'>[]
  ) => Promise<{ success: boolean; error?: string }>;
  onUpdate?: (tx: Transaction) => Promise<{ success: boolean; error?: string }>;
  editingTransaction?: Transaction | null;
  familyMembers: FamilyMember[];
  creditCards: CreditCard[];
  customWallets: CustomWallet[];
  contacts: Contact[];
  expenseCategories: string[];
  incomeCategories: string[];
}

export const TransactionModal: React.FC<TransactionModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  onUpdate,
  editingTransaction,
  familyMembers,
  creditCards,
  customWallets,
  contacts,
  expenseCategories,
  incomeCategories,
}) => {
  // Tabs: 0: Expense, 1: Income, 2: Family Transfer, 3: Wallet Transfer, 4: Borrow/Lend
  const [activeTab, setActiveTab] = useState<number>(0);

  // Common Fields
  const [amount, setAmount] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [date, setDate] = useState<string>(new Date().toISOString().slice(0, 16));
  const [category, setCategory] = useState<string>('Groceries');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Tab 0 (Expense) State
  const [expenseWalletType, setExpenseWalletType] = useState<WalletType>('EXPENSE_WALLET');
  const [selectedCreditCardId, setSelectedCreditCardId] = useState<number | null>(null);
  const [selectedCustomWalletId, setSelectedCustomWalletId] = useState<number | null>(null);
  // Split Engine
  const [isSplitting, setIsSplitting] = useState<boolean>(false);
  const [isEqualSplit, setIsEqualSplit] = useState<boolean>(true);
  const [selectedSplitEntities, setSelectedSplitEntities] = useState<Record<string, boolean>>({});
  const [customSplitAmounts, setCustomSplitAmounts] = useState<Record<string, string>>({});

  // Tab 1 (Income) State
  const [incomeWalletType, setIncomeWalletType] = useState<WalletType>('EXPENSE_WALLET');

  // Tab 2 (Family Transfer) State
  const [selectedFamilyMemberId, setSelectedFamilyMemberId] = useState<number | null>(null);
  const [familyDirection, setFamilyDirection] = useState<TransactionDirection>('GIVEN_TO_THEM');
  const [familyWalletType, setFamilyWalletType] = useState<WalletType>('EXPENSE_WALLET');

  // Tab 3 (Wallet Transfer) State
  const [transferSourceWallet, setTransferSourceWallet] = useState<WalletType>('EXPENSE_WALLET');
  const [transferDestWallet, setTransferDestWallet] = useState<WalletType>('SAVINGS_WALLET');
  const [transferSourceCustomId, setTransferSourceCustomId] = useState<number | null>(null);
  const [transferDestCustomId, setTransferDestCustomId] = useState<number | null>(null);
  const [transferRepayCardId, setTransferRepayCardId] = useState<number | null>(null);

  // Tab 4 (Borrow / Lend) State
  const [borrowLendTargetKey, setBorrowLendTargetKey] = useState<string>('');
  const [borrowLendDirection, setBorrowLendDirection] = useState<TransactionDirection>('LENT');
  const [borrowLendWallet, setBorrowLendWallet] = useState<WalletType>('EXPENSE_WALLET');
  const [borrowLendCustomId, setBorrowLendCustomId] = useState<number | null>(null);
  const [borrowLendCardId, setBorrowLendCardId] = useState<number | null>(null);

  useEffect(() => {
    if (editingTransaction) {
      setAmount(String(editingTransaction.amount));
      setNotes(editingTransaction.notes || '');
      const d = new Date(editingTransaction.date);
      setDate(new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16));
      setCategory(editingTransaction.category || 'Other');

      switch (editingTransaction.type) {
        case 'PERSONAL_EXPENSE':
          setActiveTab(0);
          setExpenseWalletType(editingTransaction.source_wallet);
          setSelectedCreditCardId(editingTransaction.credit_card_id || null);
          setSelectedCustomWalletId(editingTransaction.custom_wallet_id || null);
          break;
        case 'PERSONAL_INCOME':
          setActiveTab(1);
          setIncomeWalletType(editingTransaction.source_wallet);
          setSelectedCustomWalletId(editingTransaction.custom_wallet_id || null);
          break;
        case 'FAMILY_TRANSFER':
          setActiveTab(2);
          setSelectedFamilyMemberId(editingTransaction.family_member_id || null);
          setFamilyDirection(editingTransaction.direction || 'GIVEN_TO_THEM');
          setFamilyWalletType(editingTransaction.source_wallet);
          break;
        case 'WALLET_TRANSFER':
          setActiveTab(3);
          setTransferSourceWallet(editingTransaction.source_wallet);
          setTransferDestWallet(editingTransaction.destination_wallet || 'SAVINGS_WALLET');
          setTransferSourceCustomId(editingTransaction.custom_wallet_id || null);
          setTransferDestCustomId(editingTransaction.destination_custom_wallet_id || null);
          setTransferRepayCardId(editingTransaction.credit_card_id || null);
          break;
        case 'BORROW_LEND':
          setActiveTab(4);
          if (editingTransaction.contact_id) {
            setBorrowLendTargetKey(`C_${editingTransaction.contact_id}`);
          } else if (editingTransaction.family_member_id) {
            setBorrowLendTargetKey(`F_${editingTransaction.family_member_id}`);
          }
          setBorrowLendDirection(editingTransaction.direction || 'LENT');
          setBorrowLendWallet(editingTransaction.source_wallet);
          setBorrowLendCardId(editingTransaction.credit_card_id || null);
          setBorrowLendCustomId(editingTransaction.custom_wallet_id || null);
          break;
      }
    } else {
      // Reset defaults
      setAmount('');
      setNotes('');
      setDate(new Date().toISOString().slice(0, 16));
      setCategory(expenseCategories[0] || 'Groceries');
      setExpenseWalletType('EXPENSE_WALLET');
      setIsSplitting(false);
      setSelectedSplitEntities({});
      setCustomSplitAmounts({});
      if (creditCards.length > 0) setSelectedCreditCardId(creditCards[0].id);
      if (customWallets.length > 0) setSelectedCustomWalletId(customWallets[0].id);
      if (familyMembers.length > 0) setSelectedFamilyMemberId(familyMembers[0].id);
      if (contacts.length > 0) setBorrowLendTargetKey(`C_${contacts[0].id}`);
      else if (familyMembers.length > 0) setBorrowLendTargetKey(`F_${familyMembers[0].id}`);
    }
    setError(null);
  }, [editingTransaction, isOpen]);

  if (!isOpen) return null;

  const tabLabels = ['Expense', 'Income', 'Family Transfer', 'Wallet Transfer', 'Borrow / Lend'];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount <= 0) {
      setError('Please enter a valid amount greater than 0.');
      return;
    }

    setSubmitting(true);
    setError(null);

    const timestamp = new Date(date).getTime() || Date.now();

    try {
      if (editingTransaction && onUpdate) {
        // Update existing transaction
        const updated: Transaction = {
          ...editingTransaction,
          amount: numAmount,
          notes,
          date: timestamp,
          category: activeTab === 0 ? category : activeTab === 1 ? category : 'Other',
        };
        const res = await onUpdate(updated);
        if (!res.success) {
          setError(res.error || 'Failed to update transaction.');
          setSubmitting(false);
          return;
        }
      } else {
        // Create new transaction based on active tab
        let txData: Omit<Transaction, 'id'>;
        let splitsData: Omit<TransactionSplit, 'id' | 'transaction_id'>[] = [];

        switch (activeTab) {
          case 0: { // Personal Expense
            txData = {
              amount: numAmount,
              notes,
              date: timestamp,
              type: 'PERSONAL_EXPENSE',
              source_wallet: expenseWalletType,
              category,
              credit_card_id: expenseWalletType === 'CREDIT_CARD' ? selectedCreditCardId : null,
              custom_wallet_id: expenseWalletType === 'TEMPORARY_WALLET' ? selectedCustomWalletId : null,
            };

            // Calculate splits if enabled
            if (isSplitting) {
              const activeEntityKeys = Object.keys(selectedSplitEntities).filter(
                (k) => selectedSplitEntities[k]
              );
              if (activeEntityKeys.length > 0) {
                const totalPeople = activeEntityKeys.length + 1; // Participants + User
                const equalShare = numAmount / totalPeople;

                splitsData = activeEntityKeys.map((key) => {
                  const isContact = key.startsWith('C_');
                  const entityId = parseInt(key.slice(2), 10);
                  const splitAmt = isEqualSplit
                    ? equalShare
                    : parseFloat(customSplitAmounts[key] || '0') || equalShare;

                  return {
                    amount: splitAmt,
                    contact_id: isContact ? entityId : null,
                    family_member_id: !isContact ? entityId : null,
                    is_settled: false,
                  };
                });
              }
            }
            break;
          }
          case 1: { // Personal Income
            txData = {
              amount: numAmount,
              notes,
              date: timestamp,
              type: 'PERSONAL_INCOME',
              source_wallet: incomeWalletType,
              category,
              custom_wallet_id: incomeWalletType === 'TEMPORARY_WALLET' ? selectedCustomWalletId : null,
            };
            break;
          }
          case 2: { // Family Transfer
            if (!selectedFamilyMemberId) {
              setError('Please select a family member.');
              setSubmitting(false);
              return;
            }
            txData = {
              amount: numAmount,
              notes,
              date: timestamp,
              type: 'FAMILY_TRANSFER',
              source_wallet: familyWalletType,
              direction: familyDirection,
              family_member_id: selectedFamilyMemberId,
              category: 'Other',
              custom_wallet_id: familyWalletType === 'TEMPORARY_WALLET' ? selectedCustomWalletId : null,
            };
            break;
          }
          case 3: { // Wallet Transfer
            txData = {
              amount: numAmount,
              notes,
              date: timestamp,
              type: 'WALLET_TRANSFER',
              source_wallet: transferSourceWallet,
              destination_wallet: transferDestWallet,
              category: 'Other',
              custom_wallet_id:
                transferSourceWallet === 'TEMPORARY_WALLET' ? transferSourceCustomId : null,
              destination_custom_wallet_id:
                transferDestWallet === 'TEMPORARY_WALLET' ? transferDestCustomId : null,
              credit_card_id:
                transferDestWallet === 'CREDIT_CARD' ? transferRepayCardId : null,
            };
            break;
          }
          case 4: { // Borrow / Lend
            if (!borrowLendTargetKey) {
              setError('Please select a person.');
              setSubmitting(false);
              return;
            }
            const isContact = borrowLendTargetKey.startsWith('C_');
            const personId = parseInt(borrowLendTargetKey.slice(2), 10);

            txData = {
              amount: numAmount,
              notes,
              date: timestamp,
              type: 'BORROW_LEND',
              source_wallet: borrowLendWallet,
              direction: borrowLendDirection,
              category: 'Other',
              contact_id: isContact ? personId : null,
              family_member_id: !isContact ? personId : null,
              credit_card_id: borrowLendWallet === 'CREDIT_CARD' ? borrowLendCardId : null,
              custom_wallet_id:
                borrowLendWallet === 'TEMPORARY_WALLET' ? borrowLendCustomId : null,
            };
            break;
          }
          default:
            throw new Error('Invalid tab');
        }

        const res = await onSubmit(txData, splitsData);
        if (!res.success) {
          setError(res.error || 'Failed to save transaction.');
          setSubmitting(false);
          return;
        }
      }

      onClose();
    } catch (err: any) {
      setError(err.message || 'Error occurred');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-xl shadow-2xl overflow-hidden my-8">
        {/* Modal Header */}
        <div className="flex items-center justify-between p-5 border-b border-lightBorder dark:border-darkBorder">
          <h2 className="text-lg font-bold text-gray-900 dark:text-white">
            {editingTransaction ? 'Edit Transaction' : 'Log New Transaction'}
          </h2>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-darkCard transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        {/* Tab Navigation (only when creating new) */}
        {!editingTransaction && (
          <div className="flex overflow-x-auto border-b border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard/40 px-2">
            {tabLabels.map((lbl, idx) => (
              <button
                key={lbl}
                onClick={() => {
                  setActiveTab(idx);
                  if (idx === 1) setCategory(incomeCategories[0] || 'Salary');
                  else if (idx === 0) setCategory(expenseCategories[0] || 'Groceries');
                }}
                className={`px-4 py-3 text-xs font-semibold whitespace-nowrap transition-colors border-b-2 ${
                  activeTab === idx
                    ? 'border-finance-blue text-finance-blue dark:text-finance-blueLight bg-white dark:bg-darkSurface'
                    : 'border-transparent text-gray-600 dark:text-darkMuted hover:text-gray-900 dark:hover:text-darkText'
                }`}
              >
                {lbl}
              </button>
            ))}
          </div>
        )}

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {error && (
            <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-600 dark:text-red-400 text-xs font-medium">
              {error}
            </div>
          )}

          {/* Amount & Date Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                Amount (₹)
              </label>
              <input
                type="number"
                step="0.01"
                required
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-base font-semibold focus:outline-none focus:ring-2 focus:ring-finance-blue"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                Date & Time
              </label>
              <input
                type="datetime-local"
                required
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-xs focus:outline-none focus:ring-2 focus:ring-finance-blue"
              />
            </div>
          </div>

          {/* TAB 0: PERSONAL EXPENSE */}
          {activeTab === 0 && (
            <div className="space-y-4">
              {/* Category */}
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                  Category
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-xs focus:outline-none focus:ring-2 focus:ring-finance-blue"
                >
                  {expenseCategories.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>

              {/* Wallet Source */}
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                  Paid From Wallet
                </label>
                <div className="grid grid-cols-3 gap-2">
                  <button
                    type="button"
                    onClick={() => setExpenseWalletType('EXPENSE_WALLET')}
                    className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all ${
                      expenseWalletType === 'EXPENSE_WALLET'
                        ? 'bg-finance-blue/10 border-finance-blue text-finance-blue'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    Expense Wallet
                  </button>
                  <button
                    type="button"
                    onClick={() => setExpenseWalletType('CREDIT_CARD')}
                    className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all ${
                      expenseWalletType === 'CREDIT_CARD'
                        ? 'bg-finance-teal/10 border-finance-teal text-finance-teal'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    Credit Card
                  </button>
                  <button
                    type="button"
                    onClick={() => setExpenseWalletType('TEMPORARY_WALLET')}
                    className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all ${
                      expenseWalletType === 'TEMPORARY_WALLET'
                        ? 'bg-finance-gold/10 border-finance-gold text-finance-gold'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    Custom Wallet
                  </button>
                </div>

                {expenseWalletType === 'CREDIT_CARD' && (
                  <div className="mt-3">
                    <label className="block text-[11px] text-gray-500 dark:text-darkMuted mb-1">
                      Select Credit Card
                    </label>
                    <select
                      value={selectedCreditCardId || ''}
                      onChange={(e) => setSelectedCreditCardId(Number(e.target.value))}
                      className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-xs"
                    >
                      {creditCards.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.card_name} ({c.owner_name})
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {expenseWalletType === 'TEMPORARY_WALLET' && (
                  <div className="mt-3">
                    <label className="block text-[11px] text-gray-500 dark:text-darkMuted mb-1">
                      Select Custom Wallet
                    </label>
                    <select
                      value={selectedCustomWalletId || ''}
                      onChange={(e) => setSelectedCustomWalletId(Number(e.target.value))}
                      className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-xs"
                    >
                      {customWallets.map((cw) => (
                        <option key={cw.id} value={cw.id}>
                          {cw.name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}
              </div>

              {/* Expense Split Section */}
              <div className="p-4 rounded-xl bg-gray-50 dark:bg-darkCard/50 border border-lightBorder dark:border-darkBorder space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Divide size={16} className="text-finance-blue" />
                    <span className="text-xs font-bold text-gray-900 dark:text-white">
                      Split this expense
                    </span>
                  </div>
                  <input
                    type="checkbox"
                    checked={isSplitting}
                    onChange={(e) => setIsSplitting(e.target.checked)}
                    className="w-4 h-4 rounded text-finance-blue focus:ring-finance-blue"
                  />
                </div>

                {isSplitting && (
                  <div className="space-y-3 pt-2">
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => setIsEqualSplit(true)}
                        className={`flex-1 py-1.5 text-xs font-semibold rounded-lg border ${
                          isEqualSplit
                            ? 'bg-finance-blue text-white border-finance-blue'
                            : 'border-lightBorder dark:border-darkBorder text-gray-600 dark:text-darkMuted'
                        }`}
                      >
                        Split Equally
                      </button>
                      <button
                        type="button"
                        onClick={() => setIsEqualSplit(false)}
                        className={`flex-1 py-1.5 text-xs font-semibold rounded-lg border ${
                          !isEqualSplit
                            ? 'bg-finance-blue text-white border-finance-blue'
                            : 'border-lightBorder dark:border-darkBorder text-gray-600 dark:text-darkMuted'
                        }`}
                      >
                        Custom Amounts
                      </button>
                    </div>

                    <p className="text-[11px] text-gray-500 dark:text-darkMuted">
                      Select people to split with:
                    </p>

                    <div className="max-h-40 overflow-y-auto space-y-2 pr-1">
                      {/* Contacts */}
                      {contacts.map((c) => {
                        const key = `C_${c.id}`;
                        const isChecked = !!selectedSplitEntities[key];
                        return (
                          <div
                            key={key}
                            className="flex items-center justify-between p-2 rounded-lg bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder text-xs"
                          >
                            <label className="flex items-center gap-2 cursor-pointer flex-1">
                              <input
                                type="checkbox"
                                checked={isChecked}
                                onChange={(e) =>
                                  setSelectedSplitEntities({
                                    ...selectedSplitEntities,
                                    [key]: e.target.checked,
                                  })
                                }
                                className="w-3.5 h-3.5 rounded text-finance-blue"
                              />
                              <span className="font-medium text-gray-800 dark:text-gray-200">
                                {c.name} (Contact)
                              </span>
                            </label>
                            {isChecked && !isEqualSplit && (
                              <input
                                type="number"
                                placeholder="Amount"
                                value={customSplitAmounts[key] || ''}
                                onChange={(e) =>
                                  setCustomSplitAmounts({
                                    ...customSplitAmounts,
                                    [key]: e.target.value,
                                  })
                                }
                                className="w-24 px-2 py-1 rounded border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
                              />
                            )}
                          </div>
                        );
                      })}

                      {/* Family Members */}
                      {familyMembers.map((fm) => {
                        const key = `F_${fm.id}`;
                        const isChecked = !!selectedSplitEntities[key];
                        return (
                          <div
                            key={key}
                            className="flex items-center justify-between p-2 rounded-lg bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder text-xs"
                          >
                            <label className="flex items-center gap-2 cursor-pointer flex-1">
                              <input
                                type="checkbox"
                                checked={isChecked}
                                onChange={(e) =>
                                  setSelectedSplitEntities({
                                    ...selectedSplitEntities,
                                    [key]: e.target.checked,
                                  })
                                }
                                className="w-3.5 h-3.5 rounded text-finance-blue"
                              />
                              <span className="font-medium text-gray-800 dark:text-gray-200">
                                {fm.name} ({fm.relationship})
                              </span>
                            </label>
                            {isChecked && !isEqualSplit && (
                              <input
                                type="number"
                                placeholder="Amount"
                                value={customSplitAmounts[key] || ''}
                                onChange={(e) =>
                                  setCustomSplitAmounts({
                                    ...customSplitAmounts,
                                    [key]: e.target.value,
                                  })
                                }
                                className="w-24 px-2 py-1 rounded border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
                              />
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* TAB 1: PERSONAL INCOME */}
          {activeTab === 1 && (
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                  Income Category
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-xs focus:outline-none focus:ring-2 focus:ring-finance-blue"
                >
                  {incomeCategories.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                  Deposit Into Wallet
                </label>
                <div className="grid grid-cols-3 gap-2">
                  <button
                    type="button"
                    onClick={() => setIncomeWalletType('EXPENSE_WALLET')}
                    className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all ${
                      incomeWalletType === 'EXPENSE_WALLET'
                        ? 'bg-finance-blue/10 border-finance-blue text-finance-blue'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    Expense Wallet
                  </button>
                  <button
                    type="button"
                    onClick={() => setIncomeWalletType('SAVINGS_WALLET')}
                    className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all ${
                      incomeWalletType === 'SAVINGS_WALLET'
                        ? 'bg-finance-green/10 border-finance-green text-finance-green'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    Savings Wallet
                  </button>
                  <button
                    type="button"
                    onClick={() => setIncomeWalletType('TEMPORARY_WALLET')}
                    className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all ${
                      incomeWalletType === 'TEMPORARY_WALLET'
                        ? 'bg-finance-gold/10 border-finance-gold text-finance-gold'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    Custom Wallet
                  </button>
                </div>

                {incomeWalletType === 'TEMPORARY_WALLET' && (
                  <div className="mt-3">
                    <label className="block text-[11px] text-gray-500 dark:text-darkMuted mb-1">
                      Select Custom Wallet
                    </label>
                    <select
                      value={selectedCustomWalletId || ''}
                      onChange={(e) => setSelectedCustomWalletId(Number(e.target.value))}
                      className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-xs"
                    >
                      {customWallets.map((cw) => (
                        <option key={cw.id} value={cw.id}>
                          {cw.name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* TAB 2: FAMILY TRANSFER */}
          {activeTab === 2 && (
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                  Family Member
                </label>
                <select
                  value={selectedFamilyMemberId || ''}
                  onChange={(e) => setSelectedFamilyMemberId(Number(e.target.value))}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-xs"
                >
                  {familyMembers.map((fm) => (
                    <option key={fm.id} value={fm.id}>
                      {fm.name} ({fm.relationship})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                  Transfer Direction
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setFamilyDirection('GIVEN_TO_THEM')}
                    className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all ${
                      familyDirection === 'GIVEN_TO_THEM'
                        ? 'bg-finance-red/10 border-finance-red text-finance-red'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    Given to them (Outflow)
                  </button>
                  <button
                    type="button"
                    onClick={() => setFamilyDirection('RECEIVED_FROM_THEM')}
                    className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all ${
                      familyDirection === 'RECEIVED_FROM_THEM'
                        ? 'bg-finance-green/10 border-finance-green text-finance-green'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    Received from them (Inflow)
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                  Wallet Involved
                </label>
                <select
                  value={familyWalletType}
                  onChange={(e) => setFamilyWalletType(e.target.value as WalletType)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-xs"
                >
                  <option value="EXPENSE_WALLET">Expense Wallet</option>
                  <option value="SAVINGS_WALLET">Savings Wallet</option>
                  <option value="TEMPORARY_WALLET">Custom Wallet</option>
                </select>
              </div>
            </div>
          )}

          {/* TAB 3: WALLET TRANSFER */}
          {activeTab === 3 && (
            <div className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                    Source Wallet (From)
                  </label>
                  <select
                    value={transferSourceWallet}
                    onChange={(e) => setTransferSourceWallet(e.target.value as WalletType)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-xs"
                  >
                    <option value="EXPENSE_WALLET">Expense Wallet</option>
                    <option value="SAVINGS_WALLET">Savings Wallet</option>
                    <option value="TEMPORARY_WALLET">Custom Wallet</option>
                  </select>
                  {transferSourceWallet === 'TEMPORARY_WALLET' && (
                    <select
                      value={transferSourceCustomId || ''}
                      onChange={(e) => setTransferSourceCustomId(Number(e.target.value))}
                      className="w-full mt-2 px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs"
                    >
                      {customWallets.map((cw) => (
                        <option key={cw.id} value={cw.id}>
                          {cw.name}
                        </option>
                      ))}
                    </select>
                  )}
                </div>

                <div>
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                    Destination Wallet (To)
                  </label>
                  <select
                    value={transferDestWallet}
                    onChange={(e) => setTransferDestWallet(e.target.value as WalletType)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-xs"
                  >
                    <option value="EXPENSE_WALLET">Expense Wallet</option>
                    <option value="SAVINGS_WALLET">Savings Wallet</option>
                    <option value="CREDIT_CARD">Credit Card (Repayment)</option>
                    <option value="TEMPORARY_WALLET">Custom Wallet</option>
                  </select>

                  {transferDestWallet === 'CREDIT_CARD' && (
                    <select
                      value={transferRepayCardId || ''}
                      onChange={(e) => setTransferRepayCardId(Number(e.target.value))}
                      className="w-full mt-2 px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs"
                    >
                      {creditCards.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.card_name} ({c.owner_name})
                        </option>
                      ))}
                    </select>
                  )}

                  {transferDestWallet === 'TEMPORARY_WALLET' && (
                    <select
                      value={transferDestCustomId || ''}
                      onChange={(e) => setTransferDestCustomId(Number(e.target.value))}
                      className="w-full mt-2 px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs"
                    >
                      {customWallets.map((cw) => (
                        <option key={cw.id} value={cw.id}>
                          {cw.name}
                        </option>
                      ))}
                    </select>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: BORROW / LEND */}
          {activeTab === 4 && (
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                  Person (Contact or Family)
                </label>
                <select
                  value={borrowLendTargetKey}
                  onChange={(e) => setBorrowLendTargetKey(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-xs"
                >
                  <optgroup label="Contacts">
                    {contacts.map((c) => (
                      <option key={`C_${c.id}`} value={`C_${c.id}`}>
                        {c.name}
                      </option>
                    ))}
                  </optgroup>
                  <optgroup label="Family Members">
                    {familyMembers.map((fm) => (
                      <option key={`F_${fm.id}`} value={`F_${fm.id}`}>
                        {fm.name} ({fm.relationship})
                      </option>
                    ))}
                  </optgroup>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                  Direction
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setBorrowLendDirection('LENT')}
                    className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all ${
                      borrowLendDirection === 'LENT'
                        ? 'bg-finance-red/10 border-finance-red text-finance-red'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    I Lent (Given to them)
                  </button>
                  <button
                    type="button"
                    onClick={() => setBorrowLendDirection('BORROWED')}
                    className={`py-2 px-3 rounded-xl text-xs font-semibold border transition-all ${
                      borrowLendDirection === 'BORROWED'
                        ? 'bg-finance-green/10 border-finance-green text-finance-green'
                        : 'border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted'
                    }`}
                  >
                    I Borrowed (Received from them)
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                  Wallet Used
                </label>
                <select
                  value={borrowLendWallet}
                  onChange={(e) => setBorrowLendWallet(e.target.value as WalletType)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-xs"
                >
                  <option value="EXPENSE_WALLET">Expense Wallet</option>
                  <option value="SAVINGS_WALLET">Savings Wallet</option>
                  <option value="CREDIT_CARD">Credit Card</option>
                  <option value="TEMPORARY_WALLET">Custom Wallet</option>
                </select>

                {borrowLendWallet === 'CREDIT_CARD' && (
                  <select
                    value={borrowLendCardId || ''}
                    onChange={(e) => setBorrowLendCardId(Number(e.target.value))}
                    className="w-full mt-2 px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs"
                  >
                    {creditCards.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.card_name} ({c.owner_name})
                      </option>
                    ))}
                  </select>
                )}
              </div>
            </div>
          )}

          {/* Notes / Description */}
          <div>
            <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
              Notes / Description
            </label>
            <input
              type="text"
              placeholder="e.g. Dinner with team, Grocery shopping"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-gray-900 dark:text-white text-xs focus:outline-none focus:ring-2 focus:ring-finance-blue"
            />
          </div>

          {/* Modal Actions */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-lightBorder dark:border-darkBorder">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted text-xs font-semibold hover:bg-gray-100 dark:hover:bg-darkCard transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-5 py-2.5 rounded-xl bg-finance-blue hover:bg-finance-blueDark text-white text-xs font-bold shadow-md transition-all disabled:opacity-50"
            >
              {submitting
                ? 'Saving...'
                : editingTransaction
                ? 'Update Transaction'
                : 'Save Transaction'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
