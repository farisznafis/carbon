import { apiRequest } from '@/api/_shared/httpClient';
import {
  mapAdminOperator,
  mapAdminOperatorListResponse,
  mapAdminOperatorProfileResponse,
} from './mappers';
import type {
  AdminOperator,
  AdminOperatorListApiResponse,
  AdminOperatorListResponse,
  AdminOperatorProfileApiResponse,
  CreateAdminOperatorPayload,
  CreateAdminOperatorResponse,
  UpdateAdminOperatorPayload,
  UpdateAdminOperatorResponse,
} from './types';

/** Calls GET /admin/companies/operator-user to load operators for the logged-in admin company. */
export async function getAdminOperators(): Promise<AdminOperatorListResponse> {
  const response = await apiRequest<AdminOperatorListApiResponse>(
    '/admin/companies/operator-user',
    {
      method: 'GET',
      fallbackErrorMessage: '運用者一覧を取得できませんでした。',
    },
  );

  return mapAdminOperatorListResponse(response);
}

/** Calls GET /admin/companies/operator-user/{userId} for a single operator profile. */
export async function getAdminOperatorProfile(
  userId: string,
): Promise<AdminOperator> {
  const response = await apiRequest<AdminOperatorProfileApiResponse>(
    `/admin/companies/operator-user/${encodeURIComponent(userId)}`,
    {
      method: 'GET',
      fallbackErrorMessage: '運用者のプロファイルを取得できませんでした。',
    },
  );

  return mapAdminOperatorProfileResponse(response);
}

/** Calls POST /admin/companies/operator-user to create a new operator account. */
export async function createAdminOperator(
  payload: CreateAdminOperatorPayload,
): Promise<AdminOperator | null> {
  const response = await apiRequest<CreateAdminOperatorResponse>(
    '/admin/companies/operator-user',
    {
      method: 'POST',
      body: payload,
      fallbackErrorMessage: '運用者を登録できませんでした。',
    },
  );

  return response.user ? mapAdminOperator(response.user) : null;
}

/** Calls PATCH /admin/companies/operator-user/{userId} to update an operator profile and permissions. */
export async function updateAdminOperator(
  userId: string,
  payload: UpdateAdminOperatorPayload,
): Promise<AdminOperator | null> {
  const response = await apiRequest<UpdateAdminOperatorResponse>(
    `/admin/companies/operator-user/${encodeURIComponent(userId)}`,
    {
      method: 'PATCH',
      body: payload,
      fallbackErrorMessage: '運用者を更新できませんでした。',
    },
  );

  const updatedOperator = response.user ?? response.profile;
  return updatedOperator ? mapAdminOperator(updatedOperator) : null;
}

export type {
  AdminOperator,
  AdminOperatorApiItem,
  AdminOperatorListResponse,
  AdminOperatorPermissionCode,
  CreateAdminOperatorPayload,
  UpdateAdminOperatorPayload,
} from './types';
