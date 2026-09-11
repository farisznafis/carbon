import { useCallback, useMemo } from 'react';
import type { User } from '@/api/users';
import { findCurrentAdminOperator } from '@/features/admin/operators/currentAdminOperator';
import {
  useAdminOperatorsQuery,
  useAdminOperatorProfileQuery,
} from './useAdminOperatorsQuery';

/** Rejects the legacy admin-login placeholder before building a detail endpoint. */
function getSessionOperatorId(userId: string): string | null {
  const normalizedId = userId.trim();
  return normalizedId && normalizedId.toLocaleLowerCase('en-US') !== 'admin'
    ? normalizedId
    : null;
}

/** Loads the signed-in admin/operator profile while supporting legacy sessions without an ID. */
export function useCurrentAdminOperatorProfileQuery(
  user: User | null,
  enabled = true,
) {
  const isAdminPortalSession = user?.role === 'admin';
  const sessionOperatorId = user ? getSessionOperatorId(user.id) : null;
  const needsOperatorIdResolution =
    enabled && isAdminPortalSession && !sessionOperatorId;
  const operatorsQuery = useAdminOperatorsQuery(
    needsOperatorIdResolution,
    'caller',
  );
  const matchedOperator = useMemo(
    () =>
      user
        ? findCurrentAdminOperator(operatorsQuery.data?.users ?? [], user)
        : null,
    [operatorsQuery.data?.users, user],
  );
  const resolvedOperatorId = sessionOperatorId ?? matchedOperator?.id;
  const profileQuery = useAdminOperatorProfileQuery(
    resolvedOperatorId,
    enabled && isAdminPortalSession && Boolean(resolvedOperatorId),
    'caller',
  );

  /** Retries ID resolution first and then refreshes the resolved detail profile. */
  const refetch = useCallback(async () => {
    if (!user || !isAdminPortalSession) return null;

    let operatorId = resolvedOperatorId;
    if (!operatorId) {
      const refreshedOperators = await operatorsQuery.refetch();
      operatorId = findCurrentAdminOperator(
        refreshedOperators.data?.users ?? [],
        user,
      )?.id;
    }

    if (!operatorId) return null;
    if (operatorId === resolvedOperatorId) {
      return (await profileQuery.refetch()).data ?? null;
    }

    return matchedOperator ?? null;
  }, [
    isAdminPortalSession,
    matchedOperator,
    operatorsQuery,
    profileQuery,
    resolvedOperatorId,
    user,
  ]);

  return {
    operator: profileQuery.data,
    resolvedOperatorId,
    isLoading:
      enabled &&
      Boolean(isAdminPortalSession) &&
      (needsOperatorIdResolution
        ? operatorsQuery.isLoading ||
          Boolean(resolvedOperatorId && profileQuery.isLoading)
        : profileQuery.isLoading),
    isError:
      enabled &&
      Boolean(isAdminPortalSession) &&
      (operatorsQuery.isError || profileQuery.isError),
    refetch,
  };
}
