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

  it('opens a row on click and through the trailing › button for keyboard users', () => {
    const onRowClick = vi.fn();
    const row = { id: 'row-1', name: 'First row' };

    render(
      <DataGridTable
        rows={[row]}
        columns={columns}
        gridTemplateColumns="1fr"
        getRowKey={(item) => item.id}
        emptyMessage="No rows"
        onRowClick={onRowClick}
      />,
    );

    fireEvent.click(screen.getByText('First row'));
    expect(onRowClick).toHaveBeenCalledWith(row);

    // The › button is a real button with an accessible name; the row itself is not role="button".
    fireEvent.click(screen.getByRole('button', { name: '詳細' }));
    expect(onRowClick).toHaveBeenCalledTimes(2);
    expect(screen.getAllByRole('button')).toHaveLength(1);
    expect(screen.getByText('First row').closest('div[style]')).toHaveStyle({
      gridTemplateColumns: '1fr 32px',
    });
  });

  it('names the trailing button per row and keeps other row controls independent', () => {
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
              <RowActionButton onClick={() => onAction(row)}>更新</RowActionButton>
            ),
          },
        ]}
        gridTemplateColumns="1fr 80px"
        getRowKey={(row) => row.id}
        emptyMessage="No rows"
        onRowClick={onRowClick}
        getRowActionLabel={(row) => `${row.name}を確認`}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: '更新' }));
    expect(onAction).toHaveBeenCalledTimes(1);
    expect(onRowClick).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'First rowを確認' }));
    expect(onRowClick).toHaveBeenCalledTimes(1);
  });

  it('does not add the trailing column to tables without a row action', () => {
    render(
      <DataGridTable
        rows={[{ id: 'row-1', name: 'First row' }]}
        columns={columns}
        gridTemplateColumns="1fr"
        getRowKey={(row) => row.id}
        emptyMessage="No rows"
      />,
    );

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
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
