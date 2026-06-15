import { apiFetch, parseJsonResponse } from '../_shared/httpClient';
import {
  mapProfileApiResponseToProfileViewModel,
  type ProfileViewModel,
} from './mappers';
import type { ProfileApiResponse } from './types';

export async function getProfile(): Promise<ProfileViewModel> {
  const response = await apiFetch('/auth/user/profile', {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
  });
  const { data: json, rawBody } = await parseJsonResponse<ProfileApiResponse>(response);

  if (!response.ok) {
    const message =
      json?.message ||
      rawBody ||
      `Failed to fetch profile: HTTP ${response.status}`;
    throw new Error(message);
  }

  if (!json?.profile) {
    throw new Error('Profile data not found');
  }

  return mapProfileApiResponseToProfileViewModel(json);
}

export async function getUserProfileById(
  userId: string,
): Promise<ProfileViewModel> {
  const response = await apiFetch(`/user/profile/${encodeURIComponent(userId)}`, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
  });
  const { data: json, rawBody } =
    await parseJsonResponse<ProfileApiResponse>(response);

  if (!response.ok) {
    const message =
      json?.message ||
      rawBody ||
      `Failed to fetch user profile: HTTP ${response.status}`;
    throw new Error(message);
  }

  if (!json?.profile) {
    throw new Error('Profile data not found');
  }

  return mapProfileApiResponseToProfileViewModel(json);
}
