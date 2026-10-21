import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useNavigate } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AppShell } from './AppShell';

const { authState } = vi.hoisted(() => ({
  authState: { isAuthenticated: true, user: { role: 'admin', siteId: 'SITE-1' } },
}));

vi.mock('@/contexts/AuthContext', () => ({ useAuth: () => authState }));
// Page tours stay closed here; GuideTourProvider.test.tsx covers when they open.
vi.mock('@/api/guideTours', () => ({
  getSeenGuideTours: () => new Promise(() => undefined),
  markGuideTourSeen: vi.fn(),
}));
vi.mock('./Navbar', () => ({
  Navbar: ({ sidebarControl }: { sidebarControl?: React.ReactNode }) => <header>{sidebarControl}</header>,
}));

/** Mounts the shell at an existing route with a lightweight page. */
function renderShell(pathname: string, children: React.ReactNode = <div>Page content</div>) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={[pathname]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AppShell>{children}</AppShell>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

/** Exposes router history controls to verify the sidebar follows navigation. */
function HistoryControls() {
  const navigate = useNavigate();
  return <><button onClick={() => navigate(-1)}>Back</button><button onClick={() => navigate(1)}>Forward</button></>;
}

describe('AppShell navigation', () => {
  beforeEach(() => {
    localStorage.clear();
    authState.user.role = 'admin';
    vi.stubGlobal('matchMedia', vi.fn().mockImplementation(() => ({
      matches: true,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })));
  });

  it('collapses the admin sidebar and saves the choice', async () => {
    const user = userEvent.setup();
    const { unmount } = renderShell('/admin/projects/PRJ-1');

    expect(screen.getByRole('link', { name: 'プロジェクト' })).toHaveAttribute('aria-current', 'page');
    await user.click(screen.getByRole('button', { name: 'サイドバーを折りたたむ' }));

    expect(screen.getByRole('button', { name: 'サイドバーを展開' })).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getByRole('link', { name: 'プロジェクト' })).toHaveAttribute('aria-current', 'page');
    expect(localStorage.getItem('sidebarCollapsed')).toBe('true');
    unmount();
    renderShell('/admin/projects/PRJ-1');
    expect(screen.getByRole('button', { name: 'サイドバーを展開' })).toBeInTheDocument();
  });

  it('updates the active item through route navigation and browser history', async () => {
    const user = userEvent.setup();
    renderShell('/admin/dashboard', <HistoryControls />);

    await user.click(screen.getByRole('link', { name: 'プロジェクト' }));
    expect(screen.getByRole('link', { name: 'プロジェクト' })).toHaveAttribute('aria-current', 'page');
    await user.click(screen.getByRole('button', { name: 'Back' }));
    expect(screen.getByRole('link', { name: 'ホーム' })).toHaveAttribute('aria-current', 'page');
    await user.click(screen.getByRole('button', { name: 'Forward' }));
    expect(screen.getByRole('link', { name: 'プロジェクト' })).toHaveAttribute('aria-current', 'page');
  });

  it.each([
    [1920, 'サイドバーを折りたたむ'],
    [1536, 'サイドバーを折りたたむ'],
    [1280, 'サイドバーを展開'],
    [1024, 'サイドバーを展開'],
  ])('chooses the desktop default at %i CSS pixels', (width, toggleLabel) => {
    vi.stubGlobal('matchMedia', vi.fn().mockImplementation((query: string) => ({
      matches: query === '(min-width: 1440px)' ? width >= 1440 : width >= 1024,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })));
    renderShell('/admin/dashboard');
    expect(screen.getByRole('button', { name: toggleLabel })).toBeInTheDocument();
  });

  it.each([
    ['master', '/master/dashboard', 'マスターのメインナビゲーション'],
    ['user', '/sites/SITE-1/dashboard', 'ユーザーのメインナビゲーション'],
  ])('selects the %s sidebar', (role, pathname, label) => {
    authState.user.role = role;
    renderShell(pathname);
    expect(screen.getByRole('navigation', { name: label })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'ホーム' })).toHaveAttribute('aria-current', 'page');
  });

  it('opens a mobile navigation drawer with an accessible title', async () => {
    const user = userEvent.setup();
    renderShell('/admin/dashboard');

    await user.click(screen.getByRole('button', { name: 'メニューを開く' }));
    expect(screen.getByRole('dialog', { name: 'メニュー' })).toBeInTheDocument();

    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog', { name: 'メニュー' })).not.toBeInTheDocument();
  });

  it('closes the mobile drawer after choosing a destination', async () => {
    const user = userEvent.setup();
    renderShell('/admin/dashboard');

    await user.click(screen.getByRole('button', { name: 'メニューを開く' }));
    const dialog = screen.getByRole('dialog', { name: 'メニュー' });
    await user.click(within(dialog).getByRole('link', { name: 'プロジェクト' }));

    expect(screen.queryByRole('dialog', { name: 'メニュー' })).not.toBeInTheDocument();
  });
});
