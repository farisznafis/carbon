/**
 * The part of プロジェクト計画書 the system itself uses: 認証対象期間 and the
 * planned member counts per 年度.
 *
 * The plan is written and filed in the official Excel 様式. Only these values
 * are typed here, because the モニタリング報告書 screen needs them; everything
 * else of the plan stays in the workbook.
 *
 * They are stored on the plan's 4.排出削減計画 page, which already carries both
 * the period and the 年度 table.
 */

export const PLAN_SUMMARY_SLUG = 'emission-reduction-plan';

/** The 年度 range of the official 4.排出削減計画 table. */
export const FIRST_PLAN_YEAR = 2013;
export const LAST_PLAN_YEAR = 2050;

/**
 * The page still requires a unit for the emission columns this form no longer
 * asks for. Sent as-is until the backend drops those columns.
 */
const DEFAULT_UNIT_EMISSION = 't-CO2';

export type PlanYearValues = {
  newMembersTotal?: string | number | null;
  cumulativeMembersTotal?: string | number | null;
};

export type PlanSummaryFormValues = {
  /** yyyy-MM-dd, as a date input holds it. */
  certTargetPeriodStart?: string;
  certTargetPeriodEnd?: string;
  /** Keyed by {@link yearKey}, so a 年度 keeps its numbers when the period moves. */
  yearly?: Record<string, PlanYearValues>;
};

export type PlanYearRow = {
  year: number;
  newMembersTotal: number;
  cumulativeMembersTotal: number;
};

type PlanPageData = {
  unitEmission?: unknown;
  certTargetPeriodStart?: unknown;
  certTargetPeriodEnd?: unknown;
  years?: unknown;
};

/** `y2025` — never a bare number, which a form library would read as an array index. */
export const yearKey = (year: number) => `y${year}`;

const DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;

/** 年度 a date falls in: the year of the April it follows. Null for anything not yyyy-MM-dd. */
export function fiscalYearOf(date: string | undefined | null): number | null {
  const match = DATE_PATTERN.exec(date ?? '');
  if (!match) return null;
  const year = Number(match[1]);
  const month = Number(match[2]);
  return month >= 4 ? year : year - 1;
}

/** Every 年度 the period touches, within what the official table covers. */
export function fiscalYearsInPeriod(
  start: string | undefined | null,
  end: string | undefined | null,
): number[] {
  const first = fiscalYearOf(start);
  const last = fiscalYearOf(end);
  if (first === null || last === null || last < first) return [];

  const years: number[] = [];
  for (
    let year = Math.max(first, FIRST_PLAN_YEAR);
    year <= Math.min(last, LAST_PLAN_YEAR);
    year += 1
  ) {
    years.push(year);
  }
  return years;
}

const isBlank = (value: unknown) => value === undefined || value === null || value === '';

/** A count as typed: blank is 0, anything else must be a whole number of 0 or more. */
function toCount(value: unknown): number | null {
  if (isBlank(value)) return 0;
  const count = Number(value);
  return Number.isInteger(count) && count >= 0 ? count : null;
}

/** The 年度 rows the form currently describes. Blank counts read as 0. */
export function planYearRows(values: PlanSummaryFormValues): PlanYearRow[] {
  return fiscalYearsInPeriod(values.certTargetPeriodStart, values.certTargetPeriodEnd).map(
    (year) => {
      const entered = values.yearly?.[yearKey(year)];
      return {
        year,
        newMembersTotal: toCount(entered?.newMembersTotal) ?? 0,
        cumulativeMembersTotal: toCount(entered?.cumulativeMembersTotal) ?? 0,
      };
    },
  );
}

const isBlankOrZero = (value: unknown) => isBlank(value) || Number(value) === 0;

/**
 * True when there is no plan to save: no period and no count above zero.
 *
 * Zero counts as nothing, because a plan page that was never filled in reads
 * back from the server as a table of zeros.
 */
export function isPlanSummaryEmpty(values: PlanSummaryFormValues): boolean {
  if (!isBlank(values.certTargetPeriodStart) || !isBlank(values.certTargetPeriodEnd)) {
    return false;
  }
  return Object.values(values.yearly ?? {}).every(
    (year) =>
      isBlankOrZero(year?.newMembersTotal) && isBlankOrZero(year?.cumulativeMembersTotal),
  );
}

/** What stops these values being saved, in the operator's words; null when nothing does. */
export function planSummaryProblem(values: PlanSummaryFormValues): string | null {
  if (isPlanSummaryEmpty(values)) return null;

  const { certTargetPeriodStart: start, certTargetPeriodEnd: end } = values;
  if (isBlank(start) || isBlank(end)) {
    return '認証対象期間は開始日と終了日の両方を入力してください。';
  }
  if (fiscalYearOf(start) === null || fiscalYearOf(end) === null) {
    return '認証対象期間の日付の形式が正しくありません。';
  }
  if ((end as string) < (start as string)) {
    return '認証対象期間の終了日は開始日以降の日付を入力してください。';
  }

  for (const year of fiscalYearsInPeriod(start, end)) {
    const entered = values.yearly?.[yearKey(year)];
    if (
      toCount(entered?.newMembersTotal) === null ||
      toCount(entered?.cumulativeMembersTotal) === null
    ) {
      return `${year}年度の会員数は0以上の整数で入力してください。`;
    }
  }
  return null;
}

/**
 * The plan page as the backend stores it.
 *
 * Only the 年度 inside the period are sent; the server fills the rest of its
 * 2013–2050 table with zeros.
 */
export function toPlanPagePayload(
  values: PlanSummaryFormValues,
  unitEmission: string = DEFAULT_UNIT_EMISSION,
): Record<string, unknown> {
  return {
    unitEmission,
    certTargetPeriodStart: values.certTargetPeriodStart,
    certTargetPeriodEnd: values.certTargetPeriodEnd,
    years: planYearRows(values),
  };
}

const toDateValue = (value: unknown) =>
  typeof value === 'string' && DATE_PATTERN.test(value) ? value : '';

/** Form values for a stored plan page, plus the unit to send back with them. */
export function fromPlanPageData(data: unknown): {
  values: PlanSummaryFormValues;
  unitEmission: string;
} {
  const page = (data && typeof data === 'object' ? data : {}) as PlanPageData;
  const yearly: Record<string, PlanYearValues> = {};

  if (Array.isArray(page.years)) {
    for (const row of page.years as Array<Record<string, unknown>>) {
      const year = Number(row?.year);
      if (!Number.isInteger(year)) continue;
      yearly[yearKey(year)] = {
        newMembersTotal: toCount(row.newMembersTotal) ?? 0,
        cumulativeMembersTotal: toCount(row.cumulativeMembersTotal) ?? 0,
      };
    }
  }

  return {
    values: {
      certTargetPeriodStart: toDateValue(page.certTargetPeriodStart),
      certTargetPeriodEnd: toDateValue(page.certTargetPeriodEnd),
      yearly,
    },
    unitEmission:
      typeof page.unitEmission === 'string' && page.unitEmission
        ? page.unitEmission
        : DEFAULT_UNIT_EMISSION,
  };
}
