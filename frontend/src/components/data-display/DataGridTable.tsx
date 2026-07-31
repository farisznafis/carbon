import { ArrowDown, ArrowUp, ChevronsUpDown } from 'lucide-react';
import type { CSSProperties, KeyboardEvent, ReactNode } from 'react';
import { useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

export type DataGridTableColumn<T> = {
  id: string;
  label: string;
  sortable?: boolean;
  className?: string;
  render: (row: T) => ReactNode;
  sortValue?: (row: T) => string | number | null | undefined;
};

type SortDirection = 'asc' | 'desc';

type DataGridTableProps<T> = {
  rows: T[];
  columns: DataGridTableColumn<T>[];
  gridTemplateColumns: string;
  minWidth?: number;
  getRowKey: (row: T) => string;
  emptyMessage: ReactNode;
  onRowClick?: (row: T) => void;
};

function normalizeSortValue(value: string | number | null | undefined) {
  if (value === null || value === undefined) return '';
  return value;
}

function compareSortValues(
  a: string | number | null | undefined,
  b: string | number | null | undefined,
  direction: SortDirection,
) {
  const aValue = normalizeSortValue(a);
  const bValue = normalizeSortValue(b);

  const result =
    typeof aValue === 'number' && typeof bValue === 'number'
      ? aValue - bValue
      : String(aValue).localeCompare(String(bValue), 'ja', {
          numeric: true,
          sensitivity: 'base',
        });

  return direction === 'asc' ? result : -result;
}

export function DataGridTable<T>({
  rows,
  columns,
  gridTemplateColumns,
  minWidth = 980,
  getRowKey,
  emptyMessage,
  onRowClick,
}: DataGridTableProps<T>) {
  const [sortColumnId, setSortColumnId] = useState<string | null>(null);
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const gridStyle = {
    gridTemplateColumns,
  } satisfies CSSProperties;

  const sortedRows = useMemo(() => {
    if (!sortColumnId) return rows;

    const column = columns.find((item) => item.id === sortColumnId);
    if (!column?.sortValue) return rows;

    return [...rows].sort((a, b) =>
      compareSortValues(
        column.sortValue?.(a),
        column.sortValue?.(b),
        sortDirection,
      ),
    );
  }, [columns, rows, sortColumnId, sortDirection]);

  const handleHeaderClick = (column: DataGridTableColumn<T>) => {
    if (!column.sortable) return;

    if (sortColumnId === column.id) {
      setSortDirection((current) => (current === 'asc' ? 'desc' : 'asc'));
      return;
    }

    setSortColumnId(column.id);
    setSortDirection('asc');
  };

  const handleRowKeyDown = (event: KeyboardEvent<HTMLDivElement>, row: T) => {
    if (!onRowClick) return;

    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onRowClick(row);
    }
  };

  return (
    <div className="overflow-x-auto rounded-xl border border-border bg-card">
      <div
        className="grid gap-4 border-b border-border bg-muted/40 px-5 py-3 text-table font-bold text-foreground"
        style={{ ...gridStyle, minWidth }}
      >
        {columns.map((column) => {
          const isActiveSort = sortColumnId === column.id;
          const SortIcon = isActiveSort
            ? sortDirection === 'asc'
              ? ArrowUp
              : ArrowDown
            : ChevronsUpDown;

          return column.sortable ? (
            <Button
              key={column.id}
              type="button"
              variant="ghost"
              onClick={() => handleHeaderClick(column)}
              className={cn(
                'h-auto justify-start gap-1 p-0 text-table font-bold text-foreground hover:bg-transparent hover:text-primary',
                column.className,
              )}
            >
              <span className="truncate">{column.label}</span>
              <SortIcon
                className={cn(
                  'h-3.5 w-3.5 shrink-0',
                  !isActiveSort && 'text-muted-foreground',
                )}
              />
            </Button>
          ) : (
            <span key={column.id} className={column.className}>
              {column.label}
            </span>
          );
        })}
      </div>

      {sortedRows.length === 0 ? (
        <div className="px-5 py-8 text-center text-small text-foreground">
          {emptyMessage}
        </div>
      ) : (
        sortedRows.map((row) => (
          <div
            key={getRowKey(row)}
            role={onRowClick ? 'button' : undefined}
            tabIndex={onRowClick ? 0 : undefined}
            onClick={() => onRowClick?.(row)}
            onKeyDown={(event) => handleRowKeyDown(event, row)}
            className={cn(
              'grid min-h-[60px] items-center gap-4 border-b border-border px-5 py-3 last:border-b-0',
              onRowClick && 'cursor-pointer transition hover:bg-muted/40',
            )}
            style={{ ...gridStyle, minWidth }}
          >
            {columns.map((column) => (
              <div key={column.id} className={cn('min-w-0', column.className)}>
                {column.render(row)}
              </div>
            ))}
          </div>
        ))
      )}
    </div>
  );
}
