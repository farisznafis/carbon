import { useQuery } from '@tanstack/react-query';
import {
  getAdminProjectDetail,
  getProjectPlanProgress,
} from '@/api/admin/projects';

export const adminProjectDetailQueryKey = (projectId: string) =>
  ['admin', 'projects', projectId] as const;

export const adminProjectPlanProgressQueryKey = (projectId: string) =>
  ['admin', 'projects', projectId, 'plan-progress'] as const;

/** Loads GET /project/{projectId} and shares the result between admin project pages. */
export function useAdminProjectDetailQuery(projectId?: string) {
  return useQuery({
    queryKey: adminProjectDetailQueryKey(projectId ?? ''),
    queryFn: () => getAdminProjectDetail(projectId as string),
    enabled: Boolean(projectId),
    staleTime: 30_000,
  });
}

/** Loads GET /projectplan/{projectId}/progress independently from project detail. */
export function useAdminProjectPlanProgressQuery(projectId?: string) {
  return useQuery({
    queryKey: adminProjectPlanProgressQueryKey(projectId ?? ''),
    queryFn: () => getProjectPlanProgress(projectId as string),
    enabled: Boolean(projectId),
    staleTime: 30_000,
    retry: false,
  });
}
