import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  getBatchHistory,
  type BatchHistoryItem,
  type BatchHistoryPagination,
} from '@/api/batch';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import {
  ProTable,
  type Column,
} from '@/components/data-display/ProTable/ProTable';
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

const DEFAULT_PAGINATION: BatchHistoryPagination = {
  page: INITIAL_PAGE,
  limit: INITIAL_ITEMS_PER_PAGE,
  totalItems: 0,
  totalPages: 1,
  nextPageToken: null,
  hasMore: false,
};

type BatchHistoryTableRow = Omit<BatchHistoryItem, 'id'> & {
  id: string;
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
  const [items, setItems] = useState<BatchHistoryTableRow[]>([]);
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
      setItems(
        result.items.map((item) => ({
          ...item,
          id: String(item.id),
        })),
      );
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

  const columns = useMemo<Column<BatchHistoryTableRow>[]>(
    () => [
      {
        key: 'id',
        label: 'ID',
        sortable: true,
        className: 'w-[80px] whitespace-nowrap',
        headerClassName: 'w-[80px] whitespace-nowrap',
      },
      {
        key: 'status',
        label: 'ステータス',
        sortable: true,
        className: 'w-[140px] whitespace-nowrap',
        headerClassName: 'w-[140px] whitespace-nowrap',
        render: (value) => <BatchStatusBadge status={String(value)} />,
      },
      {
        key: 'is_active',
        label: '有効状態',
        sortable: true,
        className: 'w-[110px] whitespace-nowrap',
        headerClassName: 'w-[110px] whitespace-nowrap',
        render: (value) => (value ? '有効' : '無効'),
      },
      {
        key: 'start_time',
        label: '開始日時',
        sortable: true,
        className: 'w-[180px] whitespace-nowrap',
        headerClassName: 'w-[180px] whitespace-nowrap',
        render: (value) => formatDateTime(String(value)),
      },
      {
        key: 'end_time',
        label: '終了日時',
        sortable: true,
        className: 'w-[180px] whitespace-nowrap',
        headerClassName: 'w-[180px] whitespace-nowrap',
        render: (value) => formatDateTime(value ? String(value) : null),
      },
      {
        key: 'created_by',
        label: '作成者',
        sortable: true,
        truncate: true,
        maxWidth: 'max-w-[180px]',
        className: 'w-[160px]',
        headerClassName: 'w-[160px] whitespace-nowrap',
      },
      {
        key: 'created_at',
        label: '作成日時',
        sortable: true,
        className: 'w-[180px] whitespace-nowrap',
        headerClassName: 'w-[180px] whitespace-nowrap',
        render: (value) => formatDateTime(String(value)),
      },
      {
        key: 'updated_by',
        label: '更新者',
        sortable: true,
        truncate: true,
        maxWidth: 'max-w-[180px]',
        className: 'w-[160px]',
        headerClassName: 'w-[160px] whitespace-nowrap',
      },
      {
        key: 'updated_at',
        label: '更新日時',
        sortable: true,
        className: 'w-[180px] whitespace-nowrap',
        headerClassName: 'w-[180px] whitespace-nowrap',
        render: (value) => formatDateTime(String(value)),
      },
      {
        key: 'claimed_by',
        label: '処理担当者',
        sortable: true,
        truncate: true,
        maxWidth: 'max-w-[180px]',
        className: 'w-[160px]',
        headerClassName: 'w-[160px] whitespace-nowrap',
        render: (value) => (value ? String(value) : '-'),
      },
      {
        key: 'claimed_at',
        label: '処理取得日時',
        sortable: true,
        className: 'w-[180px] whitespace-nowrap',
        headerClassName: 'w-[180px] whitespace-nowrap',
        render: (value) => formatDateTime(value ? String(value) : null),
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
      <div className="w-full overflow-x-auto pb-2">
        <ProTable
          data={items}
          columns={columns}
          clickable={false}
          height="max-h-[520px]"
          containerClassName="min-w-[1850px] overflow-x-hidden"
          tableClassName="table-fixed"
          emptyState={
            <p className="text-muted-foreground">バッチ履歴はありません。</p>
          }
        />
      </div>
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
