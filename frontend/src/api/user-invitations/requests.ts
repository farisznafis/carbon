import { apiRequest } from '@/api/_shared/httpClient';
import type {
  CompleteUserInvitationPayload,
  CompleteUserInvitationResponse,
  CreateUserInvitationPayload,
  CreateUserInvitationResponse,
  UserInvitationContext,
} from './types';

/** POST /user/invitations - sends or reissues a user invitation email. */
export function createUserInvitation(
  payload: CreateUserInvitationPayload,
): Promise<CreateUserInvitationResponse> {
  return apiRequest<CreateUserInvitationResponse>('/user/invitations', {
    method: 'POST',
    body: payload,
    fallbackErrorMessage: '招待メールを送信できませんでした。',
  });
}

/** GET /auth/invitation - loads invitation context using an in-memory action session. */
export function getUserInvitationContext(
  actionSessionToken: string,
): Promise<UserInvitationContext> {
  return apiRequest<UserInvitationContext>('/auth/invitation', {
    method: 'GET',
    auth: false,
    headers: {
      'X-Action-Session': actionSessionToken,
    },
    fallbackErrorMessage: '招待情報を取得できませんでした。',
  });
}

/** POST /auth/invitation/complete - creates the invited account from an action session. */
export function completeUserInvitation(
  actionSessionToken: string,
  payload: CompleteUserInvitationPayload,
): Promise<CompleteUserInvitationResponse> {
  return apiRequest<CompleteUserInvitationResponse>(
    '/auth/invitation/complete',
    {
      method: 'POST',
      auth: false,
      headers: {
        'X-Action-Session': actionSessionToken,
      },
      body: payload,
      fallbackErrorMessage: 'アカウントを登録できませんでした。',
    },
  );
}
