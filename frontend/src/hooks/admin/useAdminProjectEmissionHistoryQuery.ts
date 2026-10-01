import { useMemo } from 'react';
import {
  useQueries,
  useQuery,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  getProjectEmissionHistory,
  type ProjectEmissionHistoryPoint,
} from '@/api/batch';

export const adminProjectEmissionHistoryQueryKey = (projectId: string) =>
  ['admin', 'projects', projectId, 'emission-history'] as const;

const EMISSION_HISTORY_STALE_TIME = 60_000;

/** Loads one project's per-period emission history for the admin detail page. */
export function useAdminProjectEmissionHistoryQuery(projectId?: string) {
  return useQuery({
    queryKey: adminProjectEmissionHistoryQueryKey(projectId ?? ''),
    queryFn: () => getProjectEmissionHistory(projectId as string),
    enabled: Boolean(projectId),
    staleTime: EMISSION_HISTORY_STALE_TIME,
    retry: false,
  });
}

export type ProjectEmissionHistoryResult = {
  historyByProjectId: ReadonlyMap<string, ProjectEmissionHistoryPoint[]>;
  isLoading: boolean;
  isError: boolean;
};

/**
 * Loads emission history for several projects in parallel. Each project shares
 * its cache entry with the detail page, so opening a project after the
 * dashboard does not refetch.
 */
export function useAdminProjectsEmissionHistoryQueries(
  projectIds: readonly string[],
): ProjectEmissionHistoryResult {
  const { histories, isLoading, isError } = useQueries({
    queries: projectIds.map((projectId) => ({
      queryKey: adminProjectEmissionHistoryQueryKey(projectId),
      queryFn: () => getProjectEmissionHistory(projectId),
      staleTime: EMISSION_HISTORY_STALE_TIME,
      retry: false,
    })),
    combine: combineEmissionHistoryResults,
  });

  const historyByProjectId = useMemo(
    () =>
      new Map(
        projectIds.map((projectId, index) => [
          projectId,
          histories[index] ?? [],
        ]),
      ),
    [histories, projectIds],
  );

  return { historyByProjectId, isLoading, isError };
}

/** Module-level so react-query can keep the combined result referentially stable. */
function combineEmissionHistoryResults(
  results: UseQueryResult<ProjectEmissionHistoryPoint[]>[],
) {
  return {
    histories: results.map((result) => result.data),
    isLoading: results.some((result) => result.isLoading),
    isError: results.length > 0 && results.every((result) => result.isError),
  };
}
