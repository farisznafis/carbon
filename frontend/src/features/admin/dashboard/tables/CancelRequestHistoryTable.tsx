import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  getMonitoringCancelRequests,
  type MonitoringCancelRequestListItem,
} from '@/api/monitoring';
import {
  ATTENTION_ROW_CLASS_NAME,
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { CancelRequestStatusBadge } from '@/features/admin/monitoring/CancelRequestStatusBadge';
import { formatCancelRequestDateTime } from '@/features/admin/monitoring/cancelRequestFormatters';
import { usePagination } from '@/hooks/usePagination';
import { SkeletonTable } from '@/components/feedback/loading';
import { ActionButton } from '@/components/actions';

const INITIAL_ITEMS_PER_PAGE = 10;
const cancelRequestGridColumns =
  'minmax(200px, 1.35fr) minmax(140px, 0.8fr) minmax(190px, 1fr) minmax(130px, 0.7fr)';

/** Pending cancel requests wait on the admin, so they are highlighted and labelled for review. */
function isPendingRequest(request: MonitoringCancelRequestListItem) {
  return request.status.toUpperCase() === 'PENDING';
}

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
    DataGridTableColumn<MonitoringCancelRequestListItem>[]
  >(
    () => [
      {
        id: 'siteName',
        label: '地点名',
        sortable: true,
        sortValue: (request) => request.siteName,
        render: (request) => (
          <span className="block truncate text-body font-bold text-foreground">
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
    ],
    [],
  );

  if (isLoading) {
    return <SkeletonTable columns={4}rows={6} gridTemplateColumns={cancelRequestGridColumns} minWidth={980} />;
  }

  if (loadError) {
    return (
      <div className="space-y-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center">
        <p className="text-sm text-destructive">{loadError}</p>
        <ActionButton kind="retry" onClick={reloadRequests} />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <DataGridTable
        rows={paginatedItems}
        columns={columns}
        gridTemplateColumns={cancelRequestGridColumns}
        minWidth={960}
        getRowKey={(request) => String(request.id)}
        emptyMessage="取消申請はありません。"
        onRowClick={onViewRequest}
        getRowActionLabel={(request) =>
          isPendingRequest(request) ? '取消内容を確認' : '詳細'
        }
        getRowClassName={(request) =>
          isPendingRequest(request) ? ATTENTION_ROW_CLASS_NAME : undefined
        }
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
