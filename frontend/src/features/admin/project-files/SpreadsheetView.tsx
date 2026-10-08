import { useState } from 'react';
import { SegmentButton, SegmentGroup } from '@/components/navigation/PageTabs';
import { cn } from '@/lib/utils';
import {
  SPREADSHEET_PREVIEW_MAX_COLUMNS,
  SPREADSHEET_PREVIEW_MAX_ROWS,
  columnLetter,
  type SpreadsheetSheet,
} from './spreadsheetPreview';

const ALIGN_CLASS_NAMES = {
  left: 'text-left',
  center: 'text-center',
  right: 'text-right',
} as const;

/**
 * A read-only grid of an Excel workbook: one sheet at a time, with the column
 * letters and row numbers Excel shows, so a cell can be talked about by address.
 */
export function SpreadsheetView({ sheets }: Readonly<{ sheets: SpreadsheetSheet[] }>) {
  const [activeIndex, setActiveIndex] = useState(0);
  const sheet = sheets[activeIndex] ?? sheets[0];

  if (!sheet) {
    return (
      <p className="py-10 text-center text-sm text-muted-foreground">
        表示できるシートがありません。
      </p>
    );
  }

  const columns = Array.from({ length: sheet.columnCount }, (_, index) => index + 1);
  const isEmpty = sheet.rows.length === 0 || sheet.columnCount === 0;

  return (
    <div className="flex h-full min-h-0 flex-col gap-3">
      {sheets.length > 1 ? (
        <SegmentGroup aria-label="シート" className="shrink-0">
          {sheets.map((item, index) => (
            <SegmentButton
              key={item.name}
              active={index === activeIndex}
              onClick={() => setActiveIndex(index)}
            >
              {item.name}
            </SegmentButton>
          ))}
        </SegmentGroup>
      ) : null}

      {sheet.truncated ? (
        <p className="shrink-0 text-small text-muted-foreground">
          先頭{SPREADSHEET_PREVIEW_MAX_ROWS}行・{SPREADSHEET_PREVIEW_MAX_COLUMNS}列まで表示しています。すべての内容はダウンロードしてご確認ください。
        </p>
      ) : null}

      {isEmpty ? (
        <p className="py-10 text-center text-sm text-muted-foreground">このシートは空です。</p>
      ) : (
        <div
          className="min-h-0 flex-1 overflow-auto rounded-md border border-border bg-card"
          tabIndex={0}
          aria-label={`${sheet.name} の内容`}
        >
          <table className="border-separate border-spacing-0 text-small text-foreground">
            <caption className="sr-only">{sheet.name}</caption>
            <thead>
              <tr>
                <th
                  scope="col"
                  className="sticky left-0 top-0 z-20 min-w-10 border-b border-r border-border bg-muted"
                >
                  <span className="sr-only">行</span>
                </th>
                {columns.map((column) => (
                  <th
                    key={column}
                    scope="col"
                    className="sticky top-0 z-10 min-w-24 border-b border-r border-border bg-muted px-2 py-1 font-medium text-muted-foreground"
                  >
                    {columnLetter(column)}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {sheet.rows.map((cells, rowIndex) => (
                <tr key={rowIndex}>
                  <th
                    scope="row"
                    className="sticky left-0 z-10 border-b border-r border-border bg-muted px-2 py-1 text-right font-medium tabular-nums text-muted-foreground"
                  >
                    {rowIndex + 1}
                  </th>
                  {cells.map((cell, columnIndex) =>
                    cell ? (
                      <td
                        key={columnIndex}
                        rowSpan={cell.rowSpan > 1 ? cell.rowSpan : undefined}
                        colSpan={cell.colSpan > 1 ? cell.colSpan : undefined}
                        className={cn(
                          'max-w-80 whitespace-pre-wrap break-words border-b border-r border-border px-2 py-1 align-top',
                          cell.bold && 'font-bold',
                          cell.align && ALIGN_CLASS_NAMES[cell.align],
                        )}
                      >
                        {cell.text}
                      </td>
                    ) : null,
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <p className="shrink-0 text-small text-muted-foreground">
        簡易表示のため、書式・図形・入力規則などはExcelと異なる場合があります。
      </p>
    </div>
  );
}
