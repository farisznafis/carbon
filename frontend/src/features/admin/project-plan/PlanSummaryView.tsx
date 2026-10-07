import { planYearRows, type PlanSummaryFormValues } from './planSummary';

const formatDate = (value?: string) => (value ? value.split("-").join("/") : '—');

/** 認証対象期間 and the planned member counts per 年度, read-only. */
export function PlanSummaryView({ values }: Readonly<{ values: PlanSummaryFormValues }>) {
  const rows = planYearRows(values);

  if (!values.certTargetPeriodStart && !values.certTargetPeriodEnd) {
    return (
      <p className="rounded-md border border-dashed border-border px-4 py-6 text-center text-sm text-muted-foreground">
        認証対象期間・会員数計画はまだ入力されていません。
      </p>
    );
  }

  let runningTotal = 0;

  return (
    <div className="space-y-4">
      <dl className="text-sm">
        <dt className="text-muted-foreground">認証対象期間</dt>
        <dd className="mt-1">
          {formatDate(values.certTargetPeriodStart)} ～ {formatDate(values.certTargetPeriodEnd)}
        </dd>
      </dl>

      {rows.length > 0 ? (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[560px] text-left text-sm">
            <caption className="pb-2 text-left text-sm text-muted-foreground">
              会員数の計画値（年度別）
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
              {rows.map((row) => {
                runningTotal += row.newMembersTotal;
                return (
                  <tr key={row.year} className="border-b border-border">
                    <th scope="row" className="whitespace-nowrap px-2 py-2 font-normal">
                      {row.year}年度
                    </th>
                    <td className="px-2 py-2">{row.newMembersTotal}</td>
                    <td className="px-2 py-2">{row.cumulativeMembersTotal}</td>
                    <td className="px-2 py-2 text-muted-foreground">{runningTotal}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  );
}
