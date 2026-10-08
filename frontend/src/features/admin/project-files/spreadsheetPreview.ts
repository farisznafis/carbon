import type ExcelJSModule from 'exceljs';

/** Enough of a plan workbook to read it; the rest is for the downloaded file. */
export const SPREADSHEET_PREVIEW_MAX_ROWS = 500;
export const SPREADSHEET_PREVIEW_MAX_COLUMNS = 50;

export type SpreadsheetCell = {
  text: string;
  rowSpan: number;
  colSpan: number;
  bold: boolean;
  align?: 'left' | 'center' | 'right';
};

export type SpreadsheetSheet = {
  name: string;
  columnCount: number;
  /** One entry per row; null where a merged cell above or to the left covers it. */
  rows: (SpreadsheetCell | null)[][];
  truncated: boolean;
};

/** Column letters as Excel shows them: 1 → A, 27 → AA. */
export function columnLetter(index: number): string {
  let letters = '';
  let n = index;
  while (n > 0) {
    const remainder = (n - 1) % 26;
    letters = String.fromCharCode(65 + remainder) + letters;
    n = Math.floor((n - 1) / 26);
  }
  return letters;
}

/** "B3" → { row: 3, col: 2 }. */
function decodeAddress(address: string) {
  const match = /^\$?([A-Z]+)\$?(\d+)$/.exec(address.toUpperCase());
  if (!match) return null;
  const col = [...match[1]].reduce((total, char) => total * 26 + char.charCodeAt(0) - 64, 0);
  return { row: Number(match[2]), col };
}

const alignmentOf = (horizontal?: string): SpreadsheetCell['align'] =>
  horizontal === 'center' || horizontal === 'centerContinuous'
    ? 'center'
    : horizontal === 'right'
      ? 'right'
      : horizontal === 'left'
        ? 'left'
        : undefined;

function readSheet(worksheet: ExcelJSModule.Worksheet): SpreadsheetSheet {
  const rowCount = Math.min(worksheet.rowCount, SPREADSHEET_PREVIEW_MAX_ROWS);
  const columnCount = Math.min(worksheet.columnCount, SPREADSHEET_PREVIEW_MAX_COLUMNS);

  // Top-left cell of each merge → its span; every other cell of the merge is covered.
  const spans = new Map<string, { rowSpan: number; colSpan: number }>();
  const covered = new Set<string>();
  const merges: string[] = (worksheet.model as { merges?: string[] }).merges ?? [];
  for (const range of merges) {
    const [from, to] = range.split(':').map(decodeAddress);
    if (!from || !to) continue;
    spans.set(`${from.row}:${from.col}`, {
      rowSpan: Math.min(to.row, rowCount) - from.row + 1,
      colSpan: Math.min(to.col, columnCount) - from.col + 1,
    });
    for (let r = from.row; r <= to.row; r += 1) {
      for (let c = from.col; c <= to.col; c += 1) {
        if (r !== from.row || c !== from.col) covered.add(`${r}:${c}`);
      }
    }
  }

  const rows: (SpreadsheetCell | null)[][] = [];
  for (let r = 1; r <= rowCount; r += 1) {
    const row = worksheet.getRow(r);
    const cells: (SpreadsheetCell | null)[] = [];
    for (let c = 1; c <= columnCount; c += 1) {
      if (covered.has(`${r}:${c}`)) {
        cells.push(null);
        continue;
      }
      const cell = row.getCell(c);
      const span = spans.get(`${r}:${c}`);
      let text = '';
      try {
        text = cell.text ?? '';
      } catch {
        // A value exceljs cannot format (e.g. a broken formula) shows as empty.
      }
      cells.push({
        text,
        rowSpan: span?.rowSpan ?? 1,
        colSpan: span?.colSpan ?? 1,
        bold: Boolean(cell.font?.bold),
        align: alignmentOf(cell.alignment?.horizontal),
      });
    }
    rows.push(cells);
  }

  return {
    name: worksheet.name,
    columnCount,
    rows,
    truncated:
      worksheet.rowCount > SPREADSHEET_PREVIEW_MAX_ROWS ||
      worksheet.columnCount > SPREADSHEET_PREVIEW_MAX_COLUMNS,
  };
}

/**
 * Reads the visible sheets of an .xlsx file into plain cell text.
 *
 * exceljs is loaded only here, so the preview costs nothing until an Excel
 * file is actually opened.
 */
export async function readSpreadsheet(buffer: ArrayBuffer): Promise<SpreadsheetSheet[]> {
  const imported = await import('exceljs');
  const ExcelJS = ((imported as { default?: typeof ExcelJSModule }).default ??
    imported) as typeof ExcelJSModule;

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(buffer);

  return workbook.worksheets
    .filter((worksheet) => worksheet.state === undefined || worksheet.state === 'visible')
    .map(readSheet);
}
