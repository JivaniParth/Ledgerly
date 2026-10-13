import React, { useState } from 'react';
import { Calculator } from 'lucide-react';
import { convertAmountToWords, evaluateMathExpression, formatIndianCurrency } from '../../lib/amountUtils';
import { CalculatorModal } from './CalculatorModal';

interface AmountInputFieldProps {
  value: string;
  onChange: (value: string) => void;
  label?: string;
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
  className?: string;
  autoFocus?: boolean;
}

export const AmountInputField: React.FC<AmountInputFieldProps> = ({
  value,
  onChange,
  label = 'Amount',
  placeholder = '0.00',
  disabled = false,
  required = false,
  className = '',
  autoFocus = false,
}) => {
  const [showCalculator, setShowCalculator] = useState(false);

  const numericValue = parseFloat((value || '').replace(/,/g, ''));
  const hasValidNumber = !isNaN(numericValue) && numericValue > 0;

  const handleBlur = () => {
    // If the user typed a math expression like 1200+350, evaluate it
    if (value && '+-*/'.split('').some((op) => value.includes(op))) {
      const evaluated = evaluateMathExpression(value);
      if (evaluated && !isNaN(parseFloat(evaluated))) {
        onChange(evaluated);
      }
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      if (value && '+-*/'.split('').some((op) => value.includes(op))) {
        e.preventDefault();
        const evaluated = evaluateMathExpression(value);
        if (evaluated && !isNaN(parseFloat(evaluated))) {
          onChange(evaluated);
        }
      }
    }
  };

  return (
    <div className={`space-y-1 ${className}`}>
      {label && (
        <label className="text-xs font-semibold text-gray-700 dark:text-gray-300 block">
          {label} {required && <span className="text-red-500">*</span>}
        </label>
      )}

      <div className="relative flex items-center">
        {/* Currency symbol prefix */}
        <span className="absolute left-3.5 text-gray-500 dark:text-darkMuted font-bold text-sm select-none pointer-events-none">
          ₹
        </span>

        {/* Input element */}
        <input
          type="text"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onBlur={handleBlur}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled}
          autoFocus={autoFocus}
          className="w-full pl-8 pr-11 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-sm font-semibold text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-finance-blue/40 focus:border-finance-blue transition-all"
        />

        {/* Calculator Button */}
        <button
          type="button"
          onClick={() => setShowCalculator(true)}
          disabled={disabled}
          title="Open Calculator Keyboard"
          className="absolute right-2 p-1.5 rounded-lg text-gray-400 hover:text-finance-blue dark:hover:text-blue-400 hover:bg-gray-100 dark:hover:bg-darkBorder/60 transition-colors"
        >
          <Calculator size={18} />
        </button>
      </div>

      {/* Words in Indian Rupees */}
      {hasValidNumber && (
        <p className="text-[11px] font-medium text-finance-blue dark:text-blue-400 px-1 truncate animate-in fade-in duration-150">
          {convertAmountToWords(numericValue)}
        </p>
      )}

      {/* Calculator Modal */}
      {showCalculator && (
        <CalculatorModal
          isOpen={showCalculator}
          onClose={() => setShowCalculator(false)}
          initialValue={value}
          onDone={(calculated) => {
            onChange(calculated);
          }}
          title={`Calculate ${label}`}
        />
      )}
    </div>
  );
};
