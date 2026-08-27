import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  createAdminOperator,
  getAdminOperatorProfile,
  getAdminOperators,
  updateAdminOperator,
} from './index';

/** Creates a JSON response used by the operator API tests. */
function jsonResponse(body: unknown) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('admin operator API', () => {
  it('loads and normalizes the actual operator list fields', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({
          statusCode: 'OK',
          users: [
            {
              id: 12,
              username: 'yamada.taro',
              firstName: '太郎',
              lastName: '山田',
              userPosition: '担当者',
              siteId: 5,
              siteName: '東京拠点',
              permissions: ['PROJECT_READ', 'PROJECT_VIEW', 'UNKNOWN_PERMISSION'],
            },
          ],
        }),
      ),
    );

    const response = await getAdminOperators();

    expect(fetch).toHaveBeenCalledWith(
      '/api/admin/companies/operator-user',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(response.users[0]).toMatchObject({
      id: '12',
      firstName: '太郎',
      lastName: '山田',
      userPosition: '担当者',
      siteId: '5',
      siteName: '東京拠点',
      permissions: ['PROJECT_READ', 'UNKNOWN_PERMISSION'],
    });
  });

  it('encodes the user ID when loading a profile', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({ statusCode: 'OK', profile: { id: 1 } }),
      ),
    );

    await getAdminOperatorProfile('1/2');

    expect(fetch).toHaveBeenCalledWith(
      '/api/admin/companies/operator-user/1%2F2',
      expect.objectContaining({ method: 'GET' }),
    );
  });

  it('posts the operator account without position or site assignment', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse({ statusCode: 'OK', user: { id: 3 } }));
    vi.stubGlobal('fetch', fetchMock);

    await createAdminOperator({
      username: 'ito.kenichi',
      firstName: '健一',
      firstNameKana: 'ケンイチ',
      lastName: '伊藤',
      lastNameKana: 'イトウ',
      email: 'ito@example.com',
      phoneNumber: '0354321235',
      password: 'Password1',
      role: 'operator',
      permissionCodes: ['PROJECT_READ'],
    });

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/admin/companies/operator-user',
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('"permissionCodes":["PROJECT_READ"]'),
      }),
    );
    expect(fetchMock.mock.calls[0]?.[1]?.body).not.toContain('userPosition');
    expect(fetchMock.mock.calls[0]?.[1]?.body).not.toContain('siteId');
  });

  it('patches the encoded operator ID with updated profile and permissions', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse({ statusCode: 'OK', profile: { id: 3 } }));
    vi.stubGlobal('fetch', fetchMock);

    await updateAdminOperator('3/4', {
      username: 'ito.kenichi',
      firstName: '健一',
      firstNameKana: 'ケンイチ',
      lastName: '伊藤',
      lastNameKana: 'イトウ',
      email: 'ito@example.com',
      phoneNumber: '0354321235',
      permissionCodes: ['PROJECT_READ'],
    });

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/admin/companies/operator-user/3%2F4',
      expect.objectContaining({
        method: 'PATCH',
        body: expect.stringContaining('"permissionCodes":["PROJECT_READ"]'),
      }),
    );
    expect(fetchMock.mock.calls[0]?.[1]?.body).not.toContain('userPosition');
    expect(fetchMock.mock.calls[0]?.[1]?.body).not.toContain('siteId');
  });

  it('omits permissionCodes when updating an administrator profile', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse({ statusCode: 'OK', profile: { id: 3 } }));
    vi.stubGlobal('fetch', fetchMock);

    await updateAdminOperator('3', {
      username: 'admin.primary',
      firstName: '太郎',
      firstNameKana: 'タロウ',
      lastName: '管理',
      lastNameKana: 'カンリ',
      email: 'admin@example.com',
      phoneNumber: '0354321235',
    });

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/admin/companies/operator-user/3',
      expect.objectContaining({ method: 'PATCH' }),
    );
    expect(fetchMock.mock.calls[0]?.[1]?.body).not.toContain('permissionCodes');
  });
});
