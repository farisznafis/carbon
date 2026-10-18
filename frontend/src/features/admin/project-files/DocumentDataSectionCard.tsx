import { Check, Copy } from 'lucide-react';
import type { DocumentDataSection } from '@/api/admin/projectDocumentData';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';
import { EMPTY_VALUE, copyValueText, displayValue, isNumericType } from './documentDataCopy';

/** Copies one value; `key` identifies the control that shows the ✓. */
export type CopyHandler = (key: string, text: string, label: string) => void;

type CopyIconButtonProps = Readonly<{
  label: string;
  copied: boolean;
  disabled?: boolean;
  onClick: () => void;
}>;

/** An icon-only copy control: always visible, so touch and keyboard users find it too. */
function CopyIconButton({ label, copied, disabled, onClick }: CopyIconButtonProps) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          type="button"
          size="sm"
          variant="ghost"
          className={cn(
            'w-8 shrink-0 px-0 text-muted-foreground hover:text-foreground',
            copied && 'text-success-strong hover:text-success-strong',
          )}
          aria-label={`${label}をコピー`}
          disabled={disabled}
          onClick={onClick}
        >
          {copied ? <Check aria-hidden="true" /> : <Copy aria-hidden="true" />}
        </Button>
      </TooltipTrigger>
      <TooltipContent>{copied ? 'コピーしました' : 'コピー'}</TooltipContent>
    </Tooltip>
  );
}

type DocumentDataSectionCardProps = Readonly<{
  section: DocumentDataSection;
  copiedKey: string | null;
  onCopy: CopyHandler;
}>;

/**
 * One section of a form: its label/value items, then its table if it has one.
 * Each value copies on its own, to be pasted into one cell at a time.
 */
export function DocumentDataSectionCard({ section, copiedKey, onCopy }: DocumentDataSectionCardProps) {
  const headingId = `document-data-${section.key}`;
  const { table } = section;

  return (
    <section aria-labelledby={headingId} className="space-y-2">
      <h3 id={headingId} className="px-1 text-sm font-bold text-foreground">
        {section.title}
      </h3>

      <div className="overflow-hidden rounded-lg border border-border">
        {section.items.length > 0 && (
          <dl className="divide-y divide-border">
            {section.items.map((item) => {
              const copyKey = `${section.key}::${item.key}`;
              const text = copyValueText(item.value);
              return (
                <div
                  key={item.key}
                  className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-0.5 py-2 pl-4 pr-2 transition-colors hover:bg-muted/40 sm:grid-cols-[minmax(10rem,16rem)_minmax(0,1fr)_auto]"
                >
                  <dt className="text-small text-muted-foreground">{item.label}</dt>
                  <dd
                    className={cn(
                      'order-3 min-w-0 text-sm font-bold text-foreground [overflow-wrap:anywhere] sm:order-none',
                      (isNumericType(item.type) || item.type === 'DATE') && 'tabular-nums',
                      !text && 'font-normal text-muted-foreground',
                    )}
                  >
                    {displayValue(item.value)}
                  </dd>
                  <dd className="row-span-2 sm:row-span-1">
                    <CopyIconButton
                      label={item.label}
                      copied={copiedKey === copyKey}
                      disabled={!text}
                      onClick={() => onCopy(copyKey, text, item.label)}
                    />
                  </dd>
                </div>
              );
            })}
          </dl>
        )}

        {table && (
          <div className={cn(section.items.length > 0 && 'border-t border-border')}>
            {/* Only the table scrolls sideways; the page never does. */}
            <Table className="text-small" aria-label={`${section.title}の表`}>
              <TableHeader>
                <TableRow className="hover:bg-transparent">
                  {table.columns.map((column) => (
                    <TableHead
                      key={column.key}
                      className={cn(
                        'h-9 whitespace-nowrap bg-muted/40 px-3 text-small text-muted-foreground',
                        isNumericType(column.type) && 'text-right',
                      )}
                    >
                      {column.label}
                    </TableHead>
                  ))}
                </TableRow>
              </TableHeader>
              <TableBody>
                {table.rows.map((row, rowIndex) => {
                  const rowName = copyValueText(row[0]) || `${rowIndex + 1}行目`;
                  return (
                    <TableRow key={rowIndex}>
                      {table.columns.map((column, columnIndex) => {
                        const cellKey = `${section.key}::table::${rowIndex}::${column.key}`;
                        const text = copyValueText(row[columnIndex]);
                        const copied = copiedKey === cellKey;
                        const cellLabel = columnIndex === 0 ? column.label : `${rowName} ${column.label}`;
                        return (
                          <TableCell key={column.key} className="p-0">
                            <button
                              type="button"
                              className={cn(
                                'group flex min-h-9 w-full items-center gap-1.5 whitespace-nowrap px-3 py-1.5 text-left text-sm text-foreground transition-colors hover:bg-primary/5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring disabled:cursor-default disabled:text-muted-foreground disabled:hover:bg-transparent',
                                isNumericType(column.type) && 'justify-end tabular-nums',
                                copied && 'bg-success-subtle',
                              )}
                              aria-label={`${cellLabel}をコピー`}
                              disabled={!text}
                              onClick={() => onCopy(cellKey, text, cellLabel)}
                            >
                              <span>{text || EMPTY_VALUE}</span>
                              {text && (
                                <span aria-hidden="true" className="inline-flex size-3.5 shrink-0">
                                  {copied ? (
                                    <Check className="size-3.5 text-success-strong" />
                                  ) : (
                                    <Copy className="size-3.5 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100 group-focus-visible:opacity-100" />
                                  )}
                                </span>
                              )}
                            </button>
                          </TableCell>
                        );
                      })}
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
            {table.rows.length === 0 && (
              <p className="px-4 py-3 text-small text-muted-foreground">表のデータはありません。</p>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
