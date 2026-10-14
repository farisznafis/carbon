import { useMemo } from 'react';
import { useQueries } from '@tanstack/react-query';
import { getProjectEmissionSummary } from '@/api/batch';
import { getMonitoringRecords, type MonitoringRecordListItem } from '@/api/monitoring';
import { latestDate, toJstMonth, type TargetPair } from '@/features/master/home/masterHomeModel';
import {
  combineQueries,
  MASTER_QUERY_OPTIONS,
  useLatestMonitoring,
  useMasterBaseData,
  useReductionResults,
} from '@/features/master/shared/useMasterDashboardData';
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
  totalEmission,
  type AnalysisAxis,
  type AnalyticsPeriod,
} from './masterAnalyticsModel';

const RECORDS_PAGE_LIMIT = 50;
// shortcut: 50 × 40 = 2,000 records per pair (~160 years of monthly data); page by period if that ever binds.
const MAX_RECORD_PAGES = 40;

/**
 * Every record of one pair, all pages. Fetched once and filtered by period in the
 * browser, so switching 期間 never refetches.
 */
async function getAllRecords(pair: TargetPair) {
  const records: MonitoringRecordListItem[] = [];
  for (let page = 1; page <= MAX_RECORD_PAGES; page += 1) {
    const { items, pagination } = await getMonitoringRecords({
      siteId: pair.siteId,
      projectId: pair.projectId,
      page,
      limit: RECORDS_PAGE_LIMIT,
    });
    records.push(...items);
    if (!pagination.hasMore) break;
  }
  return records;
}

export type SelectorCard = {
  id: string;
  name: string;
  total: number | null;
  /** project axis: participating site names; site axis: project names. */
  members: string[];
  /** project axis only: embl − empj of the whole project, from reduction-result-total. */
  projectReduction?: number | null;
  /** site axis only. */
  lastSubmittedAt?: string | null;
};

export function useMasterAnalyticsData({
  axis,
  period,
  selectedId,
}: {
  axis: AnalysisAxis;
  period: AnalyticsPeriod;
  selectedId: string | null;
}) {
  const base = useMasterBaseData();
  const { siteProjects, targets } = base;
  const currentMonth = toJstMonth(new Date()) ?? '';

  // Shared with ホーム, so these are usually already cached.
  const reductions = useReductionResults(targets);
  const latestMonitoring = useLatestMonitoring(targets);
  const projectIds = useMemo(() => [...new Set(targets.map((pair) => pair.projectId))], [targets]);
  const projectTotals = useQueries({
    queries: projectIds.map((projectId) => ({
      queryKey: ['batch', 'reduction-result-total', projectId] as const,
      queryFn: () => getProjectEmissionSummary(projectId),
      ...MASTER_QUERY_OPTIONS,
    })),
    combine: combineQueries,
  });

  const selection = useMemo(() => {
    const allResults = targets.map((pair, index) => ({ pair, items: reductions.data[index] ?? [] }));
    const emissionDataMonths = allResults
      .flatMap(({ items }) => items.map((item) => toJstMonth(item.processingDate)))
      .filter((month): month is string => month !== null);
    const emissionMonths = getPeriodMonths(
      period,
      getLatestEmissionMonth(allResults) ?? currentMonth,
      emissionDataMonths,
    );

    const keyOf = (pair: TargetPair) => (axis === 'project' ? pair.projectId : pair.siteId);
    const groups = new Map<string, { name: string; indexes: number[] }>();
    targets.forEach((pair, index) => {
      const id = keyOf(pair);
      const group = groups.get(id) ?? { name: axis === 'project' ? pair.projectName : pair.siteName, indexes: [] };
      group.indexes.push(index);
      groups.set(id, group);
    });

    const cards: SelectorCard[] = [...groups.entries()]
      .map(([id, group]) => {
        const pairs = group.indexes.map((index) => targets[index]);
        const summary = projectTotals.data[projectIds.indexOf(id)];
        return {
          id,
          name: group.name,
          total: totalEmission(group.indexes.map((index) => allResults[index]), emissionMonths),
          members: pairs.map((pair) => (axis === 'project' ? pair.siteName : pair.projectName)),
          ...(axis === 'project'
            ? {
                projectReduction:
                  summary?.baselineEmission != null
                    ? summary.baselineEmission - (summary.projectEmission ?? 0)
                    : null,
              }
            : {
                lastSubmittedAt: latestDate(
                  group.indexes.flatMap((index) => latestMonitoring.data[index] ?? []),
                ),
              }),
        };
      })
      .sort((left, right) => (right.total ?? -1) - (left.total ?? -1));

    const activeId = cards.some((card) => card.id === selectedId) ? selectedId : (cards[0]?.id ?? null);
    const results = allResults.filter(({ pair }) => keyOf(pair) === activeId);

    return {
      cards,
      activeCard: cards.find((card) => card.id === activeId) ?? null,
      selectedPairs: results.map(({ pair }) => pair),
      emissionMonths,
      summary: summarizePeriod(results, emissionMonths, getPreviousMonths(period, emissionMonths)),
      trend: buildTrendLines(results, emissionMonths, axis),
      emissionRows: buildEmissionRows(results, emissionMonths),
      ranking: buildRanking(results, emissionMonths, axis),
    };
  }, [targets, reductions.data, latestMonitoring.data, projectTotals.data, projectIds, period, axis, selectedId, currentMonth]);

  // Only the selected card's pairs: the calendar and 提出率 never show the others.
  const records = useQueries({
    queries: selection.selectedPairs.map((pair) => ({
      queryKey: ['monitoring', 'records-all', pair.siteId, pair.projectId] as const,
      queryFn: () => getAllRecords(pair),
      ...MASTER_QUERY_OPTIONS,
    })),
    combine: combineQueries,
  });

  const calendar = useMemo(() => {
    const pairRecords = selection.selectedPairs.map((pair, index) => ({
      pair,
      startedAt: siteProjects
        .find((entry) => entry.siteId === pair.siteId)
        ?.projects.find((project) => project.id === pair.projectId)?.startedAt,
      records: records.data[index] ?? [],
    }));
    const recordMonths = pairRecords
      .flatMap((entry) => entry.records.map((record) => toJstMonth(record.dateRecorded)))
      .filter((month): month is string => month !== null);
    const monitoringMonths = getPeriodMonths(period, currentMonth, recordMonths);
    const calendarRows = buildCalendarRows(pairRecords, monitoringMonths);
    return { monitoringMonths, calendarRows, submissionRate: getSubmissionRate(calendarRows) };
  }, [selection.selectedPairs, records.data, siteProjects, period, currentMonth]);

  return {
    ...selection,
    ...calendar,
    hasTargets: targets.length > 0,
    isBaseLoading: base.isPending,
    isEmissionLoading: base.isPending || reductions.isPending,
    isRecordsLoading: base.isPending || reductions.isPending || records.isPending,
    emissionError: reductions.isError,
    recordsError: records.isError,
    error: base.error,
  };
}
