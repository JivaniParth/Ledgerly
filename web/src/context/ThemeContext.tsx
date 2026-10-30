import React, { createContext, useContext, useEffect, useState } from 'react';
import { supabase } from '../lib/supabase';

type ThemeMode = 'dark' | 'light' | 'system';

interface ThemeContextType {
  theme: ThemeMode;
  setTheme: (theme: ThemeMode) => void;
  isDark: boolean;
  expenseWalletColor: string;
  setExpenseWalletColor: (color: string) => void;
  creditCardColor: string;
  setCreditCardColor: (color: string) => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [theme, setThemeState] = useState<ThemeMode>(() => {
    return (localStorage.getItem('ledgerly_theme') as ThemeMode) || 'dark';
  });

  const [expenseWalletColor, setExpenseWalletColorState] = useState<string>(() => {
    return localStorage.getItem('ledgerly_expense_color') || '#1A56DB';
  });

  const [creditCardColor, setCreditCardColorState] = useState<string>(() => {
    return localStorage.getItem('ledgerly_card_color') || '#0097A7';
  });

  const [isDark, setIsDark] = useState<boolean>(true);

  // Sync settings from Supabase Auth metadata or app_settings table
  useEffect(() => {
    const syncFromRemote = async () => {
      try {
        const { data: { user } } = await supabase.auth.getUser();
        if (user?.user_metadata) {
          const meta = user.user_metadata;
          if (meta.app_theme && meta.app_theme !== theme) {
            setThemeState(meta.app_theme as ThemeMode);
            localStorage.setItem('ledgerly_theme', meta.app_theme);
          }
          if (meta.expense_wallet_color && meta.expense_wallet_color !== expenseWalletColor) {
            setExpenseWalletColorState(meta.expense_wallet_color);
            localStorage.setItem('ledgerly_expense_color', meta.expense_wallet_color);
          }
          if (meta.credit_card_wallet_color && meta.credit_card_wallet_color !== creditCardColor) {
            setCreditCardColorState(meta.credit_card_wallet_color);
            localStorage.setItem('ledgerly_card_color', meta.credit_card_wallet_color);
          }
        }
      } catch {
        // Fallback or offline
      }
    };

    syncFromRemote();

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      if (session?.user?.user_metadata) {
        const meta = session.user.user_metadata;
        if (meta.app_theme) {
          setThemeState(meta.app_theme as ThemeMode);
          localStorage.setItem('ledgerly_theme', meta.app_theme);
        }
        if (meta.expense_wallet_color) {
          setExpenseWalletColorState(meta.expense_wallet_color);
          localStorage.setItem('ledgerly_expense_color', meta.expense_wallet_color);
        }
        if (meta.credit_card_wallet_color) {
          setCreditCardColorState(meta.credit_card_wallet_color);
          localStorage.setItem('ledgerly_card_color', meta.credit_card_wallet_color);
        }
      }
    });

    return () => {
      subscription.unsubscribe();
    };
  }, []);

  useEffect(() => {
    const root = document.documentElement;
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

    const applyTheme = () => {
      const dark = theme === 'dark' || (theme === 'system' && mediaQuery.matches);
      setIsDark(dark);
      if (dark) {
        root.classList.add('dark');
      } else {
        root.classList.remove('dark');
      }
    };

    applyTheme();
    mediaQuery.addEventListener('change', applyTheme);
    return () => mediaQuery.removeEventListener('change', applyTheme);
  }, [theme]);

  const setTheme = (newTheme: ThemeMode) => {
    setThemeState(newTheme);
    localStorage.setItem('ledgerly_theme', newTheme);
    supabase.auth.updateUser({
      data: { app_theme: newTheme },
    }).catch(() => {});
    Promise.resolve(supabase.from('app_settings').upsert([{ key: 'app_theme', value: newTheme }])).catch(() => {});
  };

  const setExpenseWalletColor = (color: string) => {
    setExpenseWalletColorState(color);
    localStorage.setItem('ledgerly_expense_color', color);
    supabase.auth.updateUser({
      data: { expense_wallet_color: color },
    }).catch(() => {});
    Promise.resolve(supabase.from('app_settings').upsert([{ key: 'expense_wallet_color', value: color }])).catch(() => {});
  };

  const setCreditCardColor = (color: string) => {
    setCreditCardColorState(color);
    localStorage.setItem('ledgerly_card_color', color);
    supabase.auth.updateUser({
      data: { credit_card_wallet_color: color },
    }).catch(() => {});
    Promise.resolve(supabase.from('app_settings').upsert([{ key: 'credit_card_wallet_color', value: color }])).catch(() => {});
  };

  return (
    <ThemeContext.Provider
      value={{
        theme,
        setTheme,
        isDark,
        expenseWalletColor,
        setExpenseWalletColor,
        creditCardColor,
        setCreditCardColor,
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('useTheme must be used within a ThemeProvider');
  return context;
};
