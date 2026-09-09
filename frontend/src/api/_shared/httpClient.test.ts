import { afterEach, describe, expect, it, vi } from 'vitest';
import { PAGE_FORBIDDEN_EVENT } from './forbiddenAccess';
import { apiRequest } from './httpClient';

/** Builds the backend 403 response used by the access-handling tests. */
function createForbiddenResponse() {
  return new Response(
    JSON.stringify({
      statusCode: 'FORBIDDEN',
      message: 'アクセスできません。',
    }),
    {
      status: 403,
      headers: { 'Content-Type': 'application/json' },
    },
  );
}

describe('apiRequest forbidden handling', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, '', '/');
  });

  it('reports a page-level forbidden state for an admin GET request', async () => {
    window.history.replaceState({}, '', '/admin/projects');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(createForbiddenResponse()));
    const onPageForbidden = vi.fn();
    window.addEventListener(PAGE_FORBIDDEN_EVENT, onPageForbidden);

    await expect(
      apiRequest('/project', { method: 'GET' }),
    ).rejects.toMatchObject({ status: 403 });

    expect(onPageForbidden).toHaveBeenCalledTimes(1);
    expect(
      (onPageForbidden.mock.calls[0]?.[0] as CustomEvent).detail,
    ).toEqual({ requestPath: '/project', routeKey: '/admin/projects' });
    window.removeEventListener(PAGE_FORBIDDEN_EVENT, onPageForbidden);
  });

  it('leaves caller-handled GET actions available for toast handling', async () => {
    window.history.replaceState({}, '', '/admin/projects/PROJECT-1');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(createForbiddenResponse()));
    const onPageForbidden = vi.fn();
    window.addEventListener(PAGE_FORBIDDEN_EVENT, onPageForbidden);

    await expect(
      apiRequest('/project/PROJECT-1/file', {
        method: 'GET',
        forbiddenHandling: 'caller',
      }),
    ).rejects.toMatchObject({ status: 403 });

    expect(onPageForbidden).not.toHaveBeenCalled();
    window.removeEventListener(PAGE_FORBIDDEN_EVENT, onPageForbidden);
  });

  it('does not replace the page for a forbidden mutation', async () => {
    window.history.replaceState({}, '', '/admin/projects');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(createForbiddenResponse()));
    const onPageForbidden = vi.fn();
    window.addEventListener(PAGE_FORBIDDEN_EVENT, onPageForbidden);

    await expect(
      apiRequest('/project', { method: 'POST', body: {} }),
    ).rejects.toMatchObject({ status: 403 });

    expect(onPageForbidden).not.toHaveBeenCalled();
    window.removeEventListener(PAGE_FORBIDDEN_EVENT, onPageForbidden);
  });

  it('uses a safe fallback instead of exposing an HTML infrastructure error', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('<!DOCTYPE html><html><body>gateway error</body></html>', {
          status: 502,
          headers: { 'Content-Type': 'text/html; charset=utf-8' },
        }),
      ),
    );

    await expect(
      apiRequest('/admin/companies/participate/open', {
        fallbackErrorMessage: '入会届一覧の取得に失敗しました。',
      }),
    ).rejects.toMatchObject({
      status: 502,
      message: '入会届一覧の取得に失敗しました。: HTTP 502',
    });
  });
});
