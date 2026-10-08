import ExcelJS from 'exceljs';
import { describe, expect, it } from 'vitest';
import { columnLetter, readSpreadsheet } from './spreadsheetPreview';

async function workbookBuffer(build: (workbook: ExcelJS.Workbook) => void) {
  const workbook = new ExcelJS.Workbook();
  build(workbook);
  return (await workbook.xlsx.writeBuffer()) as ArrayBuffer;
}

describe('columnLetter', () => {
  it('names columns the way Excel does', () => {
    expect([1, 26, 27, 52, 703].map(columnLetter)).toEqual(['A', 'Z', 'AA', 'AZ', 'AAA']);
  });
});

describe('readSpreadsheet', () => {
  it('reads cell text, bold, alignment and merged cells of visible sheets only', async () => {
    const buffer = await workbookBuffer((workbook) => {
      const sheet = workbook.addWorksheet('様式1');
      sheet.getCell('A1').value = 'プロジェクト計画書';
      sheet.getCell('A1').font = { bold: true };
      sheet.getCell('A1').alignment = { horizontal: 'center' };
      sheet.mergeCells('A1:C1');
      sheet.getCell('A2').value = '会員数';
      sheet.getCell('B2').value = 12;
      workbook.addWorksheet('hidden', { state: 'hidden' }).getCell('A1').value = 'x';
    });

    const sheets = await readSpreadsheet(buffer);

    expect(sheets.map((sheet) => sheet.name)).toEqual(['様式1']);
    const [first] = sheets;
    expect(first.columnCount).toBe(3);
    expect(first.rows[0][0]).toMatchObject({
      text: 'プロジェクト計画書',
      colSpan: 3,
      bold: true,
      align: 'center',
    });
    expect(first.rows[0][1]).toBeNull();
    expect(first.rows[0][2]).toBeNull();
    expect(first.rows[1].map((cell) => cell?.text)).toEqual(['会員数', '12', '']);
    expect(first.truncated).toBe(false);
  });
});
