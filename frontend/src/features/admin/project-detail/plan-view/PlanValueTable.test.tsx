import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { PlanValueTable } from './PlanValueTable';

vi.mock('@/api/admin/projects', () => ({
  getProjectPlanFileDownloadUrl: vi.fn(),
}));

const table: FieldConfig = {
  name: 'page.rows',
  label: '排出活動',
  type: 'table',
  columns: [
    { name: 'symbol', label: '記号', type: 'text' },
    {
      name: 'hasDetail',
      label: '詳細有無',
      type: 'select',
      options: [
        { label: '有', value: true },
        { label: '無', value: false },
      ],
    },
    { name: 'detail', label: '詳細', type: 'text' },
  ],
};

function renderTable(rows: unknown) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <PlanValueTable
        projectId="PROJ1"
        field={table}
        rows={rows}
        conditionalFields={[{ field: 'rows[].detail', when: 'rows[].hasDetail', is: true }]}
      />
    </QueryClientProvider>,
  );
}

describe('PlanValueTable', () => {
  it('shows one row per stored item, with option labels', () => {
    renderTable([
      { symbol: 'EM1', hasDetail: true, detail: '電力' },
      { symbol: 'EM2', hasDetail: false, detail: '残っていた値' },
    ]);

    const bodyRows = screen.getAllByRole('row').slice(1);
    expect(bodyRows).toHaveLength(2);
    expect(within(bodyRows[0]).getByText('EM1')).toBeInTheDocument();
    expect(within(bodyRows[0]).getByText('有')).toBeInTheDocument();
    expect(within(bodyRows[0]).getByText('電力')).toBeInTheDocument();
  });

  it('blanks a cell whose 有/無 rule does not apply to that row', () => {
    renderTable([{ symbol: 'EM2', hasDetail: false, detail: '残っていた値' }]);

    expect(screen.queryByText('残っていた値')).not.toBeInTheDocument();
  });

  it('says so when the table has no rows', () => {
    renderTable([]);

    expect(screen.getByText('データなし')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });
});
