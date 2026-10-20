import { useCallback, useEffect, useMemo, useState } from 'react';
import { History, Info, X } from 'lucide-react';
import {
  getMonitoringDeletionHistory,
  type MonitoringDeletionHistoryItem,
  type MonitoringDeletionHistoryResult,
} from '@/api/monitoring';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { SkeletonTable } from '@/components/feedback/loading';
import { ActionButton } from '@/components/actions';
import { Button } from '@/components/ui/button';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { logger } from '@/lib/logger';
import {
  formatMonitoringDate,
  formatMonitoringDateTime,
  formatMonitoringValue,
} from './monitoringRecordDisplay';

const DEFAULT_PAGE = 1;
const DEFAULT_LIMIT = 10;
const PAGE_SIZE_OPTIONS = [10, 20, 50];
const DELETION_TABLE_MIN_WIDTH = 760;
const DELETION_TABLE_COLUMNS =
  'minmax(140px,1.2fr) minmax(100px,.9fr) minmax(120px,1fr) minmax(160px,1.4fr) minmax(120px,1fr)';

const EMPTY_RESULT: MonitoringDeletionHistoryResult = {
  history: [],
  pagination: {
    page: DEFAULT_PAGE,
    limit: DEFAULT_LIMIT,
    totalItems: 0,
    totalPages: 1,
    nextPageToken: null,
    hasMore: false,
  },
};

/** Header button that opens the deletion history in a dialog (same shape as 取消一覧); it fetches only while open. */
export function MonitoringDeletionHistoryDialog({
  projectId,
}: Readonly<{ projectId: string }>) {
  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm" className="shrink-0 gap-1.5">
          <History className="h-4 w-4" aria-hidden="true" />
          削除履歴
        </Button>
      </DialogTrigger>
      <DialogContent
        hideCloseButton
        className="max-h-[90dvh] max-w-[min(96vw,80rem)] overflow-y-auto"
      >
        {/* Close button is centred on the title + description block, like the card header action. */}
        <DialogHeader className="flex-row items-center justify-between gap-4 space-y-0">
          <div className="min-w-0 space-y-1.5">
            <DialogTitle>削除履歴</DialogTitle>
            <DialogDescription>
              このプロジェクトで削除されたモニタリングデータの履歴です。
            </DialogDescription>
          </div>
          <DialogClose asChild>
            <Button type="button" variant="ghost" size="icon" aria-label="閉じる">
              <X className="h-4 w-4" aria-hidden="true" />
            </Button>
          </DialogClose>
        </DialogHeader>
        <MonitoringDeletionHistoryTable projectId={projectId} />
      </DialogContent>
    </Dialog>
  );
}

/** Lists GET /admin/monitoring-data/deletion-history for one project. */
export function MonitoringDeletionHistoryTable({
  projectId,
}: Readonly<{ projectId: string }>) {
  const [page, setPage] = useState(DEFAULT_PAGE);
  const [limit, setLimit] = useState(DEFAULT_LIMIT);
  const [result, setResult] =
    useState<MonitoringDeletionHistoryResult>(EMPTY_RESULT);
  const [isLoading, setIsLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  const loadHistory = useCallback(async () => {
    try {
      setIsLoading(true);
      setLoadError(null);
      setResult(await getMonitoringDeletionHistory({ projectId, page, limit }));
    } catch (error) {
      logger.error('Failed to load monitoring deletion history:', error);
      setResult(EMPTY_RESULT);
      setLoadError(
        error instanceof Error ? error.message : '削除履歴の取得に失敗しました。',
      );
    } finally {
      setIsLoading(false);
    }
  }, [limit, page, projectId]);

  useEffect(() => {
    void loadHistory();
  }, [loadHistory]);

  const columns = useMemo<DataGridTableColumn<MonitoringDeletionHistoryItem>[]>(
    () => [
      {
        id: 'deletedAt',
        label: '削除日時',
        sortable: true,
        render: (item) => formatMonitoringDateTime(item.deletedAt),
        sortValue: (item) => item.deletedAt,
      },
      {
        id: 'dateRecorded',
        label: '記録日',
        sortable: true,
        render: (item) => formatMonitoringDate(item.dateRecorded),
        sortValue: (item) => item.dateRecorded,
      },
      {
        id: 'monitoringRecordId',
        label: 'レコードID',
        render: (item) => (
          <span className="font-mono text-subtle text-muted-foreground">
            {formatMonitoringValue(item.monitoringRecordId)}
          </span>
        ),
      },
      {
        id: 'result',
        label: '結果',
        sortable: true,
        render: (item) =>
          item.result === 'SUCCESS' ? (
            <StatusBadge tone="success">成功</StatusBadge>
          ) : (
            <div className="flex items-center gap-1">
              <StatusBadge tone="danger">失敗</StatusBadge>
              {item.failureReason ? (
                // Click, not hover: works on touch and keeps the full reason readable.
                <Popover>
                  <PopoverTrigger asChild>
                    <button
                      type="button"
                      aria-label="失敗理由を表示"
                      className="inline-flex h-7 w-7 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    >
                      <Info className="h-4 w-4" aria-hidden="true" />
                    </button>
                  </PopoverTrigger>
                  <PopoverContent align="start" className="w-80 space-y-1">
                    <p className="text-subtle font-bold text-destructive">
                      失敗理由
                    </p>
                    <p className="whitespace-pre-wrap break-words text-small text-foreground">
                      {item.failureReason}
                    </p>
                  </PopoverContent>
                </Popover>
              ) : null}
            </div>
          ),
        sortValue: (item) => item.result,
      },
      {
        id: 'deletedBy',
        label: '削除者',
        sortable: true,
        render: (item) => formatMonitoringValue(item.deletedBy),
        sortValue: (item) => item.deletedBy,
      },
    ],
    [],
  );

  return (
    <div className="space-y-4">
      {isLoading ? (
        <SkeletonTable
          columns={5}
          rows={4}
          gridTemplateColumns={DELETION_TABLE_COLUMNS}
          minWidth={DELETION_TABLE_MIN_WIDTH}
        />
      ) : loadError ? (
        <div className="space-y-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center">
          <p role="alert" className="text-small text-destructive">
            {loadError}
          </p>
          <ActionButton kind="retry" onClick={() => void loadHistory()} />
        </div>
      ) : (
        <>
          <DataGridTable
            rows={result.history}
            columns={columns}
            gridTemplateColumns={DELETION_TABLE_COLUMNS}
            minWidth={DELETION_TABLE_MIN_WIDTH}
            getRowKey={(item) => String(item.id)}
            emptyMessage="削除履歴はありません。"
          />
          <PaginationControls
            currentPage={Math.max(result.pagination.page || page, 1)}
            totalPages={result.pagination.totalPages}
            totalItems={result.pagination.totalItems}
            itemsPerPage={limit}
            rowsPerPageOptions={PAGE_SIZE_OPTIONS}
            onPageChange={setPage}
            onItemsPerPageChange={(nextLimit) => {
              setLimit(nextLimit);
              setPage(DEFAULT_PAGE);
            }}
          />
        </>
      )}
    </div>
  );
}
