/**
 * Authentication API Module
 * ==========================
 * Handles user authentication operations.
 */

import { apiFetch, apiRequest, parseJsonResponse } from '../_shared/httpClient';
import type {
  ActionLinkExchangeResponse,
  ChangePasswordPayload,
  ChangePasswordResponse,
  LoginResponse,
  PasswordResetCompleteResponse,
  PasswordResetRequestResponse,
  SignUpData,
  SignUpResponse,
} from './types';

export async function login(
  companyId: string,
  email: string,
  password: string,
): Promise<LoginResponse> {
  const response = await apiFetch('/auth/user/login', {
    method: 'POST',
    auth: false,
    body: { companyId, identifier: email, password },
  });

  const { data: json } = await parseJsonResponse<LoginResponse & { message?: string }>(response);

  if (!response.ok) {
    throw new Error(json?.message || `HTTP ${response.status} Error during login`);
  }

  if (json?.statusCode !== 'OK') {
    throw new Error(json?.message || 'Error during login');
  }

  return json;
}

export async function adminLogin(
  identifier: string,
  password: string,
): Promise<LoginResponse> {
  const response = await apiFetch('/auth/admin/login', {
    method: 'POST',
    auth: false,
    body: { identifier, password },
  });

  const { data: json } = await parseJsonResponse<LoginResponse & { message?: string }>(response);

  if (!response.ok) {
    throw new Error(json?.message || `HTTP ${response.status}`);
  }

  return json as LoginResponse;
}

export async function logout() {
  const res = await apiFetch('/logout', {
    method: 'POST',
  });

  if (!res.ok) {
    throw new Error('Logout failed');
  }

  return res.json();
}

export async function signUp(userData: SignUpData): Promise<SignUpResponse> {
  const response = await apiFetch('/company/signup', {
    method: 'POST',
    auth: false,
    body: userData,
  });

  const { data: json } = await parseJsonResponse<
    SignUpResponse & {
      statusCode?: string;
      message?: string;
      errors?: Record<string, string>;
    }
  >(response);

  if (!response.ok) {
    throw {
      statusCode: json?.statusCode ?? String(response.status),
      message: json?.message ?? `HTTP ${response.status} Error during signup`,
      errors: json?.errors,
    };
  }

  return json as SignUpResponse;
}

export async function changePassword(
  payload: ChangePasswordPayload,
): Promise<ChangePasswordResponse> {
  const response = await apiRequest<ChangePasswordResponse>(
    '/auth/user/change-password',
    {
      method: 'POST',
      body: payload,
      fallbackErrorMessage: 'Failed to change password',
    },
  );

  if (response.statusCode !== 'OK') {
    throw new Error(response.message || 'Failed to change password');
  }

  return response;
}

export async function requestPasswordReset(
  email: string,
): Promise<PasswordResetRequestResponse> {
  return apiRequest<PasswordResetRequestResponse>(
    '/auth/password-reset/request',
    {
      method: 'POST',
      auth: false,
      body: { email },
      fallbackErrorMessage:
        'パスワードリセット用メールの送信に失敗しました。',
    },
  );
}

export async function exchangeActionLink(
  token: string,
): Promise<ActionLinkExchangeResponse> {
  return apiRequest<ActionLinkExchangeResponse>('/auth/action-links/exchange', {
    method: 'POST',
    auth: false,
    body: { token },
    fallbackErrorMessage: 'アクションリンクを確認できませんでした。',
  });
}

export async function exchangePasswordResetActionLink(
  token: string,
): Promise<ActionLinkExchangeResponse> {
  return exchangeActionLink(token);
}

export async function completePasswordReset(
  actionSessionToken: string,
  newPassword: string,
): Promise<PasswordResetCompleteResponse> {
  return apiRequest<PasswordResetCompleteResponse>(
    '/auth/password-reset/complete',
    {
      method: 'POST',
      auth: false,
      headers: {
        'X-Action-Session': actionSessionToken,
      },
      body: { newPassword },
      fallbackErrorMessage: 'パスワードの再設定に失敗しました。',
    },
  );
}
