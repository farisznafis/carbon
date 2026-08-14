import { useEffect } from 'react';
import { getProfile, type ProfileViewModel } from '@/api/profile';
import type { User } from '@/api/users';
import { useProfileStore } from '@/stores/ProfileStore';

const EMPTY_ACCOUNT_NAME = 'アカウント';
const EMPTY_ACCOUNT_EMAIL = 'メールアドレス未設定';
const inFlightProfileRequests = new Map<string, Promise<ProfileViewModel>>();

function loadAccountProfile(userId: string) {
  const inFlightRequest = inFlightProfileRequests.get(userId);
  if (inFlightRequest) return inFlightRequest;

  const request = getProfile().finally(() => {
    inFlightProfileRequests.delete(userId);
  });

  inFlightProfileRequests.set(userId, request);
  return request;
}

function usableText(value: string | null | undefined) {
  const normalized = value?.trim();
  return normalized && normalized !== '-' ? normalized : '';
}

function emailLocalPart(email: string) {
  return email.split('@')[0]?.trim() || '';
}

export function useAccountIdentity(user: User) {
  const profile = useProfileStore((state) => state.profile);
  const setProfile = useProfileStore((state) => state.setProfile);

  useEffect(() => {
    if (profile) return;

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
  }, [profile, setProfile, user.id]);

  const lastName = usableText(profile?.lastName);
  const firstName = usableText(profile?.firstName);
  const fullName = [lastName, firstName].filter(Boolean).join(' ');
  const email = usableText(profile?.email) || usableText(user.email);
  const displayName =
    fullName ||
    usableText(user.name) ||
    usableText(user.username) ||
    usableText(user.companyName) ||
    emailLocalPart(email) ||
    EMPTY_ACCOUNT_NAME;

  return {
    displayName,
    email: email || EMPTY_ACCOUNT_EMAIL,
  };
}
