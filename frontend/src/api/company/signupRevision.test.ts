import { afterEach, describe, expect, it, vi } from 'vitest';
import { getSignupRevision, updateSignupRevision } from './signupRevision';
import type { SignupRevisionRequest } from './types';

afterEach(() => {
  vi.unstubAllGlobals();
});

const revisionPayload: SignupRevisionRequest = {
  emailVerificationChallengeId: 'challenge-id',
  corporateNumber: '1234567890123',
  corporateName: '株式会社サンプル',
  corporateNameKana: 'カブシキガイシャサンプル',
  companyNameEnglish: 'Sample Inc.',
  corporatePhoneNumber: '0312345678',
  postalCode: '1000001',
  prefecture: '東京都',
  municipality: '千代田区',
  blocknum: '1-1',
  buildingName: '',
  lastName: '山田',
  firstName: '太郎',
  lastNameKana: 'ヤマダ',
  firstNameKana: 'タロウ',
  position: '部長',
  phoneNumber: '09012345678',
  faxNumber: '',
  email: 'taro@example.com',
  methodology: ['EN-R-002'],
  activityInformation: '-',
};

describe('signup revision API', () => {
  it('gets revision data without bearer auth and sends the action session header', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(revisionPayload), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await getSignupRevision('email-link-token');

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/company/signup/revision',
      expect.objectContaining({ method: 'GET' }),
    );
    const requestOptions = fetchMock.mock.calls[0][1] as RequestInit;
    const headers = new Headers(requestOptions.headers);
    expect(headers.get('X-Action-Session')).toBe('email-link-token');
    expect(headers.has('Authorization')).toBe(false);
  });

  it('puts the exact revision body with the same action session', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ statusCode: 'OK' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await updateSignupRevision('email-link-token', revisionPayload);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/company/signup/revision',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify(revisionPayload),
      }),
    );
    const requestOptions = fetchMock.mock.calls[0][1] as RequestInit;
    expect(new Headers(requestOptions.headers).get('X-Action-Session')).toBe(
      'email-link-token',
    );
  });
});
