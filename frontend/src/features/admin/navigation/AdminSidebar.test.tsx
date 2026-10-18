import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AdminSidebar } from './AdminSidebar';

/** Renders the admin sidebar at a requested route for active-state checks. */
function renderAdminSidebar(pathname: string, collapsed = false) {
  return render(
    <MemoryRouter
      initialEntries={[pathname]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <AdminSidebar collapsed={collapsed} />
    </MemoryRouter>,
  );
}

describe('AdminSidebar', () => {
  it('marks the nested route group as active', () => {
    renderAdminSidebar('/admin/operators/OPERATOR-1');

    expect(screen.getByRole('link', { name: '運営管理' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('keeps inactive navigation items unselected', () => {
    renderAdminSidebar('/admin/operators');

    expect(screen.getByRole('link', { name: 'プロジェクト' })).not.toHaveAttribute(
      'aria-current',
    );
  });

  it('keeps a readable name when collapsed', () => {
    renderAdminSidebar('/admin/projects/PRJ-1/edit', true);

    expect(screen.getByRole('link', { name: 'プロジェクト' })).toHaveAttribute('aria-current', 'page');
  });
});
