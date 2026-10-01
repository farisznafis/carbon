import type { ProjectEmissionHistoryPoint } from '@/api/batch';
import type { TimeSeriesRow } from '@/components/data-display/charts';

export const PROJECT_EMISSION_SERIES_KEY = 'projectEmission';
export const BASELINE_REFERENCE_KEY = 'baselineEmission';

/** Maps the emission history into chart rows for the project-emission line. */
export function toProjectEmissionRows(
  history: readonly ProjectEmissionHistoryPoint[],
): TimeSeriesRow[] {
  return history
    .filter((point) => point.projectEmission !== null)
    .map((point) => ({
      period: point.period,
      [PROJECT_EMISSION_SERIES_KEY]: point.projectEmission,
    }));
}

/**
 * Picks the baseline drawn as the horizontal reference line: the most recent
 * baseline in the history, falling back to the value on the project itself.
 */
export function resolveBaselineEmission(
  history: readonly ProjectEmissionHistoryPoint[],
  fallback: number | null,
): number | null {
  for (let index = history.length - 1; index >= 0; index -= 1) {
    const baseline = history[index].baselineEmission;
    if (baseline !== null) return baseline;
  }
  return fallback;
}
