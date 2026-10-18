import { useCallback } from 'react';
import type { User } from '@/api/users';
import { useAdminOperatorProfileQuery } from './useAdminOperatorsQuery';

/** Rejects the legacy admin-login placeholder before building a detail endpoint. */
function getSessionOperatorId(userId: string): string | null {
  const normalizedId = userId.trim();
  return normalizedId && normalizedId.toLocaleLowerCase('en-US') !== 'admin'
    ? normalizedId
    : null;
}

/** Loads the signed-in admin/operator profile directly from its session ID. */
export function useCurrentAdminOperatorProfileQuery(
  user: User | null,
  enabled = true,
) {
  const isAdminPortalSession = user?.role === 'admin';
  const sessionOperatorId = user ? getSessionOperatorId(user.id) : null;
  const profileQuery = useAdminOperatorProfileQuery(
    sessionOperatorId ?? undefined,
    enabled && isAdminPortalSession && Boolean(sessionOperatorId),
    'caller',
  );

  /** Refreshes the signed-in operator profile without requesting the operator list. */
  const refetch = useCallback(async () => {
    if (!user || !isAdminPortalSession || !sessionOperatorId) return null;
    return (await profileQuery.refetch()).data ?? null;
  }, [isAdminPortalSession, profileQuery, sessionOperatorId, user]);

  const hasInvalidAdminSession =
    enabled && Boolean(isAdminPortalSession) && !sessionOperatorId;

  return {
    operator: profileQuery.data,
    resolvedOperatorId: sessionOperatorId,
    isLoading:
      enabled &&
      Boolean(isAdminPortalSession) &&
      Boolean(sessionOperatorId) &&
      profileQuery.isLoading,
    isError:
      enabled &&
      Boolean(isAdminPortalSession) &&
      (hasInvalidAdminSession || profileQuery.isError),
    refetch,
  };
}
