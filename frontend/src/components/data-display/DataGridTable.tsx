import { ArrowDown, ArrowUp, ChevronsUpDown } from 'lucide-react';
import type { CSSProperties, MouseEvent, ReactNode } from 'react';
import { useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

export type DataGridTableColumn<T> = {
  id: string;
  label: string;
  sortable?: boolean;
  className?: string;
  sticky?: 'left' | 'right';
  render: (row: T) => ReactNode;
  sortValue?: (
    row: T,
  ) => string | number | null | undefined;
};

type SortDirection = 'asc' | 'desc';

/**
 * Controls whose own clicks must not also trigger the row action. Wrap any
 * other non-navigating area (e.g. a tooltip trigger) in `data-row-click-ignore`.
 */
const INTERACTIVE_ROW_CHILD_SELECTOR =
  'button, a, input, select, textarea, label, [role="button"], [role="combobox"], [role="checkbox"], [role="switch"], [role="menuitem"], [data-row-click-ignore]';

type DataGridTableProps<T> = {
  rows: T[];
  columns: DataGridTableColumn<T>[];
  gridTemplateColumns: string;
  minWidth?: number;
  getRowKey: (row: T) => string;
  emptyMessage: ReactNode;
  /**
   * Mouse shortcut for the row's main action. The row is not focusable, so it
   * must also render a `RowActionButton` doing the same thing for keyboard and
   * screen-reader users.
   */
  onRowClick?: (row: T) => void;
  /** Extra classes for one row, e.g. to highlight rows that need attention. */
  getRowClassName?: (row: T) => string | undefined;
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

function getStickyHeaderClass(
  sticky: DataGridTableColumn<unknown>['sticky'],
) {
  if (sticky === 'right') {
    return [
      'sticky right-0 z-20',
      'self-stretch',
      'border-l border-border',
      'bg-muted',
      'pl-4',
    ].join(' ');
  }

  if (sticky === 'left') {
    return [
      'sticky left-0 z-20',
      'self-stretch',
      'border-r border-border',
      'bg-muted',
      'pr-4',
    ].join(' ');
  }

  return undefined;
}

function getStickyCellClass(
  sticky: DataGridTableColumn<unknown>['sticky'],
) {
  if (sticky === 'right') {
    return [
      'sticky right-0 z-10',
      'self-stretch',
      'flex items-center',
      'border-l border-border',
      'bg-card',
      'pl-4',
      'group-hover:bg-muted',
    ].join(' ');
  }

  if (sticky === 'left') {
    return [
      'sticky left-0 z-10',
      'self-stretch',
      'flex items-center',
      'border-r border-border',
      'bg-card',
      'pr-4',
      'group-hover:bg-muted',
    ].join(' ');
  }

  return undefined;
}

export function DataGridTable<T>({
  rows,
  columns,
  gridTemplateColumns,
  minWidth = 980,
  getRowKey,
  emptyMessage,
  onRowClick,
  getRowClassName,
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

  /** Runs the row action unless the click belongs to a control inside the row or ends a text selection. */
  const handleRowClick = (event: MouseEvent<HTMLDivElement>, row: T) => {
    if (!onRowClick) return;

    const target = event.target as Element | null;
    // React bubbles portal events (select menus, dialogs) through the row even
    // though they are not inside it in the DOM.
    if (!target || !event.currentTarget.contains(target)) return;
    if (target.closest(INTERACTIVE_ROW_CHILD_SELECTOR)) return;
    if (window.getSelection()?.toString()) return;

    onRowClick(row);
  };

  return (
    <div className='overflow-x-auto rounded-xl border border-border bg-card'>
      <div
        className='grid gap-4 border-b border-border bg-muted/40 px-5 py-3 text-table font-bold text-foreground'
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
              type='button'
              variant='ghost'
              onClick={() => handleHeaderClick(column)}
              className={cn(
                'h-auto justify-start gap-1 p-0 text-table font-bold text-foreground hover:text-primary',
                getStickyHeaderClass(column.sticky),
                !column.sticky && 'hover:bg-transparent',
                column.className,
              )}
            >
              <span className='truncate'>{column.label}</span>
              <SortIcon
                className={cn(
                  'h-3.5 w-3.5 shrink-0',
                  !isActiveSort && 'text-muted-foreground',
                )}
              />
            </Button>
          ) : (
            <span
              key={column.id}
              className={cn(
                'truncate text-table font-bold text-foreground',
                getStickyHeaderClass(column.sticky),
                column.className,
              )}
            >
              {column.label}
            </span>
          );
        })}
      </div>

      {sortedRows.length === 0 ? (
        <div className='px-5 py-8 text-center text-small text-foreground'>
          {emptyMessage}
        </div>
      ) : (
        sortedRows.map((row) => (
          <div
            key={getRowKey(row)}
            onClick={(event) => handleRowClick(event, row)}
            className={cn(
              'group grid min-h-[60px] items-center gap-4 border-b border-border px-5 py-3 last:border-b-0',
              onRowClick &&
                'cursor-pointer transition hover:bg-muted/40',
              getRowClassName?.(row),
            )}
            style={{ ...gridStyle, minWidth }}
          >
            {columns.map((column) => (
              <div
                key={column.id}
                className={cn(
                  'min-w-0',
                  getStickyCellClass(column.sticky),
                  column.className,
                )}
              >
                {column.render(row)}
              </div>
            ))}
          </div>
        ))
      )}
    </div>
  );
}
