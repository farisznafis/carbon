import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { PageBreadcrumb, type PageBreadcrumbItem } from './PageBreadcrumb';
import { BreadcrumbProvider } from './BreadcrumbProvider';
import { usePageBreadcrumb, usePageBreadcrumbItems } from './pageBreadcrumbContext';

function Topbar() {
  return <PageBreadcrumb items={usePageBreadcrumbItems()} />;
}

function Page({ items }: { items: PageBreadcrumbItem[] }) {
  usePageBreadcrumb(items);
  return null;
}

function renderShell(page: React.ReactNode) {
  return render(
    <MemoryRouter>
      <BreadcrumbProvider>
        <Topbar />
        {page}
      </BreadcrumbProvider>
    </MemoryRouter>,
  );
}

describe('usePageBreadcrumb', () => {
  it('publishes the page trail to the topbar and follows label changes', () => {
    const { rerender } = renderShell(
      <Page items={[{ label: '運用者管理', to: '/admin/operators' }, { label: '読み込み中' }]} />,
    );

    expect(screen.getByRole('link', { name: '運用者管理' })).toHaveAttribute(
      'href',
      '/admin/operators',
    );
    expect(screen.getByText('読み込み中')).toHaveAttribute('aria-current', 'page');

    rerender(
      <MemoryRouter>
        <BreadcrumbProvider>
          <Topbar />
          <Page items={[{ label: '運用者管理', to: '/admin/operators' }, { label: '佐藤 花' }]} />
        </BreadcrumbProvider>
      </MemoryRouter>,
    );

    expect(screen.getByText('佐藤 花')).toBeInTheDocument();
    expect(screen.queryByText('読み込み中')).not.toBeInTheDocument();
  });

  it('clears the trail when the page unmounts', () => {
    const { rerender } = renderShell(<Page items={[{ label: '一覧', to: '/list' }, { label: '詳細' }]} />);
    expect(screen.getByRole('navigation', { name: 'breadcrumb' })).toBeInTheDocument();

    rerender(
      <MemoryRouter>
        <BreadcrumbProvider>
          <Topbar />
        </BreadcrumbProvider>
      </MemoryRouter>,
    );

    expect(screen.queryByText('詳細')).not.toBeInTheDocument();
  });

  it('calls the latest onClick even when the trail itself is unchanged', async () => {
    const first = vi.fn();
    const latest = vi.fn();
    const items = (onClick: () => void) => [{ label: '企業一覧', onClick }, { label: '株式会社A' }];

    const { rerender } = renderShell(<Page items={items(first)} />);
    rerender(
      <MemoryRouter>
        <BreadcrumbProvider>
          <Topbar />
          <Page items={items(latest)} />
        </BreadcrumbProvider>
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByRole('button', { name: '企業一覧' }));
    expect(latest).toHaveBeenCalledOnce();
    expect(first).not.toHaveBeenCalled();
  });
});
