import { useId } from 'react';
import { useWatch, type UseFormReturn } from 'react-hook-form';
import { FormGrid } from '@/components/forms/form-layout';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  fiscalYearsInPeriod,
  planYearRows,
  yearKey,
  type PlanSummaryFormValues,
} from './planSummary';

type PlanSummarySectionProps = Readonly<{
  form: UseFormReturn<PlanSummaryFormValues>;
  disabled?: boolean;
  /** Why the last save of these values was refused, if it was. */
  error?: string | null;
}>;

/**
 * 認証対象期間 and the planned member counts per 年度 — the values of the
 * 計画書 this system keeps.
 *
 * The 年度 rows follow the period: once both dates are in, there is one row for
 * each 年度 the period touches and no others, so the operator types only the
 * years their plan is about. 延べ会員数 is the running total of 新規会員数, as
 * in the official sheet, and is shown rather than typed.
 */
export function PlanSummarySection({ form, disabled = false, error }: PlanSummarySectionProps) {
  const id = useId();
  const values = useWatch({ control: form.control }) as PlanSummaryFormValues;
  const years = fiscalYearsInPeriod(values.certTargetPeriodStart, values.certTargetPeriodEnd);
  const rows = planYearRows(values);

  let runningTotal = 0;
  const totals = rows.map((row) => {
    runningTotal += row.newMembersTotal;
    return runningTotal;
  });

  return (
    <div className="space-y-5">
      <FormGrid>
        <div className="space-y-2">
          <Label htmlFor={`${id}-start`}>認証対象期間（開始日）</Label>
          <Input
            id={`${id}-start`}
            type="date"
            disabled={disabled}
            {...form.register('certTargetPeriodStart')}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor={`${id}-end`}>認証対象期間（終了日）</Label>
          <Input
            id={`${id}-end`}
            type="date"
            disabled={disabled}
            {...form.register('certTargetPeriodEnd')}
          />
        </div>
      </FormGrid>

      {years.length === 0 ? (
        <p className="rounded-md border border-dashed border-border px-4 py-6 text-center text-sm text-muted-foreground">
          認証対象期間の開始日と終了日を入力すると、年度ごとの会員数（計画値）の入力欄が表示されます。
        </p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[560px] text-left text-sm">
            <caption className="pb-2 text-left text-sm text-muted-foreground">
              会員数の計画値（年度別）。計画書「4.排出削減計画」の値を入力してください。
            </caption>
            <thead className="border-b border-border text-muted-foreground">
              <tr>
                <th scope="col" className="px-2 py-2 font-normal">年度</th>
                <th scope="col" className="px-2 py-2 font-normal">新規会員数（件）</th>
                <th scope="col" className="px-2 py-2 font-normal">
                  認証対象となる累計会員数（件）
                </th>
                <th scope="col" className="px-2 py-2 font-normal">延べ会員数（件）</th>
              </tr>
            </thead>
            <tbody>
              {years.map((year, index) => (
                <tr key={year} className="border-b border-border">
                  <th scope="row" className="whitespace-nowrap px-2 py-2 font-normal">
                    {year}年度
                  </th>
                  <td className="px-2 py-2">
                    <Input
                      type="number"
                      min={0}
                      step={1}
                      inputMode="numeric"
                      disabled={disabled}
                      aria-label={`${year}年度 新規会員数（件）`}
                      className="w-32"
                      {...form.register(`yearly.${yearKey(year)}.newMembersTotal`)}
                    />
                  </td>
                  <td className="px-2 py-2">
                    <Input
                      type="number"
                      min={0}
                      step={1}
                      inputMode="numeric"
                      disabled={disabled}
                      aria-label={`${year}年度 認証対象となる累計会員数（件）`}
                      className="w-32"
                      {...form.register(`yearly.${yearKey(year)}.cumulativeMembersTotal`)}
                    />
                  </td>
                  <td className="px-2 py-2 text-muted-foreground">{totals[index]}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {error ? (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      ) : null}
    </div>
  );
}
