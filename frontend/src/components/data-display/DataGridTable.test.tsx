import { fireEvent, render, screen } from '@testing-library/react';
import { DataGridTable, type DataGridTableColumn } from './DataGridTable';
import { RowActionButton } from './RowActionButton';

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

  it('opens a row on click without making the row itself a focusable button', () => {
    const onRowClick = vi.fn();
    const onAction = vi.fn();

    render(
      <DataGridTable
        rows={[{ id: 'row-1', name: 'First row' }]}
        columns={[
          ...columns,
          {
            id: 'actions',
            label: '操作',
            render: (row) => (
              <RowActionButton onClick={() => onAction(row)} />
            ),
          },
        ]}
        gridTemplateColumns="1fr 80px"
        getRowKey={(row) => row.id}
        emptyMessage="No rows"
        onRowClick={onRowClick}
      />,
    );

    fireEvent.click(screen.getByText('First row'));
    expect(onRowClick).toHaveBeenCalledWith({ id: 'row-1', name: 'First row' });

    fireEvent.click(screen.getByRole('button', { name: '詳細' }));
    expect(onAction).toHaveBeenCalledTimes(1);
    expect(onRowClick).toHaveBeenCalledTimes(1);

    // The 詳細 button is the only button: the row is not role="button".
    expect(screen.getAllByRole('button')).toHaveLength(1);
  });

  it('ignores clicks inside areas marked data-row-click-ignore', () => {
    const onRowClick = vi.fn();

    render(
      <DataGridTable
        rows={[{ id: 'row-1', name: 'First row' }]}
        columns={[
          {
            id: 'name',
            label: 'Name',
            render: (row) => <span data-row-click-ignore>{row.name}</span>,
          },
        ]}
        gridTemplateColumns="1fr"
        getRowKey={(row) => row.id}
        emptyMessage="No rows"
        onRowClick={onRowClick}
      />,
    );

    fireEvent.click(screen.getByText('First row'));
    expect(onRowClick).not.toHaveBeenCalled();
  });
});
