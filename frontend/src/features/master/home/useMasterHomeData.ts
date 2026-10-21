import { useMemo } from 'react';
import {
  useLatestMonitoring,
  useMasterBaseData,
  useReductionResults,
} from '@/features/master/shared/useMasterDashboardData';
import { buildMasterHome, pairKey, toMonthlyEmissions } from './masterHomeModel';

/** Loads every source the master ホーム needs and derives its widgets. */
export function useMasterHomeData() {
  const base = useMasterBaseData();
  const { sites, users, siteProjects, targets } = base;
  const monitoring = useLatestMonitoring(targets);
  const reductions = useReductionResults(targets);

  const home = useMemo(() => {
    const latestSubmissions = new Map(
      targets.map((pair, index) => [pairKey(pair.siteId, pair.projectId), monitoring.data[index] ?? null]),
    );
    return buildMasterHome({ sites, users, siteProjects, latestSubmissions });
  }, [sites, users, siteProjects, targets, monitoring.data]);

  const monthlyEmissions = useMemo(
    () => toMonthlyEmissions(targets.map((pair, index) => ({ pair, items: reductions.data[index] ?? [] }))),
    [targets, reductions.data],
  );

  return {
    ...home,
    monthlyEmissions,
    isBaseLoading: base.isPending,
    isMonitoringLoading: base.isPending || monitoring.isPending,
    isEmissionLoading: base.isPending || reductions.isPending,
    emissionError: reductions.isError,
    error: base.error,
  };
}
