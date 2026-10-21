import { render, screen, act } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AuthProvider, useAuth } from '@/contexts/AuthContext';

const storedUser = {
  id: 'USR-7K3M9QX1',
  email: 'user@example.com',
  role: 'master',
  companyId: 'C-1',
  siteId: 'site001',
};

/** Mirrors a storage change made by another tab, which never notifies the writer. */
function dispatchStorageEvent(key: string) {
  act(() => {
    window.dispatchEvent(new StorageEvent('storage', { key }));
  });
}

function SessionProbe() {
  const { isAuthenticated, loading } = useAuth();
  return (
    <span data-testid="session">
      {loading ? 'loading' : isAuthenticated ? 'authenticated' : 'anonymous'}
    </span>
  );
}

function renderProvider() {
  render(
    <AuthProvider>
      <SessionProbe />
    </AuthProvider>,
  );
}

describe('AuthProvider cross-tab synchronization', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('keeps a token another tab has only half-written', () => {
    renderProvider();

    // The other tab is mid-login: its user has not been broadcast yet.
    localStorage.setItem('authToken', 'token-from-other-tab');
    dispatchStorageEvent('authToken');

    expect(localStorage.getItem('authToken')).toBe('token-from-other-tab');
    expect(screen.getByTestId('session')).toHaveTextContent('anonymous');
  });

  it('adopts the session once the other tab has written all of it', () => {
    renderProvider();

    localStorage.setItem('user', JSON.stringify(storedUser));
    localStorage.setItem('sessionExpiresAt', String(Date.now() + 60 * 60 * 1000));
    localStorage.setItem('authToken', 'token-from-other-tab');
    dispatchStorageEvent('authToken');

    expect(screen.getByTestId('session')).toHaveTextContent('authenticated');
  });

  it('drops a stored session that still holds a numeric 拠点 DB ID', () => {
    localStorage.setItem('user', JSON.stringify({ ...storedUser, role: 'user', siteId: '3' }));
    localStorage.setItem('sessionExpiresAt', String(Date.now() + 60 * 60 * 1000));
    localStorage.setItem('authToken', 'token');

    renderProvider();

    expect(screen.getByTestId('session')).toHaveTextContent('anonymous');
    expect(localStorage.getItem('authToken')).toBeNull();
  });

  it.each([
    ['a numeric user DB ID', { ...storedUser, id: '12' }],
    ['an operator JWT ID', { ...storedUser, role: 'admin', id: '7', siteId: '' }],
    ['a participating ID on an admin session', { ...storedUser, role: 'admin', siteId: '' }],
    ['an operator ID on a user session', { ...storedUser, id: 'OPR-7K3M9QX1' }],
    ['the retired PUSR format', { ...storedUser, id: 'PUSR0000000000000001' }],
  ])('drops a stored session that holds %s', (_label, user) => {
    localStorage.setItem('user', JSON.stringify(user));
    localStorage.setItem('sessionExpiresAt', String(Date.now() + 60 * 60 * 1000));
    localStorage.setItem('authToken', 'token');

    renderProvider();

    expect(screen.getByTestId('session')).toHaveTextContent('anonymous');
    expect(localStorage.getItem('authToken')).toBeNull();
  });

  it.each(['ADM-7K3M9QX1', 'OPR-7K3M9QX1'])('keeps an admin session that holds %s', (id) => {
    localStorage.setItem(
      'user',
      JSON.stringify({ ...storedUser, role: 'admin', id, siteId: '' }),
    );
    localStorage.setItem('sessionExpiresAt', String(Date.now() + 60 * 60 * 1000));
    localStorage.setItem('authToken', 'token');

    renderProvider();

    expect(screen.getByTestId('session')).toHaveTextContent('authenticated');
  });

  it('signs out when another tab clears the session', () => {
    localStorage.setItem('user', JSON.stringify(storedUser));
    localStorage.setItem('sessionExpiresAt', String(Date.now() + 60 * 60 * 1000));
    localStorage.setItem('authToken', 'token');

    renderProvider();
    expect(screen.getByTestId('session')).toHaveTextContent('authenticated');

    localStorage.clear();
    dispatchStorageEvent('authToken');

    expect(screen.getByTestId('session')).toHaveTextContent('anonymous');
  });

  it('ignores storage keys that do not describe the session', () => {
    localStorage.setItem('user', JSON.stringify(storedUser));
    localStorage.setItem('sessionExpiresAt', String(Date.now() + 60 * 60 * 1000));
    localStorage.setItem('authToken', 'token');

    renderProvider();

    localStorage.setItem('dashboard-storage', '{}');
    dispatchStorageEvent('dashboard-storage');

    expect(screen.getByTestId('session')).toHaveTextContent('authenticated');
    expect(localStorage.getItem('authToken')).toBe('token');
  });
});
