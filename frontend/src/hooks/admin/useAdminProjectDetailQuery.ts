import { useQuery } from '@tanstack/react-query';
import { getAdminProjectDetail } from '@/api/admin/projects';

export const adminProjectDetailQueryKey = (projectId: string) =>
  ['admin', 'projects', projectId] as const;

/** Loads GET /project/{projectId} and shares the result between admin project pages. */
export function useAdminProjectDetailQuery(projectId?: string) {
  return useQuery({
    queryKey: adminProjectDetailQueryKey(projectId ?? ''),
    queryFn: () => getAdminProjectDetail(projectId as string),
    enabled: Boolean(projectId),
    staleTime: 30_000,
  });
}
