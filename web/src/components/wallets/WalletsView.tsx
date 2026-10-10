import React, { useState } from 'react';
import {
  Wallet,
  CreditCard as CardIcon,
  Users,
  Plus,
  ArrowRightLeft,
  Edit2,
  Trash2,
  DollarSign,
  ChevronRight,
  X,
  History,
} from 'lucide-react';
import {
  CreditCard,
  CustomWallet,
  Contact,
  LedgerEntity,
  Transaction,
  WalletType,
} from '../../lib/types';
import {
  formatCurrency,
  calculateCustomWalletBalance,
  calculateCreditCardSpend,
} from '../../lib/calculations';
import { useTheme } from '../../context/ThemeContext';

interface WalletsViewProps {
  expenseBalance: number;
  savingsBalance: number;
  transactions: Transaction[];
  creditCards: CreditCard[];
  customWallets: CustomWallet[];
  contacts: Contact[];
  ledgerEntities: LedgerEntity[];
  onAddCreditCard: (card: Omit<CreditCard, 'id'>) => Promise<any>;
  onUpdateCreditCard: (card: CreditCard) => Promise<any>;
  onDeleteCreditCard: (id: number) => Promise<any>;
  onAddCustomWallet: (wallet: Omit<CustomWallet, 'id'>) => Promise<any>;
  onUpdateCustomWallet: (wallet: CustomWallet) => Promise<any>;
  onDeleteCustomWallet: (id: number) => Promise<any>;
  onAddContact: (name: string) => Promise<any>;
  onUpdateContact: (contact: Contact) => Promise<any>;
  onDeleteContact: (id: number) => Promise<any>;
  onSettleUpEntity: (
    entity: LedgerEntity,
    walletType: WalletType,
    customWalletId?: number | null
  ) => Promise<any>;
  onNavigateToFilteredTransactions: (isFamily: boolean, id: number) => void;
  onOpenTransferModal: () => void;
}

