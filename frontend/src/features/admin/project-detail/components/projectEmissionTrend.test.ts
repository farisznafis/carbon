import { describe, expect, it } from 'vitest';
import type { ProjectEmissionHistoryPoint } from '@/api/batch';
import {
  resolveBaselineEmission,
  toProjectEmissionRows,
} from './projectEmissionTrend';

const point = (
  period: string,
  baselineEmission: number | null,
  projectEmission: number | null,
): ProjectEmissionHistoryPoint => ({
  period,
  baselineEmission,
  projectEmission,
  reduction: null,
});

describe('toProjectEmissionRows', () => {
  it('keeps only periods with a project emission value', () => {
    expect(
      toProjectEmissionRows([
        point('2026-01-31', 10, 8),
        point('2026-02-28', 10, null),
      ]),
    ).toEqual([{ period: '2026-01-31', projectEmission: 8 }]);
  });
});

describe('resolveBaselineEmission', () => {
  it('prefers the latest baseline in the history', () => {
    expect(
      resolveBaselineEmission(
        [point('2026-01-31', 10, 8), point('2026-02-28', 12, 9), point('2026-03-31', null, 7)],
        5,
      ),
    ).toBe(12);
  });

  it('falls back to the project baseline when the history has none', () => {
    expect(resolveBaselineEmission([point('2026-01-31', null, 8)], 5)).toBe(5);
    expect(resolveBaselineEmission([], null)).toBeNull();
  });
});
