import React, { createContext, useContext, useEffect, useState } from 'react';

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
  };

  const setExpenseWalletColor = (color: string) => {
    setExpenseWalletColorState(color);
    localStorage.setItem('ledgerly_expense_color', color);
  };

  const setCreditCardColor = (color: string) => {
    setCreditCardColorState(color);
    localStorage.setItem('ledgerly_card_color', color);
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
