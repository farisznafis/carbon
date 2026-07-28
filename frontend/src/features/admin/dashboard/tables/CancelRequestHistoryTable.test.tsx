import { fireEvent, render, screen } from '@testing-library/react';
import type { MonitoringCancelRequestListItem } from '@/api/monitoring';
import { CancelRequestHistoryTable } from './CancelRequestHistoryTable';

const cancelRequest: MonitoringCancelRequestListItem = {
  id: 42,
  siteName: '東京サイト',
  requestedBy: '申請者',
  requestedAt: '2026-07-21T10:00:00.000Z',
  reason: '取消理由',
  recordPk: 'record-pk',
  status: 'PENDING',
  decidedAt: null,
  decidedBy: null,
};

describe('CancelRequestHistoryTable', () => {
  it('keeps the numeric request ID when opening a request', () => {
    const onViewRequest = vi.fn();

    render(
      <CancelRequestHistoryTable
        initialRequests={[cancelRequest]}
        initialLoading={false}
        initialLoadError={null}
        onViewRequest={onViewRequest}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: '取消内容を確認' }));

    expect(onViewRequest).toHaveBeenCalledWith(cancelRequest);
    expect(onViewRequest.mock.calls[0][0].id).toBe(42);
  });
});
