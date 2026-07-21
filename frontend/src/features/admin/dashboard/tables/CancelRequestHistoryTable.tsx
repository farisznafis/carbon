import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  getMonitoringCancelRequests,
  type MonitoringCancelRequestListItem,
} from '@/api/monitoring';
import {
  AdminGridTable,
  type AdminGridTableColumn,
} from '@/components/data-display/AdminGridTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { Button } from '@/components/ui/button';
import { CancelRequestStatusBadge } from '@/features/admin/monitoring/CancelRequestStatusBadge';
import { formatCancelRequestDateTime } from '@/features/admin/monitoring/cancelRequestFormatters';
import { usePagination } from '@/hooks/usePagination';

const INITIAL_ITEMS_PER_PAGE = 10;
const cancelRequestGridColumns =
  'minmax(200px, 1.35fr) minmax(140px, 0.8fr) minmax(190px, 1fr) minmax(130px, 0.7fr) minmax(180px, 0.9fr)';

type CancelRequestHistoryTableProps = {
  initialRequests: MonitoringCancelRequestListItem[];
  initialLoading: boolean;
  initialLoadError: string | null;
  onViewRequest: (request: MonitoringCancelRequestListItem) => void;
};

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

export function CancelRequestHistoryTable({
  initialRequests,
  initialLoading,
  initialLoadError,
  onViewRequest,
}: CancelRequestHistoryTableProps) {
  const [requests, setRequests] = useState(initialRequests);
  const [isLoading, setIsLoading] = useState(initialLoading);
  const [loadError, setLoadError] = useState<string | null>(initialLoadError);

  useEffect(() => {
    setRequests(initialRequests);
  }, [initialRequests]);

  useEffect(() => {
    setIsLoading(initialLoading);
  }, [initialLoading]);

  useEffect(() => {
    setLoadError(initialLoadError);
  }, [initialLoadError]);

  const reloadRequests = useCallback(async () => {
    setIsLoading(true);
    setLoadError(null);

    try {
      setRequests(await getMonitoringCancelRequests());
    } catch (error) {
      setLoadError(
        getErrorMessage(error, '取消申請一覧の取得に失敗しました。'),
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  const {
    currentPage,
    totalPages,
    totalItems,
    paginatedItems,
    goToPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(requests, { itemsPerPage: INITIAL_ITEMS_PER_PAGE });

  const columns = useMemo<
    AdminGridTableColumn<MonitoringCancelRequestListItem>[]
  >(
    () => [
      {
        id: 'siteName',
        label: '地点名',
        sortable: true,
        sortValue: (request) => request.siteName,
        render: (request) => (
          <span className="block truncate text-body font-bold text-slate-800">
            {request.siteName || '-'}
          </span>
        ),
      },
      {
        id: 'requestedBy',
        label: '申請者',
        sortable: true,
        sortValue: (request) => request.requestedBy,
        render: (request) => (
          <span className="block truncate text-body text-foreground">
            {request.requestedBy || '-'}
          </span>
        ),
      },
      {
        id: 'requestedAt',
        label: '申請日時',
        sortable: true,
        sortValue: (request) => request.requestedAt,
        render: (request) => (
          <span className="block truncate text-body text-foreground">
            {formatCancelRequestDateTime(request.requestedAt)}
          </span>
        ),
      },
      {
        id: 'status',
        label: 'ステータス',
        sortable: true,
        sortValue: (request) => request.status,
        render: (request) => <CancelRequestStatusBadge status={request.status} />,
      },
      {
        id: 'actions',
        label: '操作',
        render: (request) => (
          <div
            onClick={(event) => event.stopPropagation()}
            onPointerDown={(event) => event.stopPropagation()}
          >
            <Button
              type="button"
              variant={
                request.status.toUpperCase() === 'PENDING'
                  ? 'default'
                  : 'outline'
              }
              onClick={() => onViewRequest(request)}
            >
              {request.status.toUpperCase() === 'PENDING'
                ? '取消内容を確認'
                : '詳細'}
            </Button>
          </div>
        ),
      },
    ],
    [onViewRequest],
  );

  if (isLoading) {
    return (
      <div className="rounded-lg border border-border py-12 text-center text-sm text-muted-foreground">
        取消申請一覧を読み込んでいます...
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="space-y-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center">
        <p className="text-sm text-destructive">{loadError}</p>
        <Button type="button" variant="outline" onClick={reloadRequests}>
          再試行
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <AdminGridTable
        rows={paginatedItems}
        columns={columns}
        gridTemplateColumns={cancelRequestGridColumns}
        minWidth={960}
        getRowKey={(request) => String(request.id)}
        emptyMessage="取消申請はありません。"
      />
      <PaginationControls
        currentPage={currentPage}
        totalPages={totalPages}
        totalItems={totalItems}
        itemsPerPage={itemsPerPage}
        onPageChange={goToPage}
        onItemsPerPageChange={setItemsPerPage}
      />
    </div>
  );
}
