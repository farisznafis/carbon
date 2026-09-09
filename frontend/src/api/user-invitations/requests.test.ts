import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  completeUserInvitation,
  createUserInvitation,
  getUserInvitationContext,
} from './requests';

/** Creates a successful fetch mock and returns it for request assertions. */
function mockSuccessfulRequest(responseBody: object) {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(responseBody), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  );
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('user invitation API', () => {
  it('sends an invitation with an optional site', async () => {
    const fetchMock = mockSuccessfulRequest({ statusCode: 'OK' });

    await createUserInvitation({
      email: 'invitee@example.com',
      siteId: null,
    });

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/user/invitations',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          email: 'invitee@example.com',
          siteId: null,
        }),
      }),
    );
  });

  it('loads the invitation context with the action session header', async () => {
    const fetchMock = mockSuccessfulRequest({
      email: 'invitee@example.com',
      companyName: 'GX株式会社',
      siteId: 42,
      siteName: '東京拠点',
    });

    await getUserInvitationContext('action-session');

    const requestOptions = fetchMock.mock.calls[0]?.[1];
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/invitation',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(new Headers(requestOptions?.headers).get('X-Action-Session')).toBe(
      'action-session',
    );
    expect(new Headers(requestOptions?.headers).has('Authorization')).toBe(false);
  });

  it('completes the invitation without including an email address', async () => {
    const fetchMock = mockSuccessfulRequest({ statusCode: 'OK' });
    const payload = {
      username: 'yamada_taro',
      firstName: '太郎',
      firstNameKana: 'タロウ',
      lastName: '山田',
      lastNameKana: 'ヤマダ',
      phoneNumber: '09012345678',
      userPosition: '部長',
      password: 'Strong-password-123',
    };

    await completeUserInvitation('action-session', payload);

    const requestOptions = fetchMock.mock.calls[0]?.[1];
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/invitation/complete',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    );
    expect(requestOptions?.body).not.toContain('email');
    expect(new Headers(requestOptions?.headers).get('X-Action-Session')).toBe(
      'action-session',
    );
  });
});
