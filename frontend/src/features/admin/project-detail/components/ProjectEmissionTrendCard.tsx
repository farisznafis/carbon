import { useMemo } from 'react';
import type { AdminProjectDetail } from '@/api/admin/projects';
import {
  ChartCard,
  TimeSeriesLineChart,
  type TimeSeriesReferenceLine,
} from '@/components/data-display/charts';
import { useAdminProjectEmissionHistoryQuery } from '@/hooks/admin/useAdminProjectEmissionHistoryQuery';
import {
  formatProjectEmission,
  toProjectDetailNumber,
} from '../projectDetailDisplay';
import {
  BASELINE_REFERENCE_KEY,
  PROJECT_EMISSION_SERIES_KEY,
  resolveBaselineEmission,
  toProjectEmissionRows,
} from './projectEmissionTrend';

const PROJECT_EMISSION_SERIES = [
  {
    key: PROJECT_EMISSION_SERIES_KEY,
    label: 'プロジェクト実施後排出量',
    color: 'hsl(var(--primary))',
  },
] as const;

/** Plots project emissions per period against the baseline as a horizontal line. */
export function ProjectEmissionTrendCard({
  projectId,
  project,
  className,
}: Readonly<{
  projectId: string;
  project: AdminProjectDetail;
  className?: string;
}>) {
  const historyQuery = useAdminProjectEmissionHistoryQuery(projectId);
  const history = useMemo(() => historyQuery.data ?? [], [historyQuery.data]);
  const rows = useMemo(() => toProjectEmissionRows(history), [history]);
  const baselineEmission = resolveBaselineEmission(
    history,
    toProjectDetailNumber(project.baselineEmissions ?? project.baselineEmission),
  );
  const reduction = toProjectDetailNumber(project.emissionReduction);
  const referenceLines: TimeSeriesReferenceLine[] =
    baselineEmission === null
      ? []
      : [
          {
            key: BASELINE_REFERENCE_KEY,
            label: `ベースライン排出量（${formatProjectEmission(baselineEmission)}）`,
            value: baselineEmission,
            color: 'hsl(var(--muted-foreground))',
          },
        ];

  return (
    <ChartCard
      className={className}
      title="排出量の推移"
      description="算定期間ごとのプロジェクト実施後排出量です。破線はベースライン排出量を示し、線との差が削減量になります。"
      headline={
        <p className="text-small font-bold text-primary-strong">
          削減量 {formatProjectEmission(reduction)}
        </p>
      }
      isLoading={historyQuery.isLoading}
      error={historyQuery.isError ? '排出量データを取得できませんでした。' : null}
      isEmpty={rows.length === 0}
      emptyMessage="排出量データはまだありません。"
    >
      <TimeSeriesLineChart
        ariaLabel="プロジェクト実施後排出量とベースライン排出量の推移"
        data={rows}
        series={PROJECT_EMISSION_SERIES}
        referenceLines={referenceLines}
      />
    </ChartCard>
  );
}
