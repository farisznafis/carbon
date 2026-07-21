import { render, screen } from '@testing-library/react';
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
  it('renders API data with the shared grid table', async () => {
    vi.mocked(getBatchHistory).mockResolvedValue({
      items: [batchHistoryItem],
      pagination: {
        page: 1,
        limit: 10,
        totalItems: 1,
        totalPages: 1,
        nextPageToken: null,
        hasMore: false,
      },
    });

    render(<BatchHistoryTable />);

    expect(await screen.findByText('成功')).toBeInTheDocument();
    expect(screen.getAllByText('admin@example.com')).toHaveLength(2);
    expect(getBatchHistory).toHaveBeenCalledWith(1, 10);
  });
});
