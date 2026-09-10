'use client';

import { useEffect } from 'react';
import {
  useFieldArray,
  useFormContext,
  type ArrayPath,
  type FieldArray,
  type FieldValues,
  type Path,
} from 'react-hook-form';
import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { RequiredBadge } from '@/components/forms/shared/RequiredLabel';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { errorAt, FieldControl, useFieldDisabled } from './DynamicFormField';

/**
 * A repeating table: one row of the official form per row here.
 *
 * Most of プロジェクト計画書 is tables — 4.排出削減計画 has a row per 年度,
 * 11.モニタリング計画 a row per モニタリング項目 — and the form's unit is the
 * row, not the column. Rendering one set of inputs per page, as the wizard did
 * before this, could only ever capture the first row.
 *
 * Column configs are row-relative (`symbol`, not `rows.0.symbol`); the row
 * index is applied here, so one config serves every row and the read-only and
 * 有/無 rules the page was given apply identically down the column.
 */
export function PlanTableField<TFormValues extends FieldValues = FieldValues>({
  field,
  name,
}: {
  field: FieldConfig<TFormValues>;
  name: Path<TFormValues>;
}) {
  const form = useFormContext<TFormValues>();
  const { fields, append, remove, replace } = useFieldArray<TFormValues>({
    control: form.control,
    name: name as unknown as ArrayPath<TFormValues>,
  });

  const columns = field.columns ?? [];
  const minRows = field.minRows ?? 0;

  // The wizard loads a page by setting the whole page object at once, which is
  // a write the field array does not see — the 38 年度 the server just built
  // would land in the form values and render as an empty table. Reconciling on
  // length keeps add/remove untouched, because those move both counts together.
  const loaded = form.watch(name) as unknown[] | undefined;
  const loadedLength = Array.isArray(loaded) ? loaded.length : 0;
  useEffect(() => {
    if (loadedLength === fields.length) return;
    replace((Array.isArray(loaded) ? loaded : []) as never);
    // `loaded` itself is a new array identity on every render; the length is
    // what actually says the rows changed underneath us.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loadedLength, fields.length, replace]);
  // A derived table — the 年度 skeleton the server builds from 認証対象期間 —
  // has exactly the rows it should. Offering 行を追加 there would produce a row
  // the server discards on the next read.
  const editable = field.fixedRows !== true;

  useEffect(() => {
    if (fields.length >= minRows) return;
    for (let i = fields.length; i < minRows; i += 1) {
      append({} as FieldArray<TFormValues, ArrayPath<TFormValues>>, {
        shouldFocus: false,
      });
    }
  }, [fields.length, minRows, append]);

  const atMax = field.maxRows !== undefined && fields.length >= field.maxRows;
  // NotEmpty on the list, or anything else the server said about the table
  // itself rather than about one of its cells.
  const tableError = errorAt(form.formState.errors, name as string);
  const tableMessage =
    tableError && !Array.isArray(tableError) ? tableError.message?.toString() : undefined;

  return (
    <div className="space-y-2 md:col-span-2">
      <div className="flex flex-wrap items-center gap-1 text-sm font-medium">
        <span>{field.label}</span>
        {field.required !== false && <RequiredBadge />}
      </div>

      {/* Wide tables scroll inside their own box; the page must not. */}
      <div className="overflow-x-auto rounded-md border">
        <table className="w-full border-collapse text-sm">
          <thead className="bg-muted/50">
            <tr>
              <th className="w-12 border-b px-2 py-2 text-left font-medium text-muted-foreground">
                #
              </th>
              {columns.map((column) => (
                <th
                  key={column.name}
                  scope="col"
                  className="border-b px-2 py-2 text-left font-medium align-bottom"
                  style={column.width ? { minWidth: column.width } : undefined}
                >
                  <span className="inline-flex flex-wrap items-center gap-1">
                    <span>{column.label}</span>
                    {column.required !== false && <RequiredBadge />}
                  </span>
                </th>
              ))}
              {editable && <th className="w-16 border-b px-2 py-2" />}
            </tr>
          </thead>
          <tbody>
            {fields.map((row, index) => (
              <tr key={row.id} className="align-top">
                <td className="border-b px-2 py-2 text-muted-foreground">{index + 1}</td>
                {columns.map((column) => (
                  <TableCell<TFormValues>
                    key={column.name}
                    column={column}
                    rowPath={`${name}.${index}` as Path<TFormValues>}
                  />
                ))}
                {editable && (
                  <td className="border-b px-2 py-2">
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      // Dropping below the minimum would leave the page
                      // unsaveable with no way back up but 行を追加.
                      disabled={fields.length <= minRows}
                      onClick={() => remove(index)}
                    >
                      削除
                    </Button>
                  </td>
                )}
              </tr>
            ))}

            {fields.length === 0 && (
              <tr>
                <td
                  colSpan={columns.length + (editable ? 2 : 1)}
                  className="px-2 py-4 text-center text-muted-foreground"
                >
                  行がありません
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {editable && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={atMax}
          onClick={() =>
            append({} as FieldArray<TFormValues, ArrayPath<TFormValues>>)
          }
        >
          {field.addRowLabel ?? '行を追加'}
        </Button>
      )}

      {tableMessage && <p className="text-sm text-destructive">{tableMessage}</p>}
    </div>
  );
}

/** One cell: the column's control bound to this row, plus its own error. */
function TableCell<TFormValues extends FieldValues = FieldValues>({
  column,
  rowPath,
}: {
  column: FieldConfig<TFormValues> & { width?: string };
  rowPath: Path<TFormValues>;
}) {
  const form = useFormContext<TFormValues>();
  const prefix = `${rowPath}.`;
  const path = `${prefix}${column.name}` as Path<TFormValues>;

  // Conditions inside a row refer to sibling columns, so they resolve against
  // this row rather than the page.
  const disabled = useFieldDisabled(column, prefix);
  const visible = isVisible(column, prefix, form.watch);

  const error = errorAt(form.formState.errors, path as string);
  const errorId = `${path}-error`;

  return (
    <td className="border-b px-2 py-2">
      {visible ? (
        <>
          <FieldControl<TFormValues>
            field={column}
            name={path}
            fieldId={path as string}
            disabled={disabled}
            accessibilityProps={{
              'aria-invalid': !!error,
              'aria-describedby': error ? errorId : undefined,
              'aria-required': column.required !== false,
              'aria-label': column.label,
            }}
          />
          {error && (
            <p id={errorId} className={cn('mt-1 text-xs text-destructive')}>
              {error.message?.toString()}
            </p>
          )}
        </>
      ) : (
        <span className="text-muted-foreground">—</span>
      )}
    </td>
  );
}

function isVisible<TFormValues extends FieldValues>(
  column: FieldConfig<TFormValues>,
  prefix: string,
  watch: (path: Path<TFormValues>) => unknown,
) {
  const matches = (
    condition: { field: string; value: unknown },
  ) => {
    const value = watch(`${prefix}${condition.field}` as Path<TFormValues>);
    return Array.isArray(condition.value)
      ? condition.value.includes(value as never)
      : value === condition.value;
  };

  if (column.hideWhen) {
    const conditions = Array.isArray(column.hideWhen) ? column.hideWhen : [column.hideWhen];
    if (conditions.every(matches)) return false;
  }

  if (!column.showWhen) return true;
  const conditions = Array.isArray(column.showWhen) ? column.showWhen : [column.showWhen];
  return conditions.every(matches);
}
