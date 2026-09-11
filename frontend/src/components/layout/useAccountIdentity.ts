import { useCallback, useEffect, useMemo } from 'react';
import { getProfile, type ProfileViewModel } from '@/api/profile';
import type { User } from '@/api/users';
import { findCurrentAdminOperator } from '@/features/admin/operators/currentAdminOperator';
import {
  useAdminOperatorsQuery,
  useAdminOperatorProfileQuery,
} from '@/hooks/admin/useAdminOperatorsQuery';
import { paths } from '@/routes/paths';
import { useProfileStore } from '@/stores/ProfileStore';
import { formatUserRole } from '@/utils/formatUserRole';

const EMPTY_ACCOUNT_NAME = 'アカウント';
const EMPTY_ACCOUNT_EMAIL = 'メールアドレス未設定';
const inFlightProfileRequests = new Map<string, Promise<ProfileViewModel>>();

/** Deduplicates GET /auth/user/profile while the navbar is mounted more than once. */
function loadAccountProfile(userId: string) {
  const inFlightRequest = inFlightProfileRequests.get(userId);
  if (inFlightRequest) return inFlightRequest;

  const request = getProfile().finally(() => {
    inFlightProfileRequests.delete(userId);
  });

  inFlightProfileRequests.set(userId, request);
  return request;
}

/** Returns non-placeholder text that can safely be shown in the account control. */
function usableText(value: string | null | undefined) {
  const normalized = value?.trim();
  return normalized && normalized !== '-' ? normalized : '';
}

/** Uses the local part of an email as the final readable identity fallback. */
function emailLocalPart(email: string) {
  return email.split('@')[0]?.trim() || '';
}

/** Extracts one Unicode-safe initial from a real person name. */
function getNameInitial(name: string): string | null {
  return Array.from(name.trim())[0] ?? null;
}

/** Rejects the legacy admin-login placeholder before building a detail endpoint. */
function getSessionOperatorId(userId: string): string | null {
  const normalizedId = userId.trim();
  return normalizedId && normalizedId.toLocaleLowerCase('en-US') !== 'admin'
    ? normalizedId
    : null;
}

/** Resolves navbar identity and the role-aware profile destination for one session. */
export function useAccountIdentity(user: User) {
  const profile = useProfileStore((state) => state.profile);
  const setProfile = useProfileStore((state) => state.setProfile);
  const isAdminAccount = user.role === 'admin';
  const sessionOperatorId = getSessionOperatorId(user.id);
  const needsOperatorIdResolution = isAdminAccount && !sessionOperatorId;
  const adminOperatorsQuery = useAdminOperatorsQuery(
    needsOperatorIdResolution,
    'caller',
  );
  const matchedOperator = useMemo(
    () =>
      findCurrentAdminOperator(
        adminOperatorsQuery.data?.users ?? [],
        user,
      ),
    [adminOperatorsQuery.data?.users, user],
  );
  const resolvedOperatorId = sessionOperatorId ?? matchedOperator?.id;
  const adminOperatorProfileQuery = useAdminOperatorProfileQuery(
    resolvedOperatorId,
    isAdminAccount && Boolean(resolvedOperatorId),
    'caller',
  );
  const currentAdminOperator = adminOperatorProfileQuery.data;

  useEffect(() => {
    if (isAdminAccount || profile) return;

    let isActive = true;

    void loadAccountProfile(user.id)
      .then((nextProfile) => {
        if (isActive) setProfile(nextProfile);
      })
      .catch(() => {
        // Session data remains the non-blocking fallback for unsupported roles.
      });

    return () => {
      isActive = false;
    };
  }, [isAdminAccount, profile, setProfile, user.id]);

  const lastName = usableText(
    isAdminAccount ? currentAdminOperator?.lastName : profile?.lastName,
  );
  const firstName = usableText(
    isAdminAccount ? currentAdminOperator?.firstName : profile?.firstName,
  );
  const fullName = [lastName, firstName].filter(Boolean).join(' ');
  const identityName =
    fullName ||
    usableText(currentAdminOperator?.name) ||
    usableText(user.name);
  const email =
    usableText(
      isAdminAccount ? currentAdminOperator?.email : profile?.email,
    ) || usableText(user.email);
  const displayName =
    identityName ||
    usableText(currentAdminOperator?.username) ||
    usableText(user.name) ||
    usableText(user.username) ||
    usableText(user.companyName) ||
    emailLocalPart(email) ||
    EMPTY_ACCOUNT_NAME;
  const accountRole =
    usableText(
      isAdminAccount ? currentAdminOperator?.role : profile?.role,
    ) || usableText(user.role);

  /** Resolves the current profile route from the signed-in operator detail. */
  const resolveProfilePath = useCallback(async (): Promise<string | null> => {
    if (!isAdminAccount) return paths.profile;

    if (currentAdminOperator) {
      return paths.adminOperatorProfile(currentAdminOperator.id);
    }

    if (resolvedOperatorId) {
      const refreshedProfile = await adminOperatorProfileQuery.refetch();
      return refreshedProfile.data
        ? paths.adminOperatorProfile(refreshedProfile.data.id)
        : null;
    }

    const refreshedOperators = await adminOperatorsQuery.refetch();
    const refreshedOperator = findCurrentAdminOperator(
      refreshedOperators.data?.users ?? [],
      user,
    );

    return refreshedOperator
      ? paths.adminOperatorProfile(refreshedOperator.id)
      : null;
  }, [
    adminOperatorProfileQuery,
    adminOperatorsQuery,
    currentAdminOperator,
    isAdminAccount,
    resolvedOperatorId,
    user,
  ]);

  return {
    displayName,
    avatarInitial: getNameInitial(identityName),
    email: email || EMPTY_ACCOUNT_EMAIL,
    roleLabel: formatUserRole(accountRole),
    resolveProfilePath,
  };
}
