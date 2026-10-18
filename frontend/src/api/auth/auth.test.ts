import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  login,
  completePasswordReset,
  exchangePasswordResetActionLink,
  requestPasswordReset,
} from './auth';

afterEach(() => {
  vi.unstubAllGlobals();
});

const createFetchMock = (responseBody: object) => {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(responseBody), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  );
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
};

describe('user login API', () => {
  it.each(['OK', '200'])('accepts the %s success status', async (statusCode) => {
    createFetchMock({
      statusCode,
      role: 'master',
      token: 'login-token',
      userId: 'user-42',
      companyName: 'Example Company',
    });

    const response = await login('company-1', 'master@example.com', 'password');

    expect(response.statusCode).toBe(statusCode);
    expect(response.role).toBe('master');
  });
});

describe('password reset API', () => {
  it('requests a reset link with the email address', async () => {
    const fetchMock = createFetchMock({ statusCode: 'OK' });

    await requestPasswordReset('master@example.com');

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/password-reset/request',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ email: 'master@example.com' }),
      }),
    );
  });

  it('exchanges the action-link token', async () => {
    const fetchMock = createFetchMock({
      statusCode: 'OK',
      actionSessionToken: 'action-session',
      purpose: 'PASSWORD_RESET',
      expiresInSeconds: 900,
    });

    await exchangePasswordResetActionLink('email-link-token');

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/action-links/exchange',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ token: 'email-link-token' }),
      }),
    );
  });

  it('completes the reset with the action session header', async () => {
    const fetchMock = createFetchMock({ statusCode: 'OK' });

    await completePasswordReset('action-session', 'A-strong-password-123');

    const requestOptions = fetchMock.mock.calls[0]?.[1];
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/password-reset/complete',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ newPassword: 'A-strong-password-123' }),
      }),
    );
    expect(
      new Headers(requestOptions?.headers).get('X-Action-Session'),
    ).toBe('action-session');
  });
});
