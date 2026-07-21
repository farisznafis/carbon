import { fireEvent, render, screen } from '@testing-library/react';
import { getBatchHistory, type BatchHistoryItem } from '@/api/batch';
import { BatchHistoryTable } from './BatchHistoryTable';

vi.mock('@/api/batch', () => ({
  getBatchHistory: vi.fn(),
}));

const batchHistoryItem: BatchHistoryItem = {
  id: 7,
  status: 'SUCCESS',
  is_active: true,
  start_time: '2026-07-21T10:00:00.000Z',
  end_time: '2026-07-21T10:15:00.000Z',
  created_by: 'admin@example.com',
  created_at: '2026-07-21T09:00:00.000Z',
  updated_by: 'admin@example.com',
  updated_at: '2026-07-21T10:15:00.000Z',
  claimed_by: null,
  claimed_at: null,
};

describe('BatchHistoryTable', () => {
  it('renders preloaded data and exposes remaining fields in the detail dialog', async () => {
    render(
      <BatchHistoryTable
        initialItems={[batchHistoryItem]}
        initialPagination={{
          page: 1,
          limit: 10,
          totalItems: 1,
          totalPages: 1,
          nextPageToken: null,
          hasMore: false,
        }}
        initialLoading={false}
        initialLoadError={null}
      />,
    );

    expect(screen.getByText('成功')).toBeInTheDocument();
    expect(getBatchHistory).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: '詳細' }));

    expect(await screen.findByText('バッチ履歴の詳細')).toBeInTheDocument();
    expect(screen.getByText('有効状態')).toBeInTheDocument();
    expect(screen.getByText('作成日時')).toBeInTheDocument();
    expect(screen.getAllByText('admin@example.com')).toHaveLength(2);
  });
});
