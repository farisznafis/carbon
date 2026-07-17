import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  getMonitoringCancelRequests,
  type MonitoringCancelRequestListItem,
} from '@/api/monitoring';
import {
  type Column,
  ProTable,
} from '@/components/data-display/ProTable/ProTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { Button } from '@/components/ui/button';
import { CancelRequestStatusBadge } from '@/features/admin/monitoring/CancelRequestStatusBadge';
import { formatCancelRequestDateTime } from '@/features/admin/monitoring/cancelRequestFormatters';
import { usePagination } from '@/hooks/usePagination';

const INITIAL_ITEMS_PER_PAGE = 10;

type CancelRequestTableRow = Omit<MonitoringCancelRequestListItem, 'id'> & {
  id: string;
  cancelRequestId: number;
};

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
  const [requests, setRequests] = useState<CancelRequestTableRow[]>(
    initialRequests.map((request) => ({
      ...request,
      id: String(request.id),
      cancelRequestId: request.id,
    })),
  );
  const [isLoading, setIsLoading] = useState(initialLoading);
  const [loadError, setLoadError] = useState<string | null>(initialLoadError);
  useEffect(() => {
    setRequests(
      initialRequests.map((request) => ({
        ...request,
        id: String(request.id),
        cancelRequestId: request.id,
      })),
    );
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
      const responseItems = await getMonitoringCancelRequests();
      setRequests(
        responseItems.map((request) => ({
          ...request,
          id: String(request.id),
          cancelRequestId: request.id,
        })),
      );
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

  const columns = useMemo<Column<CancelRequestTableRow>[]>(
    () => [
      {
        key: 'siteName',
        label: '地点名',
        sortable: true,
        truncate: true,
        maxWidth: 'max-w-[260px]',
        className: 'w-[240px]',
        headerClassName: 'w-[240px] whitespace-nowrap',
      },
      {
        key: 'requestedBy',
        label: '申請者',
        sortable: true,
        truncate: true,
        maxWidth: 'max-w-[180px]',
        className: 'w-[140px]',
        headerClassName: 'w-[140px] whitespace-nowrap',
      },
      {
        key: 'requestedAt',
        label: '申請日時',
        sortable: true,
        className: 'w-[180px] whitespace-nowrap',
        headerClassName: 'w-[180px] whitespace-nowrap',
        render: (value) => formatCancelRequestDateTime(String(value)),
      },
      {
        key: 'status',
        label: 'ステータス',
        sortable: true,
        className: 'w-[130px] whitespace-nowrap',
        headerClassName: 'w-[130px] whitespace-nowrap',
        render: (value) => <CancelRequestStatusBadge status={String(value)} />,
      },
      {
        key: 'id',
        id: 'actions',
        label: '操作',
        className: 'w-[180px] whitespace-nowrap text-left',
        headerClassName: 'w-[180px] whitespace-nowrap text-left',
        render: (_, request) => (
          <Button
            type="button"
            variant={
              request.status.toUpperCase() === 'PENDING' ? 'default' : 'outline'
            }
            onClick={() =>
              onViewRequest({
                ...request,
                id: request.cancelRequestId,
              })
            }
          >
            {request.status.toUpperCase() === 'PENDING'
              ? '取消内容を確認'
              : '詳細'}
          </Button>
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
        <div className="w-full overflow-x-auto pb-2">
          <ProTable
            data={paginatedItems}
            columns={columns}
            clickable={false}
            height="max-h-[520px]"
            containerClassName="min-w-[900px] overflow-x-hidden"
            tableClassName="table-fixed"
            emptyState={
              <p className="text-muted-foreground">取消申請はありません。</p>
            }
          />
        </div>
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
