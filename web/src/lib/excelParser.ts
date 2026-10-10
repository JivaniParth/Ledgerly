import * as XLSX from 'xlsx';

export interface ExtractedRow {
  originalRowIndex: number;
  rawDate: string;
  rawDesc: string;
  rawAmount: string;
  rawCategory: string;
  dateTimestamp: number;
  parsedAmount: number;
}

export interface ExtractedTable {
  id: string;
  sheetName: string;
  title: string;
  rows: ExtractedRow[];
  importType: 'EXPENSE' | 'INCOME' | 'CREDIT_CARD_SPEND' | 'IGNORE';
  targetWallet: 'EXPENSE_WALLET' | 'SAVINGS_WALLET' | 'CREDIT_CARD' | 'TEMPORARY_WALLET';
  defaultCategory: string;
  selectedCreditCardId?: number | null;
  selectedCustomWalletId?: number | null;
}

function parseDateStringToTimestamp(str: string): number {
  if (!str) return Date.now();
  const trimmed = str.trim();
  
  // Try direct Date parse
  const parsed = Date.parse(trimmed);
  if (!isNaN(parsed)) return parsed;

  // Try DD/MM/YYYY or DD-MM-YYYY
  const parts = trimmed.split(/[/.-]/);
  if (parts.length === 3) {
    const day = parseInt(parts[0], 10);
    const month = parseInt(parts[1], 10) - 1;
    let year = parseInt(parts[2], 10);
    if (year < 100) year += 2000;
    const d = new Date(year, month, day);
    if (!isNaN(d.getTime())) return d.getTime();
  }

  return Date.now();
}

function parseAmountStringToDouble(str: string): number {
  if (!str) return 0;
  // Remove currency signs, commas, spaces
  const cleaned = str.replace(/[^0-9.-]/g, '');
  const val = parseFloat(cleaned);
  return isNaN(val) ? 0 : Math.abs(val);
}

export async function parseExcelWorkbook(file: File): Promise<ExtractedTable[]> {
  const buffer = await file.arrayBuffer();
  const workbook = XLSX.read(buffer, { type: 'array', cellDates: true });
  const tables: ExtractedTable[] = [];

  for (const sheetName of workbook.SheetNames) {
    const worksheet = workbook.Sheets[sheetName];
    if (!worksheet) continue;

    // Convert sheet to 2D array of string cells
    const rawData: (string | number | Date | null | undefined)[][] = XLSX.utils.sheet_to_json(worksheet, {
      header: 1,
      raw: false,
      dateNF: 'yyyy-mm-dd',
    });

    if (!rawData || rawData.length === 0) continue;

    // Find header row (looks for date, amount/spent/debit/credit, desc/notes/details)
    let headerRowIndex = -1;
    let dateCol = -1;
    let descCol = -1;
    let amountCol = -1;
    let categoryCol = -1;

    for (let r = 0; r < Math.min(rawData.length, 15); r++) {
      const row = rawData[r] || [];
      const rowLower = row.map((cell) => String(cell || '').toLowerCase().trim());

      let foundDate = -1;
      let foundAmount = -1;
      let foundDesc = -1;
      let foundCategory = -1;

      for (let c = 0; c < rowLower.length; c++) {
        const val = rowLower[c];
        if (val.includes('date') || val.includes('time') || val.includes('when')) {
          foundDate = c;
        } else if (
          val.includes('amount') ||
          val.includes('spent') ||
          val.includes('debit') ||
          val.includes('credit') ||
          val.includes('cost') ||
          val.includes('price') ||
          val.includes('total') ||
          val.includes('inr') ||
          val.includes('rs') ||
          val.includes('usd')
        ) {
          foundAmount = c;
        } else if (
          val.includes('desc') ||
          val.includes('particular') ||
          val.includes('narration') ||
          val.includes('note') ||
          val.includes('remark') ||
          val.includes('item') ||
          val.includes('title')
        ) {
          foundDesc = c;
        } else if (val.includes('category') || val.includes('type') || val.includes('tag')) {
          foundCategory = c;
        }
      }

      // If we found at least date and amount (or desc and amount)
      if ((foundDate !== -1 && foundAmount !== -1) || (foundDesc !== -1 && foundAmount !== -1)) {
        headerRowIndex = r;
        dateCol = foundDate !== -1 ? foundDate : 0;
        amountCol = foundAmount;
        descCol = foundDesc !== -1 ? foundDesc : (foundDate === 0 ? 1 : 0);
        categoryCol = foundCategory;
        break;
      }
    }

    // If no header found, fallback to columns 0=Date, 1=Desc, 2=Amount
    if (headerRowIndex === -1) {
      headerRowIndex = 0;
      dateCol = 0;
      descCol = 1;
      amountCol = 2;
    }

    const rows: ExtractedRow[] = [];
    for (let r = headerRowIndex + 1; r < rawData.length; r++) {
      const row = rawData[r] || [];
      const rawDate = String(row[dateCol] || '').trim();
      const rawDesc = String(row[descCol] || '').trim();
      const rawAmount = String(row[amountCol] || '').trim();
      const rawCategory = categoryCol !== -1 ? String(row[categoryCol] || '').trim() : 'Other';

      const parsedAmount = parseAmountStringToDouble(rawAmount);
      if (parsedAmount > 0 || rawDesc.length > 0) {
        rows.push({
          originalRowIndex: r,
          rawDate,
          rawDesc: rawDesc || 'Transaction',
          rawAmount: rawAmount || String(parsedAmount),
          rawCategory: rawCategory || 'Other',
          dateTimestamp: parseDateStringToTimestamp(rawDate),
          parsedAmount,
        });
      }
    }

    if (rows.length > 0) {
      tables.push({
        id: `${sheetName}_${headerRowIndex}`,
        sheetName,
        title: `${sheetName} (${rows.length} rows)`,
        rows,
        importType: 'EXPENSE',
        targetWallet: 'EXPENSE_WALLET',
        defaultCategory: 'Other',
        selectedCreditCardId: null,
        selectedCustomWalletId: null,
      });
    }
  }

  return tables;
}
