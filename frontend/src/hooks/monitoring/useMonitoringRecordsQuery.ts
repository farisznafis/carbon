import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { getMonitoringRecords } from '@/api/monitoring';

/**
 * Records only change when someone registers or a 取消 is approved, so a short
 * window spares refetching every time the tab is reopened. Own registrations
 * invalidate it right away (see MonitoringRegisterPage).
 */
const MONITORING_RECORDS_STALE_MS = 2 * 60 * 1000;

/** Prefix for one site's records in one project, across every page. */
export const monitoringRecordsQueryKey = (siteId: string, projectId: string) =>
  ['monitoring', 'records', String(siteId), String(projectId)] as const;

const cancelRequestedQueryKey = (siteId: string, projectId: string) =>
  ['monitoring', 'cancel-requested', String(siteId), String(projectId)] as const;

/** Loads GET /monitoring/{siteId}/{projectId}/list one page at a time. */
export function useMonitoringRecordsQuery(
  siteId: string,
  projectId: string,
  page: number,
  limit: number,
) {
  return useQuery({
    queryKey: [...monitoringRecordsQueryKey(siteId, projectId), { page, limit }],
    queryFn: () => getMonitoringRecords({ siteId, projectId, page, limit }),
    enabled: Boolean(siteId && projectId),
    staleTime: MONITORING_RECORDS_STALE_MS,
    refetchOnWindowFocus: false,
    // Keeps the current page on screen while the next one loads.
    placeholderData: keepPreviousData,
  });
}

/**
 * Records this browser sent a 取消申請 for. Users cannot read request status
 * from the API yet, so this is kept apart from the records (a refetch would
 * otherwise drop it) and lasts until the page is reloaded.
 */
export function useCancelRequestedRecords(siteId: string, projectId: string) {
  const queryClient = useQueryClient();
  const queryKey = cancelRequestedQueryKey(siteId, projectId);
  const { data: requestedKeys = [] } = useQuery({
    queryKey,
    queryFn: () => [] as string[],
    initialData: [] as string[],
    staleTime: Infinity,
    gcTime: Infinity,
  });

  const markRequested = useCallback(
    (recordKey: string) =>
      queryClient.setQueryData<string[]>(
        cancelRequestedQueryKey(siteId, projectId),
        (current = []) => (current.includes(recordKey) ? current : [...current, recordKey]),
      ),
    [projectId, queryClient, siteId],
  );

  return { requestedKeys, markRequested };
}
