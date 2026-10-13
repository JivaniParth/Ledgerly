import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { useLedgerlyData } from './hooks/useLedgerlyData';
import { Sidebar, ActiveTab } from './components/layout/Sidebar';
import { Header } from './components/layout/Header';
import { DashboardView } from './components/dashboard/DashboardView';
import { TransactionsView } from './components/transactions/TransactionsView';
import { WalletsView } from './components/wallets/WalletsView';
import { FamilyHubView } from './components/family/FamilyHubView';
import { AnalyticsView } from './components/analytics/AnalyticsView';
import { SettingsView } from './components/settings/SettingsView';
import { ProfileView } from './components/profile/ProfileView';
import { ExcelImportWizard } from './components/excel/ExcelImportWizard';
import { TransactionModal } from './components/entry/TransactionModal';
import { CalculatorModal } from './components/common/CalculatorModal';
import { AuthPage } from './pages/AuthPage';
import { Transaction } from './lib/types';

const MainDashboardApp: React.FC = () => {
  const { user, loading: authLoading, logout } = useAuth();
  const data = useLedgerlyData();

  const [activeTab, setActiveTab] = useState<ActiveTab>('dashboard');
  const [collapsedSidebar, setCollapsedSidebar] = useState<boolean>(false);

  // Global Calculator Modal State
  const [showGlobalCalculator, setShowGlobalCalculator] = useState<boolean>(false);

  // Transaction Modal State
  const [showTransactionModal, setShowTransactionModal] = useState<boolean>(false);
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null);

  // Filter transfer states from other tabs to Transactions
  const [familyFilterForTransactions, setFamilyFilterForTransactions] = useState<number | null>(null);
  const [contactFilterForTransactions, setContactFilterForTransactions] = useState<number | null>(null);

  if (authLoading) {
    return (
      <div className="min-h-screen bg-lightBg dark:bg-darkBg flex items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-finance-blue text-white flex items-center justify-center font-bold text-2xl shadow-lg animate-pulse">
            L
          </div>
          <p className="text-xs text-gray-500 dark:text-darkMuted font-medium">
            Loading Ledgerly...
          </p>
        </div>
      </div>
    );
  }

  if (!user) {
    return <AuthPage />;
  }

  const getPageTitle = (tab: ActiveTab): string => {
    switch (tab) {
      case 'dashboard':
        return 'Financial Dashboard';
      case 'wallets':
        return 'Wallets & Credit Cards';
      case 'transactions':
        return 'Transactions History';
      case 'family':
        return 'Family Hub';
      case 'analytics':
        return 'Estimation Insight & Analytics';
      case 'settings':
        return 'Settings & Customization';
      case 'profile':
        return 'My Profile';
      case 'excel':
        return 'Excel Import Wizard';
      default:
        return 'Dashboard';
    }
  };

  const handleNavigateToFilteredTransactions = (isFamily: boolean, id: number) => {
    if (isFamily) {
      setFamilyFilterForTransactions(id);
      setContactFilterForTransactions(null);
    } else {
      setContactFilterForTransactions(id);
      setFamilyFilterForTransactions(null);
    }
    setActiveTab('transactions');
  };

  return (
    <div className="min-h-screen flex bg-lightBg dark:bg-darkBg text-lightText dark:text-darkText">
      {/* Sidebar */}
      <Sidebar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        collapsed={collapsedSidebar}
        setCollapsed={setCollapsedSidebar}
      />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 min-h-screen">
        <Header
          streak={data.streak}
          syncing={data.syncing}
          onRefresh={data.refreshData}
          onOpenTransactionModal={() => {
            setEditingTransaction(null);
            setShowTransactionModal(true);
          }}
          onOpenCalculator={() => setShowGlobalCalculator(true)}
          title={getPageTitle(activeTab)}
        />

        <main className="flex-1 p-6 max-w-7xl w-full mx-auto overflow-y-auto">
          {activeTab === 'dashboard' && (
            <DashboardView
              expenseBalance={data.expenseBalance}
              savingsBalance={data.savingsBalance}
              transactions={data.transactions}
              savingsGoals={data.savingsGoals}
              budgetGoals={data.budgetGoals}
              creditCards={data.creditCards}
              monthlyProjection={data.monthlyProjection}
              streak={data.streak}
              onOpenTransactionModal={() => {
                setEditingTransaction(null);
                setShowTransactionModal(true);
              }}
              onNavigateToTransactions={() => {
                setFamilyFilterForTransactions(null);
                setContactFilterForTransactions(null);
                setActiveTab('transactions');
              }}
              onNavigateToAnalytics={() => setActiveTab('analytics')}
              onUpdateSavingsGoal={data.updateSavingsGoal}
            />
          )}

          {activeTab === 'wallets' && (
            <WalletsView
              expenseBalance={data.expenseBalance}
              savingsBalance={data.savingsBalance}
              transactions={data.transactions}
              creditCards={data.creditCards}
              customWallets={data.customWallets}
              contacts={data.contacts}
              ledgerEntities={data.ledgerEntities}
              onAddCreditCard={data.addCreditCard}
              onUpdateCreditCard={data.updateCreditCard}
              onDeleteCreditCard={data.deleteCreditCard}
              onAddCustomWallet={data.addCustomWallet}
              onUpdateCustomWallet={data.updateCustomWallet}
              onDeleteCustomWallet={data.deleteCustomWallet}
              onAddContact={data.addContact}
              onUpdateContact={data.updateContact}
              onDeleteContact={data.deleteContact}
              onSettleUpEntity={data.settleUpEntity}
              onNavigateToFilteredTransactions={handleNavigateToFilteredTransactions}
              onOpenTransferModal={() => {
                setEditingTransaction(null);
                setShowTransactionModal(true);
              }}
            />
          )}

          {activeTab === 'transactions' && (
            <TransactionsView
              transactions={data.transactions}
              familyMembers={data.familyMembers}
              creditCards={data.creditCards}
              customWallets={data.customWallets}
              contacts={data.contacts}
              splits={data.splits}
              expenseCategories={data.expenseCategories}
              incomeCategories={data.incomeCategories}
              onEditTransaction={(tx) => {
                setEditingTransaction(tx);
                setShowTransactionModal(true);
              }}
              onDeleteTransaction={data.deleteTransaction}
              onSplitTransaction={data.splitTransactionRetroactively}
              initialFamilyMemberFilter={familyFilterForTransactions}
              initialContactFilter={contactFilterForTransactions}
            />
          )}

          {activeTab === 'family' && (
            <FamilyHubView
              familyMembers={data.familyMembers}
              transactions={data.transactions}
              onAddFamilyMember={data.addFamilyMember}
              onUpdateFamilyMember={data.updateFamilyMember}
              onDeleteFamilyMember={data.deleteFamilyMember}
              onNavigateToFilteredTransactions={handleNavigateToFilteredTransactions}
              onOpenTransferModalWithMember={(_memberId) => {
                setEditingTransaction(null);
                setShowTransactionModal(true);
              }}
            />
          )}

          {activeTab === 'analytics' && (
            <AnalyticsView
              transactions={data.transactions}
              monthlyProjection={data.monthlyProjection}
            />
          )}

          {activeTab === 'settings' && (
            <SettingsView
              expenseCategories={data.expenseCategories}
              incomeCategories={data.incomeCategories}
              budgetGoals={data.budgetGoals}
              savingsGoals={data.savingsGoals}
              recurringTransactions={data.recurringTransactions}
              onAddCategory={data.addCategory}
              onDeleteCategory={data.deleteCategory}
              onAddBudgetGoal={data.addBudgetGoal}
              onDeleteBudgetGoal={data.deleteBudgetGoal}
              onAddSavingsGoal={data.addSavingsGoal}
              onUpdateSavingsGoal={data.updateSavingsGoal}
              onDeleteSavingsGoal={data.deleteSavingsGoal}
              onAddRecurring={data.addRecurring}
              onUpdateRecurring={data.updateRecurring}
              onDeleteRecurring={data.deleteRecurring}
              onRefreshData={data.refreshData}
              syncing={data.syncing}
              onNavigateToExcel={() => setActiveTab('excel')}
              onClearAllData={data.clearAllData}
              onLogout={logout}
            />
          )}

          {activeTab === 'profile' && <ProfileView />}

          {activeTab === 'excel' && (
            <ExcelImportWizard
              creditCards={data.creditCards}
              customWallets={data.customWallets}
              expenseCategories={data.expenseCategories}
              incomeCategories={data.incomeCategories}
              onBatchImport={data.batchInsertTransactions}
              onFinish={() => setActiveTab('transactions')}
            />
          )}
        </main>
      </div>

      {/* Global Transaction Modal (Add / Edit) */}
      <TransactionModal
        isOpen={showTransactionModal}
        onClose={() => {
          setShowTransactionModal(false);
          setEditingTransaction(null);
        }}
        onSubmit={data.addTransaction}
        onUpdate={data.updateTransaction}
        editingTransaction={editingTransaction}
        familyMembers={data.familyMembers}
        creditCards={data.creditCards}
        customWallets={data.customWallets}
        contacts={data.contacts}
        expenseCategories={data.expenseCategories}
        incomeCategories={data.incomeCategories}
      />

      {/* Global Calculator Modal */}
      <CalculatorModal
        isOpen={showGlobalCalculator}
        onClose={() => setShowGlobalCalculator(false)}
        title="Ledgerly Financial Calculator"
      />
    </div>
  );
};

export function App() {
  return (
    <AuthProvider>
      <ThemeProvider>
        <MainDashboardApp />
      </ThemeProvider>
    </AuthProvider>
  );
}

export default App;
