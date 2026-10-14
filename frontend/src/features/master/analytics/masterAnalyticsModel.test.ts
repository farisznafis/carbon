import { describe, expect, it } from 'vitest';
import type { MonitoringRecordListItem } from '@/api/monitoring';
import type { ReductionResultItem } from '@/api/batch';
import {
  buildCalendarRows,
  buildEmissionRows,
  buildRanking,
  buildTrendLines,
  getLatestEmissionMonth,
  getPeriodMonths,
  getPreviousMonths,
  getSubmissionRate,
  summarizePeriod,
} from './masterAnalyticsModel';

const pairA = { siteId: 'S1', siteName: '拠点1', projectId: 'P1', projectName: 'PJ1' };
const pairB = { siteId: 'S2', siteName: '拠点2', projectId: 'P1', projectName: 'PJ1' };

const result = (processingDate: string, empj: number, m: number, c: number, b: number): ReductionResultItem => ({
  processingDate,
  empj,
  empjM: m,
  empjSControl: c,
  empjSBattery: b,
});
const record = (dateRecorded: string, overrides: Partial<MonitoringRecordListItem> = {}) =>
  ({ dateRecorded, isActive: true, processedAt: null, elPv: 10, elPvr: 4, ...overrides }) as MonitoringRecordListItem;

describe('periods', () => {
  it('builds windows ending at the given month', () => {
    const months = getPeriodMonths('6', '2026-09', []);
    expect([months.length, months[0], months.at(-1)]).toEqual([6, '2026-04', '2026-09']);
    expect(getPreviousMonths('6', months)).toEqual(['2025-10', '2025-11', '2025-12', '2026-01', '2026-02', '2026-03']);
    expect(getPeriodMonths('all', '2026-09', ['2026-08', '2026-07'])).toEqual(['2026-07', '2026-08', '2026-09']);
    expect(getPreviousMonths('all', months)).toBeNull();
  });
});

describe('emissions', () => {
  const months = ['2026-08', '2026-09'];
  const results = [
    { pair: pairA, items: [result('2026-07-31', 20, 20, 0, 0), result('2026-08-31', 10, 6, 2, 1)] },
    { pair: pairB, items: [result('2026-08-30', 4, 2, 1, 1), result('2026-09-30', 30, 30, 0, 0)] },
  ];

  it('summarizes the period against the previous one', () => {
    expect(getLatestEmissionMonth(results)).toBe('2026-09');
    expect(summarizePeriod(results, months, ['2026-06', '2026-07'])).toEqual({
      total: 44,
      monthsWithData: 2,
      average: 22,
      change: 120,
    });
  });

  it('stacks components and puts the unexplained rest in その他', () => {
    expect(buildEmissionRows(results, months)[0]).toEqual({
      period: '2026-08',
      main: 8,
      control: 3,
      battery: 2,
      other: 1,
      total: 14,
    });
  });

  it('draws one line per site on the project axis and ranks sites', () => {
    const { series, rows } = buildTrendLines(results, months, 'project');
    expect(series).toEqual([
      { key: 's0', label: '拠点1' },
      { key: 's1', label: '拠点2' },
    ]);
    expect(rows[1]).toEqual({ period: '2026-09', s0: null, s1: 30 });
    expect(buildRanking(results, months, 'project').map((row) => [row.id, row.total, Math.round(row.share)])).toEqual([
      ['S2', 34, 77],
      ['S1', 10, 23],
    ]);
  });
});

describe('calendar', () => {
  it('classifies each month, ignores cancelled records and rates submissions', () => {
    const rows = buildCalendarRows(
      [
        {
          pair: pairA,
          startedAt: '2026-08-01',
          records: [
            record('2026-08-03', { processedAt: '2026-09-01T00:00:00Z' }),
            record('2026-09-03'),
            record('2026-10-03', { isActive: false }),
          ],
        },
      ],
      ['2026-07', '2026-08', '2026-09', '2026-10'],
    );
    expect(rows[0].cells.map((cell) => cell.status)).toEqual(['outOfScope', 'processed', 'submitted', 'missing']);
    expect([rows[0].submitted, rows[0].eligible]).toEqual([2, 3]);
    expect(Math.round(getSubmissionRate(rows) ?? 0)).toBe(67);
  });
});
