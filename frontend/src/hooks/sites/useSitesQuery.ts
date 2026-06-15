import { useQuery } from '@tanstack/react-query';
import { getSites } from '@/api/site';

export function useSitesQuery() {
  return useQuery({
    queryKey: ['sites'],
    queryFn: () => getSites(),
    staleTime: 1 * 60 * 1000,
  });
}
