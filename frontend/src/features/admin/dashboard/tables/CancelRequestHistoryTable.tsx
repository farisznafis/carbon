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
import { cn } from '@/lib/utils';

const INITIAL_ITEMS_PER_PAGE = 10;
const CANCEL_REASON_PREVIEW_CHARACTER_LIMIT = 24;

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
  const [expandedReasonIds, setExpandedReasonIds] = useState<Set<string>>(
    () => new Set(),
  );

  const toggleReason = useCallback((requestId: string) => {
    setExpandedReasonIds((currentIds) => {
      const nextIds = new Set(currentIds);

      if (nextIds.has(requestId)) {
        nextIds.delete(requestId);
      } else {
        nextIds.add(requestId);
      }

      return nextIds;
    });
  }, []);

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
        key: 'id',
        label: '申請ID',
        sortable: true,
        className: 'w-[60px] whitespace-nowrap',
        headerClassName: 'w-[60px] whitespace-nowrap',
      },
      {
        key: 'siteName',
        label: '拠点名',
        sortable: true,
        truncate: true,
        maxWidth: 'max-w-[180px]',
        className: 'w-[160px]',
        headerClassName: 'w-[160px] whitespace-nowrap',
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
        key: 'reason',
        label: '取消理由',
        className: 'w-[320px] align-top',
        headerClassName: 'w-[320px] whitespace-nowrap',
        render: (value, request) => {
          const reason = value ? String(value) : '-';
          const isExpandable =
            reason.length > CANCEL_REASON_PREVIEW_CHARACTER_LIMIT;
          const isExpanded = expandedReasonIds.has(request.id);

          if (!isExpandable) {
            return (
              <span className="block whitespace-normal break-words">
                {reason}
              </span>
            );
          }

          return (
            <button
              type="button"
              className="block w-full text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              aria-expanded={isExpanded}
              onClick={() => toggleReason(request.id)}
            >
              <span
                className={cn(
                  'block break-words',
                  isExpanded ? 'whitespace-normal' : 'truncate',
                )}
              >
                {reason}
              </span>
              <span className="mt-1 inline-block text-xs font-medium text-primary underline-offset-4 hover:underline">
                {isExpanded ? '閉じる' : '全文を表示'}
              </span>
            </button>
          );
        },
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
        key: 'decidedAt',
        label: '決定日時',
        sortable: true,
        className: 'w-[180px] whitespace-nowrap',
        headerClassName: 'w-[180px] whitespace-nowrap',
        render: (value) =>
          formatCancelRequestDateTime(value ? String(value) : null),
      },
      {
        key: 'decidedBy',
        label: '決定者',
        truncate: true,
        maxWidth: 'max-w-[180px]',
        className: 'w-[140px]',
        headerClassName: 'w-[140px] whitespace-nowrap',
        render: (value) => (value ? String(value) : '-'),
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
    [expandedReasonIds, onViewRequest, toggleReason],
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
            containerClassName="min-w-[1570px] overflow-x-hidden"
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
