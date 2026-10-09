import { useQuery } from '@tanstack/react-query';
import { getProjectMembershipPdfs } from '@/api/admin/projectCompanies';

/** The list carries presigned URLs that live 15 minutes; refetch before they lapse. */
export const PROJECT_COMPANIES_STALE_MS = 10 * 60 * 1000;

export const projectCompaniesQueryKey = (projectId: string) =>
  ['admin', 'projects', projectId, 'companies'] as const;

/**
 * Loads GET /project/{projectId}/companies, shared by the 参加企業 tab and the
 * 入会届 section of the 書類 tab so switching between them does not refetch.
 */
export function useProjectCompaniesQuery(projectId: string) {
  return useQuery({
    queryKey: projectCompaniesQueryKey(projectId),
    queryFn: () => getProjectMembershipPdfs(projectId),
    enabled: Boolean(projectId),
    staleTime: PROJECT_COMPANIES_STALE_MS,
    retry: false,
  });
}
