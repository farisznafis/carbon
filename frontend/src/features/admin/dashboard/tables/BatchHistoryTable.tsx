import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  getBatchHistory,
  type BatchHistoryItem,
  type BatchHistoryPagination,
} from '@/api/batch';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

const INITIAL_PAGE = 1;
const INITIAL_ITEMS_PER_PAGE = 10;
const BATCH_STATUS_LABELS: Record<string, string> = {
  SUCCESS: '成功',
  FAILED: '失敗',
};
const SUCCESS_BATCH_STATUSES = ['SUCCESS', 'SUCCEEDED', 'COMPLETED'];
const FAILED_BATCH_STATUSES = ['FAILED', 'ERROR'];
const batchHistoryGridColumns =
  'minmax(72px, 0.45fr) minmax(130px, 0.8fr) minmax(110px, 0.65fr) minmax(175px, 1fr) minmax(175px, 1fr) minmax(160px, 0.9fr) minmax(175px, 1fr) minmax(160px, 0.9fr) minmax(175px, 1fr) minmax(160px, 0.9fr) minmax(175px, 1fr)';

const DEFAULT_PAGINATION: BatchHistoryPagination = {
  page: INITIAL_PAGE,
  limit: INITIAL_ITEMS_PER_PAGE,
  totalItems: 0,
  totalPages: 1,
  nextPageToken: null,
  hasMore: false,
};

function formatDateTime(value: string | null) {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ja-JP', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'UTC',
  }).format(date);
}

function BatchStatusBadge({ status }: { status: string }) {
  const normalizedStatus = status.toUpperCase();
  const isSuccess = SUCCESS_BATCH_STATUSES.includes(normalizedStatus);
  const isFailed = FAILED_BATCH_STATUSES.includes(normalizedStatus);

  return (
    <span
      className={cn(
        'inline-flex rounded-full border px-2.5 py-1 text-xs font-medium',
        isSuccess && 'border-emerald-200 bg-emerald-50 text-emerald-800',
        isFailed && 'border-red-200 bg-red-50 text-red-800',
        !isSuccess &&
          !isFailed &&
          'border-amber-200 bg-amber-50 text-amber-800',
      )}
    >
      {(BATCH_STATUS_LABELS[normalizedStatus] ?? status) || '-'}
    </span>
  );
}

function getErrorMessage(error: unknown) {
  return error instanceof Error && error.message
    ? error.message
    : 'バッチ履歴の取得に失敗しました。';
}

export function BatchHistoryTable() {
  const [items, setItems] = useState<BatchHistoryItem[]>([]);
  const [pagination, setPagination] =
    useState<BatchHistoryPagination>(DEFAULT_PAGINATION);
  const [page, setPage] = useState(INITIAL_PAGE);
  const [itemsPerPage, setItemsPerPage] = useState(INITIAL_ITEMS_PER_PAGE);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const loadHistory = useCallback(async () => {
    setIsLoading(true);
    setLoadError(null);

    try {
      const result = await getBatchHistory(page, itemsPerPage);
      setItems(result.items);
      setPagination(result.pagination);
    } catch (error) {
      setLoadError(getErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  }, [itemsPerPage, page]);

  useEffect(() => {
    void loadHistory();
  }, [loadHistory]);

  const columns = useMemo<DataGridTableColumn<BatchHistoryItem>[]>(
    () => [
      {
        id: 'id',
        label: 'ID',
        sortable: true,
        sortValue: (item) => item.id,
        render: (item) => <span className="text-body">{item.id}</span>,
      },
      {
        id: 'status',
        label: 'ステータス',
        sortable: true,
        sortValue: (item) => item.status,
        render: (item) => <BatchStatusBadge status={item.status} />,
      },
      {
        id: 'isActive',
        label: '有効状態',
        sortable: true,
        sortValue: (item) => (item.is_active ? 1 : 0),
        render: (item) => (
          <span className="text-body">{item.is_active ? '有効' : '無効'}</span>
        ),
      },
      {
        id: 'startTime',
        label: '開始日時',
        sortable: true,
        sortValue: (item) => item.start_time,
        render: (item) => (
          <span className="block truncate text-body">
            {formatDateTime(item.start_time)}
          </span>
        ),
      },
      {
        id: 'endTime',
        label: '終了日時',
        sortable: true,
        sortValue: (item) => item.end_time,
        render: (item) => (
          <span className="block truncate text-body">
            {formatDateTime(item.end_time)}
          </span>
        ),
      },
      {
        id: 'createdBy',
        label: '作成者',
        sortable: true,
        sortValue: (item) => item.created_by,
        render: (item) => (
          <span className="block truncate text-body">{item.created_by || '-'}</span>
        ),
      },
      {
        id: 'createdAt',
        label: '作成日時',
        sortable: true,
        sortValue: (item) => item.created_at,
        render: (item) => (
          <span className="block truncate text-body">
            {formatDateTime(item.created_at)}
          </span>
        ),
      },
      {
        id: 'updatedBy',
        label: '更新者',
        sortable: true,
        sortValue: (item) => item.updated_by,
        render: (item) => (
          <span className="block truncate text-body">{item.updated_by || '-'}</span>
        ),
      },
      {
        id: 'updatedAt',
        label: '更新日時',
        sortable: true,
        sortValue: (item) => item.updated_at,
        render: (item) => (
          <span className="block truncate text-body">
            {formatDateTime(item.updated_at)}
          </span>
        ),
      },
      {
        id: 'claimedBy',
        label: '処理担当者',
        sortable: true,
        sortValue: (item) => item.claimed_by,
        render: (item) => (
          <span className="block truncate text-body">{item.claimed_by || '-'}</span>
        ),
      },
      {
        id: 'claimedAt',
        label: '処理取得日時',
        sortable: true,
        sortValue: (item) => item.claimed_at,
        render: (item) => (
          <span className="block truncate text-body">
            {formatDateTime(item.claimed_at)}
          </span>
        ),
      },
    ],
    [],
  );

  if (isLoading) {
    return (
      <div className="rounded-lg border border-border py-12 text-center text-sm text-muted-foreground">
        バッチ履歴を読み込んでいます...
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="space-y-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center">
        <p role="alert" className="text-sm text-destructive">
          {loadError}
        </p>
        <Button type="button" variant="outline" onClick={() => void loadHistory()}>
          再試行
        </Button>
      </div>
    );
  }

  const handleItemsPerPageChange = (nextItemsPerPage: number) => {
    setItemsPerPage(nextItemsPerPage);
    setPage(INITIAL_PAGE);
  };

  return (
    <div className="space-y-4">
      <DataGridTable
        rows={items}
        columns={columns}
        gridTemplateColumns={batchHistoryGridColumns}
        minWidth={1850}
        getRowKey={(item) => String(item.id)}
        emptyMessage="バッチ履歴はありません。"
      />
      <PaginationControls
        currentPage={pagination.page}
        totalPages={pagination.totalPages}
        totalItems={pagination.totalItems}
        itemsPerPage={pagination.limit}
        rowsPerPageOptions={[10, 20, 50, 100]}
        onPageChange={setPage}
        onItemsPerPageChange={handleItemsPerPageChange}
      />
    </div>
  );
}
