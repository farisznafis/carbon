import { useQuery } from '@tanstack/react-query';
import {
  getAdminOperatorProfile,
  getAdminOperators,
} from '@/api/admin/operators';

export const adminOperatorsQueryKey = ['admin', 'operators'] as const;

/** Loads GET /admin/companies/operator-user with a reusable React Query cache. */
export function useAdminOperatorsQuery(enabled = true) {
  return useQuery({
    queryKey: adminOperatorsQueryKey,
    queryFn: getAdminOperators,
    staleTime: 60_000,
    enabled,
  });
}

/** Loads GET /admin/companies/operator-user/{userId} for direct profile routes. */
export function useAdminOperatorProfileQuery(userId?: string) {
  return useQuery({
    queryKey: [...adminOperatorsQueryKey, userId],
    queryFn: () => getAdminOperatorProfile(userId ?? ''),
    enabled: Boolean(userId),
  });
}
