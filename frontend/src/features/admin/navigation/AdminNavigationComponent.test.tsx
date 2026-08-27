import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AdminNavigation } from './AdminNavigation';

/** Renders the admin navigation at a requested route for active-state checks. */
function renderAdminNavigation(pathname: string) {
  return render(
    <MemoryRouter
      initialEntries={[pathname]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <AdminNavigation />
    </MemoryRouter>,
  );
}

describe('AdminNavigation', () => {
  it('marks the nested route group as active', () => {
    renderAdminNavigation('/admin/operators/OPERATOR-1');

    expect(screen.getByRole('link', { name: '運営管理' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('keeps inactive navigation items unselected', () => {
    renderAdminNavigation('/admin/operators');

    expect(screen.getByRole('link', { name: 'プロジェクト' })).not.toHaveAttribute(
      'aria-current',
    );
  });
});
