import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { useRedirectIfLoggedIn } from './useRedirectIfLoggedIn';

const { authState } = vi.hoisted(() => ({
  authState: {
    isAuthenticated: false,
    user: null as { id: string; role: string; siteId?: string } | null,
  },
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => authState,
}));

/** Hosts the login redirect hook inside a real router route. */
function LoginRoute() {
  useRedirectIfLoggedIn();
  return <div>Login page</div>;
}

/** Renders all role home routes so navigation can be checked by destination. */
function renderLoginRoute() {
  return render(
    <MemoryRouter
      initialEntries={['/login']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <Routes>
        <Route path="/login" element={<LoginRoute />} />
        <Route path="/admin/dashboard" element={<div>Admin home</div>} />
        <Route path="/master/dashboard" element={<div>Master home</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('useRedirectIfLoggedIn', () => {
  it.each([
    ['admin', 'Admin home'],
    ['master', 'Master home'],
  ])('routes an authenticated %s account to %s', async (role, home) => {
    authState.isAuthenticated = true;
    authState.user = { id: '42', role };

    renderLoginRoute();

    expect(await screen.findByText(home)).toBeInTheDocument();
    expect(screen.queryByText('Login page')).not.toBeInTheDocument();
  });

  it('keeps an unauthenticated visitor on login', () => {
    authState.isAuthenticated = false;
    authState.user = null;

    renderLoginRoute();

    expect(screen.getByText('Login page')).toBeInTheDocument();
  });
});
