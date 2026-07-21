import { render, screen } from '@testing-library/react';
import { AdminGridTable, type AdminGridTableColumn } from './AdminGridTable';

type TestRow = {
  id: string;
  name: string;
};

const columns: AdminGridTableColumn<TestRow>[] = [
  {
    id: 'name',
    label: 'Name',
    render: (row) => row.name,
  },
];

describe('AdminGridTable', () => {
  it('uses the shared 60px minimum data-row height', () => {
    render(
      <AdminGridTable
        rows={[{ id: 'row-1', name: 'First row' }]}
        columns={columns}
        gridTemplateColumns="1fr"
        getRowKey={(row) => row.id}
        emptyMessage="No rows"
      />,
    );

    expect(screen.getByText('First row').closest('div[style]')).toHaveClass(
      'min-h-[60px]',
    );
  });
});
