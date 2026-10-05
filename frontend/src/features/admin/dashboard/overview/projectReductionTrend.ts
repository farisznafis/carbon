import type { ProjectEmissionHistoryPoint } from '@/api/batch';
import type { Project } from '@/api/projects';
import type {
  TimeSeriesLineSeries,
  TimeSeriesRow,
} from '@/components/data-display/charts';

export const REDUCTION_TREND_PROJECT_LIMIT = 5;

const SERIES_COLORS = [
  'hsl(var(--chart-1))',
  'hsl(var(--chart-2))',
  'hsl(var(--chart-3))',
  'hsl(var(--chart-4))',
  'hsl(var(--chart-5))',
] as const;

/** Picks the projects with the largest reported reduction for the trend chart. */
export function selectReductionTrendProjects(
  projects: readonly Project[],
  limit = REDUCTION_TREND_PROJECT_LIMIT,
): Project[] {
  return projects
    .filter((project) => project.emissionReduction !== undefined)
    .sort(
      (left, right) =>
        (right.emissionReduction ?? 0) - (left.emissionReduction ?? 0),
    )
    .slice(0, limit);
}

export type ReductionTrendChartData = {
  series: TimeSeriesLineSeries[];
  rows: TimeSeriesRow[];
};

/**
 * Merges each project's per-period reductions into chart rows keyed by
 * period. Projects without any reduction value are left out of the legend.
 */
export function buildReductionTrendChartData(
  projects: readonly Project[],
  historyByProjectId: ReadonlyMap<string, readonly ProjectEmissionHistoryPoint[]>,
): ReductionTrendChartData {
  const rowsByPeriod = new Map<string, TimeSeriesRow>();
  const series: TimeSeriesLineSeries[] = [];

  projects.forEach((project) => {
    const history = (historyByProjectId.get(String(project.id)) ?? []).filter(
      (point) => point.reduction !== null,
    );
    if (history.length === 0) return;

    // Keys must be CSS-identifier safe because they become color variables.
    const key = `project${series.length}`;
    series.push({
      key,
      label: project.projectName,
      color: SERIES_COLORS[series.length % SERIES_COLORS.length],
    });

    history.forEach((point) => {
      const row = rowsByPeriod.get(point.period) ?? { period: point.period };
      row[key] = point.reduction;
      rowsByPeriod.set(point.period, row);
    });
  });

  const rows = [...rowsByPeriod.values()].sort((left, right) =>
    left.period.localeCompare(right.period),
  );

  return { series, rows };
}
