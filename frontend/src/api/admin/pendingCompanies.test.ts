import { afterEach, describe, expect, it, vi } from 'vitest';
import { sendAdminCompanyPasswordResetLink } from './pendingCompanies';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('sendAdminCompanyPasswordResetLink', () => {
  it('posts without a body to the company password reset endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          statusCode: 'OK',
          message: 'Password reset link queued.',
          timestamp: '2026-07-28T14:00:00',
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    await sendAdminCompanyPasswordResetLink('PC000000001');

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/admin/companies/PC000000001/password-reset-link',
      expect.objectContaining({
        method: 'POST',
      }),
    );
    expect(fetchMock.mock.calls[0]?.[1]?.body).toBeUndefined();
  });
});
