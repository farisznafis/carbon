import { render, screen, act } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AuthProvider, useAuth } from '@/contexts/AuthContext';

const storedUser = {
  id: 'U-1',
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
