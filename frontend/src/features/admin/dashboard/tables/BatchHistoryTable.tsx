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
import { BatchHistoryDetailDialog } from './BatchHistoryDetailDialog';
import { SkeletonTable } from '@/components/feedback/loading';
import { BatchHistoryStatusBadge } from './BatchHistoryStatusBadge';
import { formatBatchHistoryDateTime } from './batchHistoryFormatters';
import { ActionButton } from '@/components/actions';

const INITIAL_PAGE = 1;
const INITIAL_ITEMS_PER_PAGE = 10;
const batchHistoryGridColumns =
  'minmax(72px, 0.45fr) minmax(130px, 0.8fr) minmax(175px, 1fr) minmax(175px, 1fr) minmax(160px, 0.9fr) minmax(175px, 1fr)';

type BatchHistoryTableProps = {
  initialItems: BatchHistoryItem[];
  initialPagination: BatchHistoryPagination;
  initialLoading: boolean;
  initialLoadError: string | null;
};

function getErrorMessage(error: unknown) {
  return error instanceof Error && error.message
    ? error.message
    : 'バッチ履歴の取得に失敗しました。';
}

export function BatchHistoryTable({
  initialItems,
  initialPagination,
  initialLoading,
  initialLoadError,
}: Readonly<BatchHistoryTableProps>) {
  const [items, setItems] = useState(initialItems);
  const [pagination, setPagination] = useState(initialPagination);
  const [page, setPage] = useState(initialPagination.page);
  const [itemsPerPage, setItemsPerPage] = useState(initialPagination.limit);
  const [isLoading, setIsLoading] = useState(initialLoading);
  const [loadError, setLoadError] = useState<string | null>(initialLoadError);
  const [selectedItem, setSelectedItem] = useState<BatchHistoryItem | null>(null);

  useEffect(() => {
    setItems(initialItems);
    setPagination(initialPagination);
    setIsLoading(initialLoading);
    setLoadError(initialLoadError);
  }, [initialItems, initialLoadError, initialLoading, initialPagination]);

  const loadHistory = useCallback(async (
    nextPage = page,
    nextItemsPerPage = itemsPerPage,
  ) => {
    setIsLoading(true);
    setLoadError(null);

    try {
      const result = await getBatchHistory(nextPage, nextItemsPerPage);
      setItems(result.items);
      setPagination(result.pagination);
    } catch (error) {
      setLoadError(getErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  }, [itemsPerPage, page]);

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
        render: (item) => <BatchHistoryStatusBadge status={item.status} />,
      },
      {
        id: 'startTime',
        label: '開始日時',
        sortable: true,
        sortValue: (item) => item.start_time,
        render: (item) => (
          <span className="block truncate text-body">
            {formatBatchHistoryDateTime(item.start_time)}
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
            {formatBatchHistoryDateTime(item.end_time)}
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
            {formatBatchHistoryDateTime(item.claimed_at)}
          </span>
        ),
      },
    ],
    [],
  );

  if (isLoading) {
    return <SkeletonTable columns={6}rows={6} gridTemplateColumns={batchHistoryGridColumns} minWidth={1120} />;
  }

  if (loadError) {
    return (
      <div className="space-y-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center">
        <p role="alert" className="text-sm text-destructive">
          {loadError}
        </p>
        <ActionButton kind="retry" onClick={() => void loadHistory()} />
      </div>
    );
  }

  const handleItemsPerPageChange = (nextItemsPerPage: number) => {
    setItemsPerPage(nextItemsPerPage);
    setPage(INITIAL_PAGE);
    void loadHistory(INITIAL_PAGE, nextItemsPerPage);
  };

  const handlePageChange = (nextPage: number) => {
    setPage(nextPage);
    void loadHistory(nextPage, itemsPerPage);
  };

  return (
    <div className="space-y-4">
      <DataGridTable
        rows={items}
        columns={columns}
        gridTemplateColumns={batchHistoryGridColumns}
        minWidth={1170}
        getRowKey={(item) => String(item.id)}
        emptyMessage="バッチ履歴はありません。"
        onRowClick={(item) => setSelectedItem(item)}
      />
      <PaginationControls
        currentPage={pagination.page}
        totalPages={pagination.totalPages}
        totalItems={pagination.totalItems}
        itemsPerPage={pagination.limit}
        rowsPerPageOptions={[10, 20, 50, 100]}
        onPageChange={handlePageChange}
        onItemsPerPageChange={handleItemsPerPageChange}
      />
      <BatchHistoryDetailDialog
        open={selectedItem !== null}
        item={selectedItem}
        onOpenChange={(open) => {
          if (!open) setSelectedItem(null);
        }}
      />
    </div>
  );
}
