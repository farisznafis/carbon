import { apiRequest } from '@/api/_shared/httpClient';
import type {
  SignupRevisionRequest,
  SignupRevisionResponse,
  SignupRevisionUpdateResponse,
} from './types';

const SIGNUP_REVISION_PATH = '/company/signup/revision';

function actionSessionHeaders(actionSessionToken: string) {
  return {
    'X-Action-Session': actionSessionToken,
  };
}

export function getSignupRevision(
  actionSessionToken: string,
): Promise<SignupRevisionResponse> {
  return apiRequest<SignupRevisionResponse>(SIGNUP_REVISION_PATH, {
    method: 'GET',
    auth: false,
    headers: actionSessionHeaders(actionSessionToken),
    fallbackErrorMessage: '修正対象の登録情報を取得できませんでした。',
  });
}

export function updateSignupRevision(
  actionSessionToken: string,
  payload: SignupRevisionRequest,
): Promise<SignupRevisionUpdateResponse> {
  return apiRequest<SignupRevisionUpdateResponse>(SIGNUP_REVISION_PATH, {
    method: 'PUT',
    auth: false,
    headers: actionSessionHeaders(actionSessionToken),
    body: payload,
    fallbackErrorMessage: '登録情報を再申請できませんでした。',
  });
}
