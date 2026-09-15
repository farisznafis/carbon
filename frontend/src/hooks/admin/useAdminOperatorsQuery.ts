import { useQuery } from '@tanstack/react-query';
import {
  getAdminOperatorProfile,
  getAdminOperators,
} from '@/api/admin/operators';
import type { ForbiddenHandling } from '@/api/_shared/forbiddenAccess';

export const adminOperatorsQueryKey = ['admin', 'operators'] as const;

/** Loads GET /admin/companies/operator-user with a reusable React Query cache. */
export function useAdminOperatorsQuery(
  enabled = true,
  forbiddenHandling: ForbiddenHandling = 'page',
) {
  return useQuery({
    queryKey: [...adminOperatorsQueryKey, forbiddenHandling],
    queryFn: () => getAdminOperators({ forbiddenHandling }),
    staleTime: 60_000,
    enabled,
  });
}

/** Loads GET /admin/companies/operator-user/{userId} for direct profile routes. */
export function useAdminOperatorProfileQuery(
  userId?: string,
  enabled = true,
  forbiddenHandling: ForbiddenHandling = 'page',
) {
  return useQuery({
    queryKey: [...adminOperatorsQueryKey, 'profile', userId, forbiddenHandling],
    queryFn: () =>
      getAdminOperatorProfile(userId ?? '', { forbiddenHandling }),
    enabled: enabled && Boolean(userId),
  });
}
