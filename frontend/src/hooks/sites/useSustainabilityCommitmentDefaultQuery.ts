import { useQuery } from '@tanstack/react-query';
import { getSiteProjectSustainabilityCommitmentDefault } from '@/api/site';

/** Fetches and caches GET /site/project/sites/{siteId}/sustainability-commitment/default per site. */
export function useSustainabilityCommitmentDefaultQuery(
  siteId: string,
  enabled: boolean,
) {
  return useQuery({
    queryKey: ['site-project', siteId, 'sustainability-commitment-default'],
    queryFn: () => getSiteProjectSustainabilityCommitmentDefault(siteId),
    enabled: enabled && Boolean(siteId),
    staleTime: 5 * 60 * 1000,
    retry: 1,
  });
}
