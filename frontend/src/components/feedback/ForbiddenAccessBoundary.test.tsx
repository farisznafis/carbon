import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { reportPageForbidden } from '@/api/_shared/forbiddenAccess';
import { ForbiddenAccessBoundary } from './ForbiddenAccessBoundary';

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { id: '1', role: 'admin', siteId: null },
  }),
}));

describe('ForbiddenAccessBoundary', () => {
  it('returns home and ignores a late 403 from the previous route', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/admin/projects']}>
        <ForbiddenAccessBoundary>
          <Routes>
            <Route path="/admin/projects" element={<div>プロジェクト管理</div>} />
            <Route path="/admin/dashboard" element={<div>管理ホーム</div>} />
          </Routes>
        </ForbiddenAccessBoundary>
      </MemoryRouter>,
    );

    expect(screen.getByText('プロジェクト管理')).toBeInTheDocument();

    act(() => {
      reportPageForbidden({
        requestPath: '/project',
        routeKey: '/admin/projects',
      });
    });

    expect(
      screen.getByRole('heading', { name: '権限がありません' }),
    ).toBeInTheDocument();
    expect(
      screen.getByText('このページを表示する権限がありません。', {
        exact: false,
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'ホームに戻る' }),
    ).toBeInTheDocument();
    expect(screen.queryByText('プロジェクト管理')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'ホームに戻る' }));
    expect(await screen.findByText('管理ホーム')).toBeInTheDocument();

    act(() => {
      reportPageForbidden({
        requestPath: '/project',
        routeKey: '/admin/projects',
      });
    });
    expect(screen.getByText('管理ホーム')).toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { name: '権限がありません' }),
    ).not.toBeInTheDocument();
  });

  it('keeps the admin home visible when a supporting request is forbidden', () => {
    render(
      <MemoryRouter initialEntries={['/admin/dashboard']}>
        <ForbiddenAccessBoundary>
          <div>管理ホーム</div>
        </ForbiddenAccessBoundary>
      </MemoryRouter>,
    );

    act(() => {
      reportPageForbidden({
        requestPath: '/project',
        routeKey: '/admin/dashboard',
      });
    });

    expect(screen.getByText('管理ホーム')).toBeInTheDocument();
  });

  it('ignores a forbidden request owned by another permission tab', () => {
    render(
      <MemoryRouter initialEntries={['/admin/companies']}>
        <ForbiddenAccessBoundary>
          <div>企業管理</div>
        </ForbiddenAccessBoundary>
      </MemoryRouter>,
    );

    act(() => {
      reportPageForbidden({
        requestPath: '/project',
        routeKey: '/admin/companies',
      });
    });

    expect(screen.getByText('企業管理')).toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { name: '権限がありません' }),
    ).not.toBeInTheDocument();
  });

  it('shows the warning for a forbidden request owned by the active tab', () => {
    render(
      <MemoryRouter initialEntries={['/admin/companies']}>
        <ForbiddenAccessBoundary>
          <div>企業管理</div>
        </ForbiddenAccessBoundary>
      </MemoryRouter>,
    );

    act(() => {
      reportPageForbidden({
        requestPath: '/admin/companies?status=ACTIVE',
        routeKey: '/admin/companies',
      });
    });

    expect(
      screen.getByRole('heading', { name: '権限がありません' }),
    ).toBeInTheDocument();
  });
});
