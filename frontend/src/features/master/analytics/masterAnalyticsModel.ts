import type { ReductionResultItem } from '@/api/batch';
import type { MonitoringRecordListItem } from '@/api/monitoring';
import { latestDate, toJstMonth, type TargetPair } from '@/features/master/home/masterHomeModel';

export type AnalyticsPeriod = '6' | '12' | 'all';
export type AnalysisAxis = 'project' | 'site';

export type PairResults = { pair: TargetPair; items: ReductionResultItem[] };
export type PairRecords = { pair: TargetPair; startedAt?: string; records: MonitoringRecordListItem[] };

const sum = (values: (number | null)[]) => values.reduce<number>((total, value) => total + (value ?? 0), 0);
// Float noise from summing decimals must not show up as a tiny その他 slice.
const roundEmission = (value: number) => Math.round(value * 1e6) / 1e6;

export function addMonths(month: string, offset: number) {
  const [year, monthNumber] = month.split('-').map(Number);
  const date = new Date(Date.UTC(year, monthNumber - 1 + offset, 1));
  return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}`;
}

export function monthRange(start: string, end: string) {
  const months: string[] = [];
  for (let month = start; month <= end; month = addMonths(month, 1)) months.push(month);
  return months;
}

/** Months of the period ending at `endMonth`, oldest first; 全期間 starts at the earliest data month. */
export function getPeriodMonths(period: AnalyticsPeriod, endMonth: string, dataMonths: string[]): string[] {
  const start =
    period === 'all'
      ? dataMonths.reduce((earliest, month) => (month < earliest ? month : earliest), endMonth)
      : addMonths(endMonth, -(Number(period) - 1));
  return monthRange(start, endMonth);
}

/** The same-length window right before `months`, or null for 全期間. */
export function getPreviousMonths(period: AnalyticsPeriod, months: string[]) {
  if (period === 'all' || months.length === 0) return null;
  return monthRange(addMonths(months[0], -months.length), addMonths(months[0], -1));
}

const monthOf = (item: ReductionResultItem) => toJstMonth(item.processingDate);
const itemsIn = (items: ReductionResultItem[], months: string[]) => {
  const inPeriod = new Set(months);
  return items.filter((item) => inPeriod.has(monthOf(item) ?? ''));
};

/** Latest processing month with data; emissions are reported up to there, not to today. */
export function getLatestEmissionMonth(results: PairResults[]) {
  return results
    .flatMap(({ items }) => items.map(monthOf))
    .reduce<string | null>((latest, month) => (month && (!latest || month > latest) ? month : latest), null);
}

export function totalEmission(results: PairResults[], months: string[]) {
  const items = results.flatMap(({ items: pairItems }) => itemsIn(pairItems, months));
  return items.some((item) => item.empj !== null) ? sum(items.map((item) => item.empj)) : null;
}

export function summarizePeriod(results: PairResults[], months: string[], previousMonths: string[] | null) {
  const total = totalEmission(results, months);
  const monthsWithData = new Set(
    results.flatMap(({ items }) => itemsIn(items, months).filter((item) => item.empj !== null).map(monthOf)),
  ).size;
  const previous = previousMonths ? totalEmission(results, previousMonths) : null;
  return {
    total,
    monthsWithData,
    average: total !== null && monthsWithData > 0 ? total / monthsWithData : null,
    change: total !== null && previous ? ((total - previous) / previous) * 100 : null,
  };
}

export type LineSeries = { key: string; label: string };

/** One line per site (project axis) or per project (site axis), keyed s0, s1… for CSS-safe colors. */
export function buildTrendLines(results: PairResults[], months: string[], axis: AnalysisAxis) {
  const groupOf = (pair: TargetPair) =>
    axis === 'project' ? { id: pair.siteId, label: pair.siteName } : { id: pair.projectId, label: pair.projectName };
  const groups = [...new Map(results.map(({ pair }) => [groupOf(pair).id, groupOf(pair).label])).entries()];
  const series: LineSeries[] = groups.map(([, label], index) => ({ key: `s${index}`, label }));
  const rows = months.map((period) => {
    const row: Record<string, string | number | null> = { period };
    groups.forEach(([id], index) => {
      row[`s${index}`] = totalEmission(
        results.filter(({ pair }) => groupOf(pair).id === id),
        [period],
      );
    });
    return row as { period: string } & Record<string, string | number | null>;
  });
  return { series, rows };
}

export type EmissionRow = {
  period: string;
  main: number;
  control: number;
  battery: number;
  other: number;
  total: number | null;
};

/** Stacked EmPJ per processing month; その他 is whatever empj has beyond the three components. */
export function buildEmissionRows(results: PairResults[], months: string[]): EmissionRow[] {
  return months.map((period) => {
    const items = results.flatMap(({ items: pairItems }) => itemsIn(pairItems, [period]));
    if (items.length === 0) return { period, main: 0, control: 0, battery: 0, other: 0, total: null };
    const main = sum(items.map((item) => item.empjM));
    const control = sum(items.map((item) => item.empjSControl));
    const battery = sum(items.map((item) => item.empjSBattery));
    const total = sum(items.map((item) => item.empj ?? sum([item.empjM, item.empjSControl, item.empjSBattery])));
    return { period, main, control, battery, other: roundEmission(total - main - control - battery), total };
  });
}

export type RankingRow = { id: string; label: string; total: number; share: number };

/** Ranks sites (project axis) or projects (site axis) inside the selection by period emissions. */
export function buildRanking(results: PairResults[], months: string[], axis: AnalysisAxis): RankingRow[] {
  const totals = new Map<string, RankingRow>();
  results.forEach(({ pair, items }) => {
    const id = axis === 'project' ? pair.siteId : pair.projectId;
    const label = axis === 'project' ? pair.siteName : pair.projectName;
    const current = totals.get(id) ?? { id, label, total: 0, share: 0 };
    totals.set(id, { ...current, total: current.total + sum(itemsIn(items, months).map((item) => item.empj)) });
  });
  const grandTotal = sum([...totals.values()].map((row) => row.total));
  return [...totals.values()]
    .map((row) => ({ ...row, share: grandTotal > 0 ? (row.total / grandTotal) * 100 : 0 }))
    .sort((left, right) => right.total - left.total);
}

export type CalendarStatus = 'processed' | 'submitted' | 'missing' | 'outOfScope';
export type CalendarCell = {
  month: string;
  status: CalendarStatus;
  count: number;
  lastRecordedAt: string | null;
  elPvTotal: number | null;
};
export type CalendarRow = { pair: TargetPair; cells: CalendarCell[]; submitted: number; eligible: number };

const activeRecords = (records: MonitoringRecordListItem[]) => records.filter((record) => record.isActive);

export function buildCalendarRows(pairRecords: PairRecords[], months: string[]): CalendarRow[] {
  return pairRecords.map(({ pair, startedAt, records }) => {
    const startMonth = toJstMonth(startedAt);
    const active = activeRecords(records);
    const cells = months.map((month): CalendarCell => {
      const inMonth = active.filter((record) => toJstMonth(record.dateRecorded) === month);
      const status: CalendarStatus =
        inMonth.length === 0
          ? startMonth && month < startMonth
            ? 'outOfScope'
            : 'missing'
          : inMonth.some((record) => record.processedAt)
            ? 'processed'
            : 'submitted';
      return {
        month,
        status,
        count: inMonth.length,
        lastRecordedAt: latestDate(inMonth.map((record) => record.dateRecorded)),
        elPvTotal: inMonth.some((record) => record.elPv !== null) ? sum(inMonth.map((record) => record.elPv)) : null,
      };
    });
    return {
      pair,
      cells,
      submitted: cells.filter((cell) => cell.status === 'processed' || cell.status === 'submitted').length,
      eligible: cells.filter((cell) => cell.status !== 'outOfScope').length,
    };
  });
}

/** Share of required months (not 対象外) that have a submission, in percent. */
export function getSubmissionRate(rows: CalendarRow[]) {
  const eligible = sum(rows.map((row) => row.eligible));
  return eligible > 0 ? (sum(rows.map((row) => row.submitted)) / eligible) * 100 : null;
}