export const WalletsView: React.FC<WalletsViewProps> = ({
  expenseBalance,
  savingsBalance,
  transactions,
  creditCards,
  customWallets,
  contacts,
  ledgerEntities,
  onAddCreditCard,
  onUpdateCreditCard,
  onDeleteCreditCard,
  onAddCustomWallet,
  onUpdateCustomWallet,
  onDeleteCustomWallet,
  onAddContact,
  onUpdateContact,
  onDeleteContact,
  onSettleUpEntity,
  onNavigateToFilteredTransactions,
  onOpenTransferModal,
}) => {
  const { expenseWalletColor } = useTheme();

  // Tabs: 0: Standard, 1: Custom Wallets, 2: Credit Cards, 3: Debt Ledger
  const [activeSubTab, setActiveSubTab] = useState<number>(0);

  // Modals state
  const [showAddCard, setShowAddCard] = useState(false);
  const [editingCard, setEditingCard] = useState<CreditCard | null>(null);
  const [showAddWallet, setShowAddWallet] = useState(false);
  const [editingWallet, setEditingWallet] = useState<CustomWallet | null>(null);
  const [showAddContact, setShowAddContact] = useState(false);
  const [editingContact, setEditingContact] = useState<Contact | null>(null);
  const [settlingEntity, setSettlingEntity] = useState<LedgerEntity | null>(null);
  const [settleWallet, setSettleWallet] = useState<WalletType>('EXPENSE_WALLET');

  // Form states
  const [cardName, setCardName] = useState('');
  const [cardOwner, setCardOwner] = useState('');
  const [cardLimit, setCardLimit] = useState('');
  const [cardColor, setCardColor] = useState('#0097A7');

  const [walletName, setWalletName] = useState('');
  const [walletColor, setWalletColor] = useState('#F5A623');

  const [contactName, setContactName] = useState('');

  // Settle up handler
  const handleSettleUp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!settlingEntity) return;
    await onSettleUpEntity(settlingEntity, settleWallet);
    setSettlingEntity(null);
  };

  return (
    <div className="space-y-6">
      {/* ── Sub Navigation Tabs ───────────────────────────────────────── */}
      <div className="flex border-b border-lightBorder dark:border-darkBorder bg-white dark:bg-darkSurface rounded-2xl p-1.5 shadow-sm overflow-x-auto">
        {[
          { label: 'Standard Wallets', icon: <Wallet size={16} /> },
          { label: 'Custom Wallets', icon: <Wallet size={16} /> },
          { label: 'Credit Cards', icon: <CardIcon size={16} /> },
          { label: 'Borrow & Lend Ledger', icon: <Users size={16} /> },
        ].map((tab, idx) => (
          <button
            key={tab.label}
            onClick={() => setActiveSubTab(idx)}
            className={`flex-1 min-w-[140px] flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs font-bold transition-all ${
              activeSubTab === idx
                ? 'bg-finance-blue text-white shadow-sm'
                : 'text-gray-600 dark:text-darkMuted hover:text-gray-900 dark:hover:text-darkText hover:bg-gray-100 dark:hover:bg-darkCard'
            }`}
          >
            {tab.icon}
            <span>{tab.label}</span>
          </button>
        ))}
      </div>

      {/* ── TAB 0: STANDARD WALLETS ──────────────────────────────────── */}
      {activeSubTab === 0 && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {/* Expense Wallet */}
            <div
              style={{
                background: `linear-gradient(135deg, ${expenseWalletColor || '#1A56DB'}, #1240A8)`,
              }}
              className="p-6 rounded-2xl text-white shadow-lg relative flex flex-col justify-between h-48"
            >
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-xs font-semibold uppercase tracking-wider text-blue-100">
                    Primary Operational Wallet
                  </span>
                  <h3 className="text-xl font-bold mt-1">Expense Wallet</h3>
                </div>
                <div className="w-10 h-10 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center font-bold text-base">
                  E
                </div>
              </div>

              <div>
                <div className="text-3xl font-extrabold tracking-tight">
                  {formatCurrency(expenseBalance)}
                </div>
                <p className="text-xs text-blue-100 mt-1">
                  Active balance for daily transactions & bills
                </p>
              </div>

              <div className="flex items-center justify-between pt-2 border-t border-white/10 text-xs text-blue-200">
                <span>Calculated dynamically</span>
                <span>Active</span>
              </div>
            </div>

            {/* Savings Wallet */}
            <div className="p-6 rounded-2xl text-white bg-gradient-to-br from-finance-green to-finance-greenDark shadow-lg relative flex flex-col justify-between h-48">
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-xs font-semibold uppercase tracking-wider text-emerald-100">
                    Dedicated Savings Reserve
                  </span>
                  <h3 className="text-xl font-bold mt-1">Savings Wallet</h3>
                </div>
                <div className="w-10 h-10 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center font-bold text-base">
                  S
                </div>
              </div>

              <div>
                <div className="text-3xl font-extrabold tracking-tight">
                  {formatCurrency(savingsBalance)}
                </div>
                <p className="text-xs text-emerald-100 mt-1">Protected balance for long-term goals</p>
              </div>

              <div className="flex items-center justify-between pt-2 border-t border-white/10 text-xs text-emerald-200">
                <span>Direct transfers available</span>
                <span>Secured</span>
              </div>
            </div>
          </div>

          {/* Quick Inter-Wallet Action */}
          <div className="p-6 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
            <div>
              <h4 className="font-bold text-base text-gray-900 dark:text-white">
                Inter-Wallet Transfer
              </h4>
              <p className="text-xs text-gray-500 dark:text-darkMuted mt-0.5">
                Move money seamlessly between your Expense Wallet and Savings Wallet with zero friction.
              </p>
            </div>
            <button
              onClick={onOpenTransferModal}
              className="px-5 py-2.5 rounded-xl bg-finance-blue hover:bg-finance-blueDark text-white text-xs font-bold shadow flex items-center gap-2 whitespace-nowrap"
            >
              <ArrowRightLeft size={16} />
              <span>Transfer Funds</span>
            </button>
          </div>
        </div>
      )}

      {/* ── TAB 1: CUSTOM WALLETS ────────────────────────────────────── */}
      {activeSubTab === 1 && (
        <div className="space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-bold text-lg text-gray-900 dark:text-white">Custom Event Wallets</h3>
              <p className="text-xs text-gray-500 dark:text-darkMuted">
                Create dedicated sub-wallets for trips, weddings, festivals, or temporary budgets.
              </p>
            </div>
            <button
              onClick={() => {
                setWalletName('');
                setWalletColor('#F5A623');
                setShowAddWallet(true);
              }}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark transition-all"
            >
              <Plus size={16} />
              <span>Create Wallet</span>
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {customWallets.length === 0 ? (
              <div className="col-span-full py-12 text-center text-xs text-gray-500 dark:text-darkMuted border border-dashed border-lightBorder dark:border-darkBorder rounded-2xl">
                No custom wallets created yet. Click "+ Create Wallet" to add one!
              </div>
            ) : (
              customWallets.map((cw) => {
                const bal = calculateCustomWalletBalance(cw.id, transactions);
                return (
                  <div
                    key={cw.id}
                    className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm flex flex-col justify-between space-y-4"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div
                          style={{ backgroundColor: cw.color || '#F5A623' }}
                          className="w-10 h-10 rounded-xl text-white font-bold flex items-center justify-center text-sm shadow-sm"
                        >
                          {cw.name.charAt(0).toUpperCase()}
                        </div>
                        <div>
                          <h4 className="font-bold text-sm text-gray-900 dark:text-white">
                            {cw.name}
                          </h4>
                          <span className="text-[11px] text-gray-500 dark:text-darkMuted">
                            Custom Wallet
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => {
                            setEditingWallet(cw);
                            setWalletName(cw.name);
                            setWalletColor(cw.color);
                          }}
                          className="p-1 rounded text-gray-400 hover:text-finance-blue"
                          title="Edit"
                        >
                          <Edit2 size={14} />
                        </button>
                        <button
                          onClick={() => onDeleteCustomWallet(cw.id)}
                          className="p-1 rounded text-gray-400 hover:text-red-500"
                          title="Delete"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>

                    <div>
                      <span className="text-xs text-gray-500 dark:text-darkMuted font-medium">
                        Current Balance
                      </span>
                      <div className="text-2xl font-extrabold text-gray-900 dark:text-white">
                        {formatCurrency(bal)}
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* ── TAB 2: CREDIT CARDS ──────────────────────────────────────── */}
      {activeSubTab === 2 && (
        <div className="space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-bold text-lg text-gray-900 dark:text-white">Credit Cards Manager</h3>
              <p className="text-xs text-gray-500 dark:text-darkMuted">
                Monitor card limits, current statement charges, and pay down card balances.
              </p>
            </div>
            <button
              onClick={() => {
                setCardName('');
                setCardOwner('');
                setCardLimit('');
                setCardColor('#0097A7');
                setShowAddCard(true);
              }}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark transition-all"
            >
              <Plus size={16} />
              <span>Add Credit Card</span>
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {creditCards.length === 0 ? (
              <div className="col-span-full py-12 text-center text-xs text-gray-500 dark:text-darkMuted border border-dashed border-lightBorder dark:border-darkBorder rounded-2xl">
                No credit cards linked yet. Click "+ Add Credit Card" to track your limits!
              </div>
            ) : (
              creditCards.map((card) => {
                const spent = calculateCreditCardSpend(card.id, transactions);
                const limit = card.credit_limit || 0;
                const remaining = Math.max(0, limit - spent);
                const pct = limit > 0 ? Math.min(100, Math.round((spent / limit) * 100)) : 0;

                return (
                  <div
                    key={card.id}
                    style={{
                      background: `linear-gradient(135deg, ${card.color || '#0097A7'}, #004D40)`,
                    }}
                    className="p-6 rounded-2xl text-white shadow-lg flex flex-col justify-between h-56 relative overflow-hidden"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <CardIcon size={20} />
                        <span className="font-bold text-base tracking-wide">{card.card_name}</span>
                      </div>
                      <div className="flex items-center gap-1 bg-black/20 backdrop-blur-md rounded-lg p-1">
                        <button
                          onClick={() => {
                            setEditingCard(card);
                            setCardName(card.card_name);
                            setCardOwner(card.owner_name);
                            setCardLimit(String(card.credit_limit));
                            setCardColor(card.color);
                          }}
                          className="p-1 text-white/80 hover:text-white"
                          title="Edit"
                        >
                          <Edit2 size={13} />
                        </button>
                        <button
                          onClick={() => onDeleteCreditCard(card.id)}
                          className="p-1 text-white/80 hover:text-red-300"
                          title="Delete"
                        >
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </div>

                    <div>
                      <p className="text-[11px] text-teal-100 uppercase tracking-wider">
                        Current Outstanding Spend
                      </p>
                      <div className="text-2xl font-extrabold">{formatCurrency(spent)}</div>
                      <p className="text-xs text-teal-200 mt-0.5">Cardholder: {card.owner_name}</p>
                    </div>

                    <div className="space-y-2">
                      <div className="w-full bg-black/30 h-1.5 rounded-full overflow-hidden">
                        <div
                          style={{ width: `${pct}%` }}
                          className={`h-full rounded-full ${pct > 80 ? 'bg-red-400' : 'bg-emerald-300'}`}
                        />
                      </div>
                      <div className="flex items-center justify-between text-[11px] text-teal-100">
                        <span>Limit: {formatCurrency(limit)}</span>
                        <span>Available: {formatCurrency(remaining)}</span>
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* ── TAB 3: CONTACTS & DEBT LEDGER ────────────────────────────── */}
      {activeSubTab === 3 && (
        <div className="space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-bold text-lg text-gray-900 dark:text-white">Borrow & Lend Ledger</h3>
              <p className="text-xs text-gray-500 dark:text-darkMuted">
                Track personal debts, split bills with friends and family, and settle balances.
              </p>
            </div>
            <button
              onClick={() => {
                setContactName('');
                setShowAddContact(true);
              }}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark transition-all"
            >
              <Plus size={16} />
              <span>Add Contact</span>
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {ledgerEntities.length === 0 ? (
              <div className="col-span-full py-12 text-center text-xs text-gray-500 dark:text-darkMuted border border-dashed border-lightBorder dark:border-darkBorder rounded-2xl">
                No contacts or family members in ledger. Click "+ Add Contact" to begin tracking debts!
              </div>
            ) : (
              ledgerEntities.map((entity) => {
                const owesYou = entity.balance > 0;
                const youOwe = entity.balance < 0;

                return (
                  <div
                    key={`${entity.isFamilyMember ? 'F' : 'C'}_${entity.id}`}
                    className="p-4 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm flex flex-col justify-between space-y-4"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div
                          className={`w-10 h-10 rounded-full font-bold flex items-center justify-center text-sm ${
                            entity.isFamilyMember
                              ? 'bg-purple-100 text-purple-700 dark:bg-purple-950/40 dark:text-purple-300'
                              : 'bg-finance-blue/10 text-finance-blue dark:bg-finance-blue/20'
                          }`}
                        >
                          {entity.name.charAt(0).toUpperCase()}
                        </div>
                        <div>
                          <h4 className="font-bold text-sm text-gray-900 dark:text-white">
                            {entity.name}
                          </h4>
                          <span className="text-[11px] text-gray-500 dark:text-darkMuted">
                            {entity.isFamilyMember
                              ? `Family (${entity.relationship || 'Member'})`
                              : 'Contact'}
                          </span>
                        </div>
                      </div>

                      {!entity.isFamilyMember && (
                        <div className="flex items-center gap-1">
                          <button
                            onClick={() => {
                              setEditingContact(entity.contactObj || null);
                              setContactName(entity.name);
                            }}
                            className="p-1 rounded text-gray-400 hover:text-finance-blue"
                            title="Edit"
                          >
                            <Edit2 size={13} />
                          </button>
                          <button
                            onClick={() => onDeleteContact(entity.id)}
                            className="p-1 rounded text-gray-400 hover:text-red-500"
                            title="Delete"
                          >
                            <Trash2 size={13} />
                          </button>
                        </div>
                      )}
                    </div>

                    {/* Status Badge */}
                    <div className="py-2 px-3 rounded-xl bg-gray-50 dark:bg-darkCard/50 border border-lightBorder dark:border-darkBorder/60 flex items-center justify-between">
                      <span className="text-xs text-gray-600 dark:text-darkMuted">Status</span>
                      <span
                        className={`text-xs font-bold ${
                          owesYou
                            ? 'text-finance-green'
                            : youOwe
                            ? 'text-finance-red'
                            : 'text-gray-500 dark:text-darkMuted'
                        }`}
                      >
                        {owesYou
                          ? `Owes you ${formatCurrency(entity.balance)}`
                          : youOwe
                          ? `You owe ${formatCurrency(Math.abs(entity.balance))}`
                          : 'Settled up'}
                      </span>
                    </div>

                    {/* Actions */}
                    <div className="flex items-center gap-2 pt-1">
                      <button
                        onClick={() =>
                          onNavigateToFilteredTransactions(entity.isFamilyMember, entity.id)
                        }
                        className="flex-1 py-1.5 px-3 rounded-lg border border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted text-xs font-semibold hover:bg-gray-50 dark:hover:bg-darkCard flex items-center justify-center gap-1.5 transition-colors"
                      >
                        <History size={13} />
                        <span>History</span>
                      </button>

                      {entity.balance !== 0 && (
                        <button
                          onClick={() => {
                            setSettlingEntity(entity);
                            setSettleWallet('EXPENSE_WALLET');
                          }}
                          className="py-1.5 px-3 rounded-lg bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark transition-all"
                        >
                          Settle Up
                        </button>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* ── Settle Up Modal ──────────────────────────────────────────── */}
      {settlingEntity && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <h3 className="font-bold text-base text-gray-900 dark:text-white">
              Settle Debt with {settlingEntity.name}
            </h3>
            <p className="text-xs text-gray-500 dark:text-darkMuted">
              {settlingEntity.balance > 0
                ? `${settlingEntity.name} owes you ${formatCurrency(settlingEntity.balance)}. Settle to mark this debt as collected.`
                : `You owe ${settlingEntity.name} ${formatCurrency(
                    Math.abs(settlingEntity.balance)
                  )}. Settle to record your repayment.`}
            </p>

            <form onSubmit={handleSettleUp} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  {settlingEntity.balance > 0 ? 'Receive Funds Into' : 'Pay From Wallet'}
                </label>
                <select
                  value={settleWallet}
                  onChange={(e) => setSettleWallet(e.target.value as WalletType)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
                >
                  <option value="EXPENSE_WALLET">Expense Wallet</option>
                  <option value="SAVINGS_WALLET">Savings Wallet</option>
                </select>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setSettlingEntity(null)}
                  className="px-3.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 rounded-lg bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark"
                >
                  Confirm Settlement
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Add/Edit Custom Wallet Modal ──────────────────────────────── */}
      {(showAddWallet || editingWallet) && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <h3 className="font-bold text-base text-gray-900 dark:text-white">
              {editingWallet ? 'Edit Custom Wallet' : 'Create Custom Wallet'}
            </h3>
            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Wallet Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Goa Trip 2026, Wedding Fund"
                  value={walletName}
                  onChange={(e) => setWalletName(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Badge Color
                </label>
                <input
                  type="color"
                  value={walletColor}
                  onChange={(e) => setWalletColor(e.target.value)}
                  className="w-full h-10 p-1 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard cursor-pointer"
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => {
                  setShowAddWallet(false);
                  setEditingWallet(null);
                }}
                className="px-3 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  if (!walletName.trim()) return;
                  if (editingWallet) {
                    await onUpdateCustomWallet({
                      ...editingWallet,
                      name: walletName.trim(),
                      color: walletColor,
                    });
                  } else {
                    await onAddCustomWallet({ name: walletName.trim(), color: walletColor });
                  }
                  setShowAddWallet(false);
                  setEditingWallet(null);
                }}
                className="px-4 py-1.5 rounded-lg bg-finance-blue text-white text-xs font-bold shadow"
              >
                Save Wallet
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Add/Edit Credit Card Modal ─────────────────────────────────── */}
      {(showAddCard || editingCard) && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <h3 className="font-bold text-base text-gray-900 dark:text-white">
              {editingCard ? 'Edit Credit Card' : 'Add Credit Card'}
            </h3>
            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Card Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. HDFC Regalia, Axis Magnus"
                  value={cardName}
                  onChange={(e) => setCardName(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Cardholder Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Parth"
                  value={cardOwner}
                  onChange={(e) => setCardOwner(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Credit Limit (₹)
                </label>
                <input
                  type="number"
                  required
                  placeholder="e.g. 150000"
                  value={cardLimit}
                  onChange={(e) => setCardLimit(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Card Theme Color
                </label>
                <input
                  type="color"
                  value={cardColor}
                  onChange={(e) => setCardColor(e.target.value)}
                  className="w-full h-10 p-1 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard cursor-pointer"
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => {
                  setShowAddCard(false);
                  setEditingCard(null);
                }}
                className="px-3 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  if (!cardName.trim() || !cardOwner.trim()) return;
                  const lim = parseFloat(cardLimit) || 0;
                  if (editingCard) {
                    await onUpdateCreditCard({
                      ...editingCard,
                      card_name: cardName.trim(),
                      owner_name: cardOwner.trim(),
                      credit_limit: lim,
                      color: cardColor,
                    });
                  } else {
                    await onAddCreditCard({
                      card_name: cardName.trim(),
                      owner_name: cardOwner.trim(),
                      credit_limit: lim,
                      color: cardColor,
                    });
                  }
                  setShowAddCard(false);
                  setEditingCard(null);
                }}
                className="px-4 py-1.5 rounded-lg bg-finance-blue text-white text-xs font-bold shadow"
              >
                Save Card
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Add/Edit Contact Modal ────────────────────────────────────── */}
      {(showAddContact || editingContact) && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <h3 className="font-bold text-base text-gray-900 dark:text-white">
              {editingContact ? 'Edit Contact' : 'Add New Contact'}
            </h3>
            <div>
              <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                Contact Name
              </label>
              <input
                type="text"
                required
                placeholder="e.g. Alex, Rahul"
                value={contactName}
                onChange={(e) => setContactName(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => {
                  setShowAddContact(false);
                  setEditingContact(null);
                }}
                className="px-3 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={async () => {
                  if (!contactName.trim()) return;
                  if (editingContact) {
                    await onUpdateContact({ ...editingContact, name: contactName.trim() });
                  } else {
                    await onAddContact(contactName.trim());
                  }
                  setShowAddContact(false);
                  setEditingContact(null);
                }}
                className="px-4 py-1.5 rounded-lg bg-finance-blue text-white text-xs font-bold shadow"
              >
                Save Contact
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
