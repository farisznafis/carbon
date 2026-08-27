import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { User } from '@/api/users';
import { Navbar } from './Navbar';

const { authState, logout, resolveProfilePath } = vi.hoisted(() => ({
  authState: {
    isAuthenticated: true,
    user: null as User | null,
  },
  logout: vi.fn(),
  resolveProfilePath: vi.fn(),
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    ...authState,
    logout,
  }),
}));

vi.mock('@/hooks/useNotifications', () => ({
  useNotifications: () => ({
    notifications: [],
    loading: false,
    error: null,
    unreadCount: 0,
    markAsRead: vi.fn(),
    deleteNotification: vi.fn(),
  }),
}));

vi.mock('./useAccountIdentity', () => ({
  useAccountIdentity: () => ({
    displayName: '山田 太郎',
    email: 'taro@example.com',
    resolveProfilePath,
  }),
}));

function renderNavbar() {
  return render(
    <MemoryRouter
      initialEntries={['/dashboard']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <Navbar />
      <Routes>
        <Route path="/dashboard" element={<div>Dashboard page</div>} />
        <Route path="/profile" element={<div>Profile page</div>} />
        <Route
          path="/admin/operators/:userId"
          element={<div>Admin operator profile page</div>}
        />
        <Route path="/admin/login" element={<div>Admin login page</div>} />
        <Route path="/" element={<div>Home page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('Navbar account menu', () => {
  beforeEach(() => {
    logout.mockReset();
    logout.mockResolvedValue(undefined);
    resolveProfilePath.mockReset();
    resolveProfilePath.mockResolvedValue('/profile');
    authState.isAuthenticated = true;
    authState.user = {
      id: 'USER-1',
      email: 'taro@example.com',
      role: 'user',
      siteId: 'SITE-1',
    };
  });

  it('opens the matched operator profile for an admin dashboard account', async () => {
    const user = userEvent.setup();
    authState.user = { ...authState.user!, role: 'admin' };
    resolveProfilePath.mockResolvedValue('/admin/operators/7');
    renderNavbar();

    await user.click(
      screen.getByRole('button', {
        name: 'アカウントメニューを開く：山田 太郎',
      }),
    );
    await user.click(screen.getByRole('menuitem', { name: 'プロファイル' }));

    expect(resolveProfilePath).toHaveBeenCalledTimes(1);
    expect(
      await screen.findByText('Admin operator profile page'),
    ).toBeInTheDocument();
  });

  it('replaces separate actions with one identity trigger and navigates to profile', async () => {
    const user = userEvent.setup();
    renderNavbar();

    const trigger = screen.getByRole('button', {
      name: 'アカウントメニューを開く：山田 太郎',
    });
    expect(trigger).toHaveTextContent('山田 太郎');
    expect(trigger).toHaveTextContent('taro@example.com');
    expect(screen.queryByRole('button', { name: 'ログアウト' })).toBeNull();

    await user.click(trigger);
    await user.click(screen.getByRole('menuitem', { name: 'プロファイル' }));
    expect(await screen.findByText('Profile page')).toBeInTheDocument();
  });

  it.each([
    ['user', 'Home page'],
    ['master', 'Home page'],
    ['admin', 'Admin login page'],
  ] as const)('logs out the %s role through the dropdown', async (role, target) => {
    const user = userEvent.setup();
    authState.user = { ...authState.user!, role };
    renderNavbar();

    await user.click(
      screen.getByRole('button', {
        name: 'アカウントメニューを開く：山田 太郎',
      }),
    );
    await user.click(screen.getByRole('menuitem', { name: 'ログアウト' }));

    expect(logout).toHaveBeenCalledTimes(1);
    expect(await screen.findByText(target)).toBeInTheDocument();
  });

  it('keeps the guest login and signup actions', () => {
    authState.isAuthenticated = false;
    authState.user = null;
    renderNavbar();

    expect(screen.getByRole('button', { name: 'ログイン' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '新規登録' })).toBeInTheDocument();
  });
});
