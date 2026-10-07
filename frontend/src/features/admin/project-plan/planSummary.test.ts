import { describe, expect, it } from 'vitest';
import {
  fiscalYearOf,
  fiscalYearsInPeriod,
  fromPlanPageData,
  isPlanSummaryEmpty,
  planSummaryProblem,
  toPlanPagePayload,
  yearKey,
} from './planSummary';

describe('fiscalYearOf', () => {
  it('counts January to March as the previous 年度', () => {
    expect(fiscalYearOf('2025-04-01')).toBe(2025);
    expect(fiscalYearOf('2026-03-31')).toBe(2025);
    expect(fiscalYearOf('2026-01-15')).toBe(2025);
  });

  it('is null for anything that is not a full date', () => {
    expect(fiscalYearOf('')).toBeNull();
    expect(fiscalYearOf(undefined)).toBeNull();
    expect(fiscalYearOf('2025/04/01')).toBeNull();
  });
});

describe('fiscalYearsInPeriod', () => {
  it('lists every 年度 the period touches', () => {
    // The official solar example: 2025/10/1 – 2050/10/1.
    const years = fiscalYearsInPeriod('2025-10-01', '2050-10-01');

    expect(years[0]).toBe(2025);
    expect(years[years.length - 1]).toBe(2050);
    expect(years).toHaveLength(26);
  });

  it('is empty until both dates are in and in order', () => {
    expect(fiscalYearsInPeriod('2025-10-01', '')).toEqual([]);
    expect(fiscalYearsInPeriod('', '2030-03-31')).toEqual([]);
    expect(fiscalYearsInPeriod('2030-04-01', '2025-04-01')).toEqual([]);
  });

  it('stays inside the 年度 the official table has', () => {
    expect(fiscalYearsInPeriod('2010-04-01', '2014-04-01')).toEqual([2013, 2014]);
    expect(fiscalYearsInPeriod('2049-04-01', '2060-03-31')).toEqual([2049, 2050]);
  });
});

describe('isPlanSummaryEmpty', () => {
  it('treats a never-filled plan, which reads back as zeros, as nothing to save', () => {
    const { values } = fromPlanPageData({
      unitEmission: null,
      years: [
        { year: 2013, newMembersTotal: 0, cumulativeMembersTotal: 0 },
        { year: 2014, newMembersTotal: 0, cumulativeMembersTotal: 0 },
      ],
    });

    expect(isPlanSummaryEmpty(values)).toBe(true);
    expect(planSummaryProblem(values)).toBeNull();
  });

  it('is not empty once a date or a count is entered', () => {
    expect(isPlanSummaryEmpty({ certTargetPeriodStart: '2025-10-01' })).toBe(false);
    expect(isPlanSummaryEmpty({ yearly: { [yearKey(2025)]: { newMembersTotal: '5' } } })).toBe(
      false,
    );
  });
});

describe('planSummaryProblem', () => {
  it('asks for both dates', () => {
    expect(planSummaryProblem({ certTargetPeriodStart: '2025-10-01' })).toContain('両方');
  });

  it('refuses an end before the start', () => {
    expect(
      planSummaryProblem({
        certTargetPeriodStart: '2030-04-01',
        certTargetPeriodEnd: '2025-04-01',
      }),
    ).toContain('終了日');
  });

  it('names the 年度 whose count is not a whole number of 0 or more', () => {
    const base = { certTargetPeriodStart: '2025-04-01', certTargetPeriodEnd: '2027-03-31' };

    expect(
      planSummaryProblem({ ...base, yearly: { [yearKey(2026)]: { newMembersTotal: '-1' } } }),
    ).toContain('2026年度');
    expect(
      planSummaryProblem({
        ...base,
        yearly: { [yearKey(2025)]: { cumulativeMembersTotal: '1.5' } },
      }),
    ).toContain('2025年度');
  });

  it('accepts a period with blank counts, which are sent as 0', () => {
    expect(
      planSummaryProblem({
        certTargetPeriodStart: '2025-04-01',
        certTargetPeriodEnd: '2027-03-31',
      }),
    ).toBeNull();
  });
});

describe('toPlanPagePayload', () => {
  it('sends the period and only the 年度 inside it', () => {
    const payload = toPlanPagePayload({
      certTargetPeriodStart: '2025-10-01',
      certTargetPeriodEnd: '2027-03-31',
      yearly: {
        [yearKey(2025)]: { newMembersTotal: '500', cumulativeMembersTotal: '500' },
        [yearKey(2026)]: { newMembersTotal: '450', cumulativeMembersTotal: '' },
        // Entered before the period was shortened; no longer part of the plan.
        [yearKey(2040)]: { newMembersTotal: '9', cumulativeMembersTotal: '9' },
      },
    });

    expect(payload).toEqual({
      unitEmission: 't-CO2',
      certTargetPeriodStart: '2025-10-01',
      certTargetPeriodEnd: '2027-03-31',
      years: [
        { year: 2025, newMembersTotal: 500, cumulativeMembersTotal: 500 },
        { year: 2026, newMembersTotal: 450, cumulativeMembersTotal: 0 },
      ],
    });
  });

  it('sends back the unit the stored page already had', () => {
    const loaded = fromPlanPageData({
      unitEmission: 'kg-CO2',
      certTargetPeriodStart: '2025-10-01',
      certTargetPeriodEnd: '2026-03-31',
      years: [{ year: 2025, newMembersTotal: 3, cumulativeMembersTotal: 3 }],
    });

    expect(toPlanPagePayload(loaded.values, loaded.unitEmission)).toMatchObject({
      unitEmission: 'kg-CO2',
      years: [{ year: 2025, newMembersTotal: 3, cumulativeMembersTotal: 3 }],
    });
  });
});

describe('fromPlanPageData', () => {
  it('survives a page that has never been stored', () => {
    expect(fromPlanPageData(undefined)).toEqual({
      values: { certTargetPeriodStart: '', certTargetPeriodEnd: '', yearly: {} },
      unitEmission: 't-CO2',
    });
  });
});
