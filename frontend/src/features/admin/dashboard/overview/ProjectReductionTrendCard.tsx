import { useMemo } from 'react';
import type { Project } from '@/api/projects';
import {
  ChartCard,
  TimeSeriesLineChart,
} from '@/components/data-display/charts';
import { useAdminProjectsEmissionHistoryQueries } from '@/hooks/admin/useAdminProjectEmissionHistoryQuery';
import {
  buildReductionTrendChartData,
  REDUCTION_TREND_PROJECT_LIMIT,
  selectReductionTrendProjects,
} from './projectReductionTrend';

/** Plots per-period reductions for the projects with the largest reductions. */
export function ProjectReductionTrendCard({
  projects,
}: Readonly<{ projects: readonly Project[] }>) {
  const trendProjects = useMemo(
    () => selectReductionTrendProjects(projects),
    [projects],
  );
  const projectIds = useMemo(
    () => trendProjects.map((project) => String(project.id)),
    [trendProjects],
  );
  const { historyByProjectId, isLoading, isError } =
    useAdminProjectsEmissionHistoryQueries(projectIds);
  const chartData = useMemo(
    () => buildReductionTrendChartData(trendProjects, historyByProjectId),
    [historyByProjectId, trendProjects],
  );

  return (
    <ChartCard
      title="プロジェクト別削減量"
      description={`削減量上位${REDUCTION_TREND_PROJECT_LIMIT}件のプロジェクトについて、年ごとの削減量（ベースライン − プロジェクト実施後排出量）を表示します。`}
      isLoading={isLoading}
      error={isError ? '削減量データを取得できませんでした。' : null}
      isEmpty={chartData.series.length === 0}
      emptyMessage="削減量データはまだありません。"
    >
      <TimeSeriesLineChart
        ariaLabel="プロジェクト別削減量の推移"
        yAxisLabel="削減量（t-CO₂）"
        data={chartData.rows}
        series={chartData.series}
      />
    </ChartCard>
  );
}
