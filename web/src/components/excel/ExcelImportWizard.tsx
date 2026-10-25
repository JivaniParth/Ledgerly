import React, { useState } from 'react';
import {
  UploadCloud,
  CheckCircle2,
  AlertCircle,
  Table as TableIcon,
  ChevronRight,
  ArrowLeft,
  X,
} from 'lucide-react';
import { parseExcelWorkbook, ExtractedTable, ExtractedRow } from '../../lib/excelParser';
import {
  CreditCard,
  CustomWallet,
  Transaction,
  TransactionType,
} from '../../lib/types';

interface ExcelImportWizardProps {
  creditCards: CreditCard[];
  customWallets: CustomWallet[];
  expenseCategories: string[];
  incomeCategories: string[];
  onBatchImport: (txs: Omit<Transaction, 'id'>[]) => Promise<{ success: boolean; count?: number; error?: string }>;
  onFinish: () => void;
}

export const ExcelImportWizard: React.FC<ExcelImportWizardProps> = ({
  creditCards,
  customWallets: _customWallets,
  expenseCategories,
  incomeCategories: _incomeCategories,
  onBatchImport,
  onFinish,
}) => {
  // Wizard steps: 1: Upload, 2: Configure Tables & Preview, 3: Completed
  const [step, setStep] = useState<number>(1);
  const [fileName, setFileName] = useState<string>('');
  const [tables, setTables] = useState<ExtractedTable[]>([]);
  const [selectedTableIndex, setSelectedTableIndex] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [importCount, setImportCount] = useState<number>(0);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setLoading(true);
    setError(null);
    setFileName(file.name);

    try {
      const parsedTables = await parseExcelWorkbook(file);
      if (parsedTables.length === 0) {
        setError('No valid transaction tables found in this spreadsheet.');
        setLoading(false);
        return;
      }
      setTables(parsedTables);
      setSelectedTableIndex(0);
      setStep(2);
    } catch (err: any) {
      setError(err.message || 'Failed to parse Excel file.');
    } finally {
      setLoading(false);
    }
  };

  const handleTableConfigChange = (
    index: number,
    field: keyof ExtractedTable,
    value: any
  ) => {
    setTables((prev) => {
      const updated = [...prev];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  };

  const handleRowChange = (
    tableIndex: number,
    rowIndex: number,
    field: keyof ExtractedRow,
    value: any
  ) => {
    setTables((prev) => {
      const updated = [...prev];
      const table = { ...updated[tableIndex] };
      const rows = [...table.rows];
      rows[rowIndex] = { ...rows[rowIndex], [field]: value };
      table.rows = rows;
      updated[tableIndex] = table;
      return updated;
    });
  };

  const handleDeleteRow = (tableIndex: number, rowIndex: number) => {
    setTables((prev) => {
      const updated = [...prev];
      const table = { ...updated[tableIndex] };
      table.rows = table.rows.filter((_, i) => i !== rowIndex);
      updated[tableIndex] = table;
      return updated;
    });
  };

  const handleCommitImport = async () => {
    setLoading(true);
    setError(null);

    const allTxPayloads: Omit<Transaction, 'id'>[] = [];

    for (const table of tables) {
      if (table.importType === 'IGNORE') continue;

      let txType: TransactionType = 'PERSONAL_EXPENSE';
      if (table.importType === 'INCOME') txType = 'PERSONAL_INCOME';

      for (const row of table.rows) {
        if (row.parsedAmount <= 0) continue;

        allTxPayloads.push({
          amount: row.parsedAmount,
          notes: row.rawDesc,
          date: row.dateTimestamp || Date.now(),
          type: txType,
          source_wallet: table.targetWallet,
          category: row.rawCategory || table.defaultCategory || 'Other',
          credit_card_id: table.targetWallet === 'CREDIT_CARD' ? table.selectedCreditCardId : null,
          custom_wallet_id:
            table.targetWallet === 'TEMPORARY_WALLET' ? table.selectedCustomWalletId : null,
        });
      }
    }

    if (allTxPayloads.length === 0) {
      setError('No valid transactions to import.');
      setLoading(false);
      return;
    }

    const res = await onBatchImport(allTxPayloads);
    setLoading(false);

    if (res.success) {
      setImportCount(res.count || allTxPayloads.length);
      setStep(3);
    } else {
      setError(res.error || 'Batch import failed');
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* ── Wizard Header ────────────────────────────────────────────── */}
      <div>
        <h2 className="text-xl font-bold text-gray-900 dark:text-white">Excel Transaction Importer</h2>
        <p className="text-xs text-gray-500 dark:text-darkMuted mt-0.5">
          Intelligent workbook parser with multi-table detection and statement mapping.
        </p>
      </div>

      {error && (
        <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 text-red-600 dark:text-red-400 text-xs flex items-center gap-2">
          <AlertCircle size={16} />
          <span>{error}</span>
        </div>
      )}

      {/* ── STEP 1: UPLOAD ───────────────────────────────────────────── */}
      {step === 1 && (
        <div className="p-10 rounded-2xl bg-white dark:bg-darkSurface border-2 border-dashed border-lightBorder dark:border-darkBorder text-center space-y-4 hover:border-finance-blue transition-colors">
          <div className="w-16 h-16 rounded-2xl bg-blue-50 dark:bg-blue-950/40 text-finance-blue flex items-center justify-center mx-auto">
            <UploadCloud size={32} />
          </div>

          <div>
            <h3 className="text-base font-bold text-gray-900 dark:text-white">
              Choose or Drop an Excel File
            </h3>
            <p className="text-xs text-gray-500 dark:text-darkMuted mt-1">
              Supports .xlsx and .xls bank statements, credit card bills, and multi-sheet expense logs.
            </p>
          </div>

          <label className="inline-block px-5 py-2.5 rounded-xl bg-finance-blue hover:bg-finance-blueDark text-white text-xs font-bold shadow cursor-pointer transition-all">
            {loading ? 'Analyzing Spreadsheet...' : 'Select Excel Spreadsheet'}
            <input
              type="file"
              accept=".xlsx, .xls"
              onChange={handleFileUpload}
              disabled={loading}
              className="hidden"
            />
          </label>
        </div>
      )}

      {/* ── STEP 2: CONFIGURE TABLES & PREVIEW ───────────────────────── */}
      {step === 2 && (
        <div className="space-y-6">
          {/* Table Selector Tabs */}
          <div className="flex border-b border-lightBorder dark:border-darkBorder bg-white dark:bg-darkSurface rounded-2xl p-1.5 shadow-sm overflow-x-auto">
            {tables.map((tbl, idx) => (
              <button
                key={tbl.id}
                onClick={() => setSelectedTableIndex(idx)}
                className={`flex-1 min-w-[160px] flex items-center justify-center gap-2 py-2 px-3 rounded-xl text-xs font-bold transition-all ${
                  selectedTableIndex === idx
                    ? 'bg-finance-blue text-white shadow-sm'
                    : 'text-gray-600 dark:text-darkMuted hover:text-gray-900 dark:hover:text-darkText'
                }`}
              >
                <TableIcon size={14} />
                <span className="truncate">{tbl.title}</span>
              </button>
            ))}
          </div>

          {/* Current Table Configuration */}
          {tables[selectedTableIndex] && (
            <div className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-5">
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                    Table Import Type
                  </label>
                  <select
                    value={tables[selectedTableIndex].importType}
                    onChange={(e) =>
                      handleTableConfigChange(selectedTableIndex, 'importType', e.target.value)
                    }
                    className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
                  >
                    <option value="EXPENSE">Expense Outflows</option>
                    <option value="INCOME">Income Inflows</option>
                    <option value="IGNORE">Ignore this Table</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                    Target Wallet
                  </label>
                  <select
                    value={tables[selectedTableIndex].targetWallet}
                    onChange={(e) =>
                      handleTableConfigChange(selectedTableIndex, 'targetWallet', e.target.value)
                    }
                    className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
                  >
                    <option value="EXPENSE_WALLET">Expense Wallet</option>
                    <option value="SAVINGS_WALLET">Savings Wallet</option>
                    <option value="CREDIT_CARD">Credit Card</option>
                    <option value="TEMPORARY_WALLET">Custom Wallet</option>
                  </select>
                </div>

                {tables[selectedTableIndex].targetWallet === 'CREDIT_CARD' && (
                  <div>
                    <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                      Select Credit Card
                    </label>
                    <select
                      value={tables[selectedTableIndex].selectedCreditCardId || ''}
                      onChange={(e) =>
                        handleTableConfigChange(
                          selectedTableIndex,
                          'selectedCreditCardId',
                          Number(e.target.value)
                        )
                      }
                      className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
                    >
                      <option value="">Select Card</option>
                      {creditCards.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.card_name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                <div>
                  <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                    Default Category
                  </label>
                  <select
                    value={tables[selectedTableIndex].defaultCategory}
                    onChange={(e) =>
                      handleTableConfigChange(selectedTableIndex, 'defaultCategory', e.target.value)
                    }
                    className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
                  >
                    {expenseCategories.map((cat) => (
                      <option key={cat} value={cat}>
                        {cat}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Editable Preview Table */}
              <div className="border border-lightBorder dark:border-darkBorder rounded-xl overflow-hidden">
                <div className="max-h-80 overflow-y-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-gray-50 dark:bg-darkCard text-gray-500 uppercase text-[10px] sticky top-0 font-bold border-b border-lightBorder dark:border-darkBorder">
                      <tr>
                        <th className="py-2.5 px-3">Date</th>
                        <th className="py-2.5 px-3">Description</th>
                        <th className="py-2.5 px-3">Category</th>
                        <th className="py-2.5 px-3 text-right">Amount (₹)</th>
                        <th className="py-2.5 px-3 text-center">Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-lightBorder dark:divide-darkBorder/60">
                      {tables[selectedTableIndex].rows.map((row, rIdx) => (
                        <tr key={rIdx} className="hover:bg-gray-50 dark:hover:bg-darkCard/40">
                          <td className="py-2 px-3 whitespace-nowrap">
                            <input
                              type="text"
                              value={row.rawDate}
                              onChange={(e) =>
                                handleRowChange(selectedTableIndex, rIdx, 'rawDate', e.target.value)
                              }
                              className="px-2 py-1 rounded border border-lightBorder dark:border-darkBorder bg-transparent text-xs w-28"
                            />
                          </td>

                          <td className="py-2 px-3">
                            <input
                              type="text"
                              value={row.rawDesc}
                              onChange={(e) =>
                                handleRowChange(selectedTableIndex, rIdx, 'rawDesc', e.target.value)
                              }
                              className="px-2 py-1 rounded border border-lightBorder dark:border-darkBorder bg-transparent text-xs w-full"
                            />
                          </td>

                          <td className="py-2 px-3 whitespace-nowrap">
                            <input
                              type="text"
                              value={row.rawCategory}
                              onChange={(e) =>
                                handleRowChange(selectedTableIndex, rIdx, 'rawCategory', e.target.value)
                              }
                              className="px-2 py-1 rounded border border-lightBorder dark:border-darkBorder bg-transparent text-xs w-24"
                            />
                          </td>

                          <td className="py-2 px-3 text-right whitespace-nowrap">
                            <input
                              type="number"
                              step="0.01"
                              value={row.parsedAmount}
                              onChange={(e) =>
                                handleRowChange(
                                  selectedTableIndex,
                                  rIdx,
                                  'parsedAmount',
                                  parseFloat(e.target.value) || 0
                                )
                              }
                              className="px-2 py-1 rounded border border-lightBorder dark:border-darkBorder bg-transparent text-xs w-24 text-right font-semibold"
                            />
                          </td>

                          <td className="py-2 px-3 text-center whitespace-nowrap">
                            <button
                              onClick={() => handleDeleteRow(selectedTableIndex, rIdx)}
                              className="text-gray-400 hover:text-red-500 p-1"
                            >
                              <X size={14} />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* Action Footer */}
          <div className="flex items-center justify-between pt-2">
            <button
              onClick={() => setStep(1)}
              className="px-4 py-2 rounded-xl border border-lightBorder dark:border-darkBorder text-xs font-semibold hover:bg-gray-50 dark:hover:bg-darkCard flex items-center gap-1.5"
            >
              <ArrowLeft size={14} />
              <span>Back to Upload</span>
            </button>

            <button
              onClick={handleCommitImport}
              disabled={loading}
              className="px-6 py-2.5 rounded-xl bg-finance-blue hover:bg-finance-blueDark text-white text-xs font-bold shadow flex items-center gap-2 disabled:opacity-50"
            >
              <span>{loading ? 'Importing Transactions...' : 'Confirm & Import to Database'}</span>
              <ChevronRight size={15} />
            </button>
          </div>
        </div>
      )}

      {/* ── STEP 3: COMPLETED ────────────────────────────────────────── */}
      {step === 3 && (
        <div className="p-10 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder text-center space-y-4 shadow-sm">
          <div className="w-16 h-16 rounded-2xl bg-emerald-50 dark:bg-emerald-950/40 text-finance-green flex items-center justify-center mx-auto">
            <CheckCircle2 size={36} />
          </div>

          <div>
            <h3 className="text-lg font-bold text-gray-900 dark:text-white">Import Successful!</h3>
            <p className="text-xs text-gray-500 dark:text-darkMuted mt-1">
              Successfully imported {importCount} transactions from "{fileName}" directly into your
              cloud database.
            </p>
          </div>

          <button
            onClick={onFinish}
            className="px-6 py-2.5 rounded-xl bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark"
          >
            View in Transactions
          </button>
        </div>
      )}
    </div>
  );
};
