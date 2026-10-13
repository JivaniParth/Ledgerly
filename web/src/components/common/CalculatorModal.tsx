import React, { useState, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import {
  X,
  Delete,
  Check,
  RotateCcw,
  Equal,
} from 'lucide-react';
import {
  evaluateMathExpression,
  convertAmountToWords,
  formatIndianCurrency,
} from '../../lib/amountUtils';
import { formatCurrency } from '../../lib/calculations';

interface CalculatorModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialValue?: string;
  onDone?: (calculatedValue: string) => void;
  title?: string;
}

export const CalculatorModal: React.FC<CalculatorModalProps> = ({
  isOpen,
  onClose,
  initialValue = '',
  onDone,
  title = 'Ledgerly Financial Calculator',
}) => {
  const [expression, setExpression] = useState<string>(initialValue || '');

  // Reset or update expression when opening
  useEffect(() => {
    if (isOpen) {
      setExpression(initialValue || '');
    }
  }, [isOpen, initialValue]);

  const evaluated = evaluateMathExpression(expression);
  const numericResult = parseFloat(evaluated);

  const handleKeyPress = useCallback((key: string) => {
    if (key === 'C') {
      setExpression('');
      return;
    }
    if (key === 'BACKSPACE') {
      setExpression((prev) => prev.slice(0, -1));
      return;
    }
    if (key === '=') {
      setExpression(evaluateMathExpression(expression));
      return;
    }
    if (key === 'DONE') {
      const finalResult = evaluateMathExpression(expression);
      if (onDone) {
        onDone(finalResult || '0');
      }
      onClose();
      return;
    }

    setExpression((prev) => {
      // Prevent consecutive operators
      if ('+-*/'.includes(key) && prev.length > 0 && '+-*/'.includes(prev[prev.length - 1])) {
        return prev.slice(0, -1) + key;
      }
      return prev + key;
    });
  }, [expression, onDone, onClose]);

  // Physical keyboard support
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      e.stopPropagation();
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      } else if (e.key === 'Enter') {
        e.preventDefault();
        handleKeyPress('DONE');
      } else if (e.key === 'Backspace') {
        e.preventDefault();
        handleKeyPress('BACKSPACE');
      } else if (e.key === '=') {
        e.preventDefault();
        handleKeyPress('=');
      } else if ('0123456789+-*/.'.includes(e.key)) {
        e.preventDefault();
        handleKeyPress(e.key);
      } else if (e.key.toLowerCase() === 'c') {
        e.preventDefault();
        handleKeyPress('C');
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose, handleKeyPress]);

  if (!isOpen) return null;

  const buttons = [
    [
      { label: 'C', key: 'C', type: 'danger' },
      { label: '÷', key: '/', type: 'operator' },
      { label: '×', key: '*', type: 'operator' },
      { label: '⌫', key: 'BACKSPACE', type: 'action' },
    ],
    [
      { label: '7', key: '7', type: 'num' },
      { label: '8', key: '8', type: 'num' },
      { label: '9', key: '9', type: 'num' },
      { label: '−', key: '-', type: 'operator' },
    ],
    [
      { label: '4', key: '4', type: 'num' },
      { label: '5', key: '5', type: 'num' },
      { label: '6', key: '6', type: 'num' },
      { label: '+', key: '+', type: 'operator' },
    ],
    [
      { label: '1', key: '1', type: 'num' },
      { label: '2', key: '2', type: 'num' },
      { label: '3', key: '3', type: 'num' },
      { label: '=', key: '=', type: 'equals' },
    ],
    [
      { label: '0', key: '0', type: 'num' },
      { label: '00', key: '00', type: 'num' },
      { label: '.', key: '.', type: 'num' },
      { label: 'DONE', key: 'DONE', type: 'done' },
    ],
  ];

  return createPortal(
    <div
      className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200"
      onClick={(e) => {
        e.stopPropagation();
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-3xl w-full max-w-sm overflow-hidden shadow-2xl flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="px-5 py-4 border-b border-lightBorder dark:border-darkBorder flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-finance-blue text-white flex items-center justify-center font-bold text-xs shadow-sm">
              ₹
            </div>
            <h3 className="font-bold text-sm text-gray-900 dark:text-white">{title}</h3>
          </div>
          <button
            type="button"
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              onClose();
            }}
            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-darkCard transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        {/* Display Screen */}
        <div className="p-5 bg-gray-50/80 dark:bg-darkCard/50 border-b border-lightBorder dark:border-darkBorder/60 text-right space-y-1">
          {/* Active Expression */}
          <div className="text-xs font-mono font-medium text-gray-500 dark:text-darkMuted min-h-[1.25rem] truncate tracking-wider">
            {expression ? expression.replace(/\*/g, ' × ').replace(/\//g, ' ÷ ').replace(/\+/g, ' + ').replace(/-/g, ' − ') : '0'}
          </div>

          {/* Big Evaluated Result */}
          <div className="text-3xl font-extrabold tracking-tight text-gray-900 dark:text-white truncate">
            {numericResult && !isNaN(numericResult)
              ? formatCurrency(numericResult)
              : '₹0.00'}
          </div>

          {/* Words in Indian Rupees */}
          <div className="text-[11px] font-medium text-finance-blue dark:text-blue-400 min-h-[1rem] truncate">
            {!isNaN(numericResult) && numericResult > 0
              ? convertAmountToWords(numericResult)
              : 'Zero Rupees'}
          </div>
        </div>

        {/* Keypad */}
        <div className="p-4 grid grid-cols-4 gap-2">
          {buttons.flat().map((btn) => {
            let bgClass = 'bg-gray-100 dark:bg-darkCard text-gray-800 dark:text-darkText hover:bg-gray-200 dark:hover:bg-darkBorder';

            if (btn.type === 'operator') {
              bgClass = 'bg-blue-50 dark:bg-blue-950/40 text-finance-blue dark:text-blue-400 font-bold hover:bg-blue-100 dark:hover:bg-blue-900/40';
            } else if (btn.type === 'danger') {
              bgClass = 'bg-red-50 dark:bg-red-950/40 text-red-600 dark:text-red-400 font-bold hover:bg-red-100 dark:hover:bg-red-900/40';
            } else if (btn.type === 'equals') {
              bgClass = 'bg-amber-50 dark:bg-amber-950/40 text-finance-gold font-bold hover:bg-amber-100 dark:hover:bg-amber-900/40';
            } else if (btn.type === 'done') {
              bgClass = 'bg-finance-blue text-white font-bold hover:bg-finance-blueDark shadow-md active:scale-95';
            }

            return (
              <button
                type="button"
                key={btn.key}
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  handleKeyPress(btn.key);
                }}
                className={`h-12 rounded-2xl flex items-center justify-center text-base font-semibold transition-all active:scale-95 select-none ${bgClass}`}
              >
                {btn.key === 'BACKSPACE' ? (
                  <Delete size={20} />
                ) : btn.key === 'DONE' ? (
                  <div className="flex items-center gap-1">
                    <Check size={18} />
                    <span className="text-xs font-bold">Done</span>
                  </div>
                ) : (
                  btn.label
                )}
              </button>
            );
          })}
        </div>

        {/* Footer Note */}
        <div className="px-5 py-2.5 bg-gray-50/50 dark:bg-darkCard/30 border-t border-lightBorder dark:border-darkBorder/40 text-center text-[10px] text-gray-400 dark:text-darkMuted">
          Type numbers & operators on physical keyboard or click buttons. Press Enter to confirm.
        </div>
      </div>
    </div>,
    document.body
  );
};
