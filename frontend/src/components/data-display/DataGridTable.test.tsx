import { render, screen } from '@testing-library/react';
import { DataGridTable, type DataGridTableColumn } from './DataGridTable';

type TestRow = {
  id: string;
  name: string;
};

const columns: DataGridTableColumn<TestRow>[] = [
  {
    id: 'name',
    label: 'Name',
    render: (row) => row.name,
  },
];

describe('DataGridTable', () => {
  it('uses the shared 60px minimum data-row height', () => {
    render(
      <DataGridTable
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
