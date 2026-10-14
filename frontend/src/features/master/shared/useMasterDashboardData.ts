import { useMemo } from 'react';
import { useQueries, type UseQueryResult } from '@tanstack/react-query';
import { getSiteProjects } from '@/api/site';
import { getMonitoringRecent } from '@/api/monitoring';
import { getReductionResults } from '@/api/batch';
import { useSitesQuery } from '@/hooks/sites/useSitesQuery';
import { useUsersQuery } from '@/hooks/users/useUsersQuery';
import { getTargetPairs, latestDate, type TargetPair } from '@/features/master/home/masterHomeModel';

export const MASTER_STALE_MS = 5 * 60 * 1000;

/** Dashboard data changes at most monthly; refetching on every window focus only adds requests. */
export const MASTER_QUERY_OPTIONS = { staleTime: MASTER_STALE_MS, refetchOnWindowFocus: false } as const;

// One key per source, shared by ホーム and アナリティクス so switching tabs reuses the cache.
export const masterQueryKeys = {
  siteProjects: (siteId: string) => ['site-projects', siteId] as const,
  latestMonitoring: (siteId: string, projectId: string) =>
    ['monitoring', 'recent', siteId, projectId] as const,
  reductionResults: (siteId: string, projectId: string) =>
    ['batch', 'reduction-result', siteId, projectId] as const,
};

// Module-level so useQueries only re-runs it when the results change.
export const combineQueries = <T,>(results: UseQueryResult<T>[]) => ({
  data: results.map((result) => result.data),
  isPending: results.some((result) => result.isPending),
  isError: results.some((result) => result.isError),
});

/** Sites, users and per-site projects, plus the APPROVED × hasSiteRelation pairs derived from them. */
export function useMasterBaseData() {
  const sitesQuery = useSitesQuery();
  const usersQuery = useUsersQuery();
  const sites = useMemo(() => sitesQuery.data?.sites ?? [], [sitesQuery.data]);
  const users = useMemo(() => usersQuery.data?.users ?? [], [usersQuery.data]);

  const projects = useQueries({
    queries: sites.map((site) => ({
      queryKey: masterQueryKeys.siteProjects(site.siteId),
      queryFn: () => getSiteProjects(site.siteId),
      ...MASTER_QUERY_OPTIONS,
    })),
    combine: combineQueries,
  });
  const siteProjects = useMemo(
    () => sites.map((site, index) => ({ siteId: site.siteId, projects: projects.data[index] ?? [] })),
    [sites, projects.data],
  );
  const targets = useMemo(() => getTargetPairs(siteProjects, sites), [siteProjects, sites]);

  return {
    sites,
    users,
    siteProjects,
    targets,
    isPending: sitesQuery.isPending || usersQuery.isPending || projects.isPending,
    error: sitesQuery.error,
  };
}

/** Reduction results for each pair, in the same order as `pairs`. */
export function useReductionResults(pairs: TargetPair[]) {
  return useQueries({
    queries: pairs.map((pair) => ({
      queryKey: masterQueryKeys.reductionResults(pair.siteId, pair.projectId),
      queryFn: () => getReductionResults(pair.siteId, pair.projectId),
      ...MASTER_QUERY_OPTIONS,
    })),
    combine: combineQueries,
  });
}

/** Latest dateRecorded per pair from GET /monitoring, in the same order as `pairs`. */
export function useLatestMonitoring(pairs: TargetPair[]) {
  return useQueries({
    queries: pairs.map((pair) => ({
      queryKey: masterQueryKeys.latestMonitoring(pair.siteId, pair.projectId),
      queryFn: async () =>
        latestDate(
          (await getMonitoringRecent({ projectId: pair.projectId, siteId: pair.siteId })).map(
            (item) => item.dateRecorded,
          ),
        ),
      ...MASTER_QUERY_OPTIONS,
    })),
    combine: combineQueries,
  });
}
