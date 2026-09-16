import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import type { ProjectPlanConditionalField } from '@/api/admin/types';
import { PlanFileValue } from './PlanFileValue';
import {
  cellAppliesUnderRules,
  formatPlanValue,
  isShownFor,
} from './planViewValues';

type PlanValueTableProps = {
  projectId: string;
  field: FieldConfig;
  rows: unknown;
  conditionalFields: ReadonlyArray<ProjectPlanConditionalField>;
};

const isRow = (value: unknown): value is Record<string, unknown> =>
  Boolean(value) && typeof value === 'object' && !Array.isArray(value);

/**
 * A repeating table of a saved plan page, read-only.
 *
 * Column names are row-relative, so every show/hide rule — the column's own
 * and the page's 有/無 rules — is decided against that row alone.
 */
export function PlanValueTable({
  projectId,
  field,
  rows,
  conditionalFields,
}: Readonly<PlanValueTableProps>) {
  const columns = field.columns ?? [];
  const tableRows = Array.isArray(rows) ? rows.filter(isRow) : [];

  if (tableRows.length === 0) {
    return <p className="text-sm text-muted-foreground">データなし</p>;
  }

  return (
    <div className="overflow-x-auto rounded-md border border-border">
      <table className="w-full border-collapse text-sm">
        <thead className="bg-muted/50">
          <tr>
            <th scope="col" className="w-12 px-3 py-2 text-left font-medium text-muted-foreground">
              No.
            </th>
            {columns.map((column) => (
              <th
                key={String(column.name)}
                scope="col"
                className="px-3 py-2 text-left font-medium text-muted-foreground"
                style={column.width ? { minWidth: column.width } : undefined}
              >
                {column.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {tableRows.map((row, index) => (
            <tr key={index} className="border-t border-border align-top">
              <td className="px-3 py-2 text-muted-foreground">{index + 1}</td>
              {columns.map((column) => {
                const columnName = String(column.name);
                const applies =
                  isShownFor(column, (path) => row[path]) &&
                  cellAppliesUnderRules(String(field.name), columnName, row, conditionalFields);
                const value = row[columnName];

                return (
                  <td key={columnName} className="whitespace-pre-wrap break-words px-3 py-2">
                    {!applies ? (
                      <span className="text-muted-foreground">—</span>
                    ) : column.type === 'file' && typeof value === 'string' && value ? (
                      <PlanFileValue projectId={projectId} s3Key={value} />
                    ) : (
                      formatPlanValue(column, value)
                    )}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
