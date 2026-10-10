import React from 'react';
import {
  LayoutDashboard,
  Wallet,
  ReceiptText,
  Users,
  BarChart3,
  Settings,
  UserCheck,
  FileSpreadsheet,
  ChevronLeft,
  ChevronRight,
  LogOut,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export type ActiveTab =
  | 'dashboard'
  | 'wallets'
  | 'transactions'
  | 'family'
  | 'analytics'
  | 'settings'
  | 'profile'
  | 'excel';

interface SidebarProps {
  activeTab: ActiveTab;
  setActiveTab: (tab: ActiveTab) => void;
  collapsed: boolean;
  setCollapsed: (collapsed: boolean) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  setActiveTab,
  collapsed,
  setCollapsed,
}) => {
  const { logout, userName } = useAuth();

  const menuItems: { id: ActiveTab; label: string; icon: React.ReactNode }[] = [
    { id: 'dashboard', label: 'Dashboard', icon: <LayoutDashboard size={20} /> },
    { id: 'wallets', label: 'Wallets & Cards', icon: <Wallet size={20} /> },
    { id: 'transactions', label: 'Transactions', icon: <ReceiptText size={20} /> },
    { id: 'family', label: 'Family Hub', icon: <Users size={20} /> },
    { id: 'analytics', label: 'Estimation & Stats', icon: <BarChart3 size={20} /> },
    { id: 'excel', label: 'Import Excel', icon: <FileSpreadsheet size={20} /> },
    { id: 'settings', label: 'Settings', icon: <Settings size={20} /> },
    { id: 'profile', label: 'Profile', icon: <UserCheck size={20} /> },
  ];

  return (
    <aside
      className={`h-screen sticky top-0 bg-white dark:bg-darkSurface border-r border-lightBorder dark:border-darkBorder flex flex-col transition-all duration-300 z-30 ${
        collapsed ? 'w-20' : 'w-64'
      }`}
    >
      {/* Brand Header */}
      <div
        className={`h-16 flex items-center border-b border-lightBorder dark:border-darkBorder ${
          collapsed ? 'justify-center px-2' : 'justify-between px-4'
        }`}
      >
        {collapsed ? (
          <button
            onClick={() => setCollapsed(false)}
            className="w-10 h-10 rounded-xl bg-finance-blue text-white flex items-center justify-center font-bold text-lg shadow-md hover:scale-105 active:scale-95 transition-all group relative cursor-pointer"
            title="Expand sidebar"
          >
            <span className="group-hover:hidden">L</span>
            <ChevronRight size={20} className="hidden group-hover:block" />
          </button>
        ) : (
          <>
            <div className="flex items-center gap-3 overflow-hidden">
              <div className="w-10 h-10 rounded-xl bg-finance-blue text-white flex items-center justify-center font-bold text-xl shadow-md shrink-0">
                L
              </div>
              <div className="flex flex-col truncate">
                <span className="font-bold text-lg tracking-tight text-gray-900 dark:text-white leading-tight">
                  Ledgerly
                </span>
                <span className="text-xs text-finance-blue font-medium">Personal Finance</span>
              </div>
            </div>
            <button
              onClick={() => setCollapsed(true)}
              className="p-1.5 rounded-lg text-gray-500 hover:text-gray-900 dark:text-gray-400 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-darkCard transition-colors shrink-0"
              title="Collapse sidebar"
            >
              <ChevronLeft size={18} />
            </button>
          </>
        )}
      </div>

      {/* Nav Items */}
      <nav className={`flex-1 py-4 space-y-1.5 overflow-y-auto ${collapsed ? 'px-2' : 'px-3'}`}>
        {menuItems.map((item) => {
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`w-full flex items-center rounded-xl font-medium text-sm transition-all ${
                collapsed
                  ? 'justify-center h-10 w-10 mx-auto px-0'
                  : 'gap-3 px-3 py-2.5'
              } ${
                isActive
                  ? 'bg-finance-blue text-white shadow-sm font-semibold'
                  : 'text-gray-600 dark:text-darkMuted hover:text-gray-900 dark:hover:text-darkText hover:bg-gray-100 dark:hover:bg-darkCard'
              }`}
              title={collapsed ? item.label : undefined}
            >
              <span className={`shrink-0 ${isActive ? 'text-white' : 'text-gray-500 dark:text-darkMuted'}`}>
                {item.icon}
              </span>
              {!collapsed && <span className="truncate">{item.label}</span>}
            </button>
          );
        })}
      </nav>

      {/* Footer Profile & Logout */}
      <div className={`border-t border-lightBorder dark:border-darkBorder ${collapsed ? 'p-2' : 'p-3'}`}>
        {!collapsed && (
          <div className="px-3 py-2 mb-2 rounded-xl bg-gray-50 dark:bg-darkCard/60 border border-lightBorder dark:border-darkBorder/60 flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-finance-blue/10 dark:bg-finance-blue/20 text-finance-blue font-bold flex items-center justify-center text-sm">
              {userName.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold text-gray-900 dark:text-white truncate">
                {userName}
              </p>
              <p className="text-[10px] text-gray-500 dark:text-darkMuted truncate">Connected</p>
            </div>
          </div>
        )}
        <button
          onClick={logout}
          className={`w-full flex items-center rounded-xl font-medium text-xs text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors ${
            collapsed ? 'justify-center h-10 w-10 mx-auto px-0' : 'gap-3 px-3 py-2'
          }`}
          title={collapsed ? 'Log out' : undefined}
        >
          <LogOut size={18} className="shrink-0" />
          {!collapsed && <span>Log Out</span>}
        </button>
      </div>
    </aside>
  );
};
