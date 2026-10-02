import { useCallback, useEffect, useMemo, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import {
  getAdminProjectMonitoringRecords,
  type MonitoringRecordListItem,
  type MonitoringRecordListResult,
} from '@/api/monitoring';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { RowActionButton } from '@/components/data-display/RowActionButton';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { SkeletonTable } from '@/components/feedback/loading';
import { Button } from '@/components/ui/button';
import { logger } from '@/lib/logger';
import {
  formatMonitoringDate,
  formatMonitoringNumber,
  formatMonitoringValue,
} from './monitoringRecordDisplay';

const DEFAULT_PAGE = 1;
const DEFAULT_LIMIT = 10;
const PAGE_SIZE_OPTIONS = [10, 20, 50];
const MONITORING_TABLE_MIN_WIDTH = 920;
const MONITORING_TABLE_COLUMNS =
  'minmax(90px,1.1fr) minmax(70px,.8fr) repeat(5,minmax(88px,1fr)) minmax(70px,.7fr) 88px';

const EMPTY_RESULT: MonitoringRecordListResult = {
  items: [],
  pagination: {
    page: DEFAULT_PAGE,
    limit: DEFAULT_LIMIT,
    totalItems: 0,
    totalPages: 1,
    nextPageToken: null,
    hasMore: false,
  },
};

type ProjectMonitoringTableProps = {
  projectId: string;
  initialPage?: number;
  initialLimit?: number;
  onViewDetail: (
    record: MonitoringRecordListItem,
    page: number,
    limit: number,
  ) => void;
};

/** Displays monitoring records for the project supplied by the project-detail route. */
export function ProjectMonitoringTable({
  projectId,
  initialPage = DEFAULT_PAGE,
  initialLimit = DEFAULT_LIMIT,
  onViewDetail,
}: ProjectMonitoringTableProps) {
  const [page, setPage] = useState(initialPage);
  const [limit, setLimit] = useState(initialLimit);
  const [result, setResult] =
    useState<MonitoringRecordListResult>(EMPTY_RESULT);
  const [isLoading, setIsLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  /** Fetches GET /monitoring/{projectId}/list for the active pagination state. */
  const loadMonitoringRecords = useCallback(async () => {
    try {
      setIsLoading(true);
      setLoadError(null);
      const nextResult = await getAdminProjectMonitoringRecords({
        projectId,
        page,
        limit,
      });
      setResult(nextResult);
    } catch (error) {
      logger.error('Failed to load project monitoring records:', error);
      setResult(EMPTY_RESULT);
      setLoadError(
        error instanceof Error
          ? error.message
          : 'モニタリングデータの取得に失敗しました。',
      );
    } finally {
      setIsLoading(false);
    }
  }, [limit, page, projectId]);

  useEffect(() => {
    void loadMonitoringRecords();
  }, [loadMonitoringRecords]);

  const columns = useMemo<DataGridTableColumn<MonitoringRecordListItem>[]>(
    () => [
      {
        id: 'dateRecorded',
        label: '記録日',
        sortable: true,
        render: (record) => formatMonitoringDate(record.dateRecorded),
        sortValue: (record) => record.dateRecorded,
      },
      {
        id: 'methodologyType',
        label: '方法論',
        sortable: true,
        render: (record) => formatMonitoringValue(record.methodologyType),
        sortValue: (record) => record.methodologyType,
      },
      {
        id: 'elPv',
        label: '発電量',
        sortable: true,
        render: (record) => formatMonitoringNumber(record.elPv, 'kWh'),
        sortValue: (record) => record.elPv,
      },
      {
        id: 'elPvr',
        label: '自家消費量',
        sortable: true,
        render: (record) => formatMonitoringNumber(record.elPvr, 'kWh'),
        sortValue: (record) => record.elPvr,
      },
      {
        id: 'elpjControlValue',
        label: '制御時間',
        sortable: true,
        render: (record) =>
          formatMonitoringNumber(record.elpjControlValue, '時間'),
        sortValue: (record) => record.elpjControlValue,
      },
      {
        id: 'elpjBatteryValue',
        label: '蓄電池放電量',
        sortable: true,
        render: (record) =>
          formatMonitoringNumber(record.elpjBatteryValue, 'kWh'),
        sortValue: (record) => record.elpjBatteryValue,
      },
      {
        id: 'elpjAccuracy',
        label: '測定精度',
        sortable: true,
        render: (record) =>
          formatMonitoringNumber(record.elpjAccuracy, '%'),
        sortValue: (record) => record.elpjAccuracy,
      },
      {
        id: 'isActive',
        label: '状態',
        sortable: true,
        render: (record) => (
          <StatusBadge tone={record.isActive ? 'success' : 'neutral'}>
            {record.isActive ? '有効' : '無効'}
          </StatusBadge>
        ),
        sortValue: (record) => (record.isActive ? 1 : 0),
      },
      {
        id: 'actions',
        label: '操作',
        render: (record) => (
          <RowActionButton onClick={() => onViewDetail(record, page, limit)} />
        ),
      },
    ],
    [limit, onViewDetail, page],
  );

  return (
    <div className="space-y-4">
      {isLoading ? (
        <SkeletonTable
          columns={9}
          rows={6}
          gridTemplateColumns={MONITORING_TABLE_COLUMNS}
          minWidth={MONITORING_TABLE_MIN_WIDTH}
        />
      ) : loadError ? (
        <div className="space-y-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center">
          <p role="alert" className="text-small text-destructive">
            {loadError}
          </p>
          <Button
            type="button"
            variant="outline"
            onClick={() => void loadMonitoringRecords()}
          >
            <RefreshCw />
            再試行
          </Button>
        </div>
      ) : (
        <>
          <DataGridTable
            rows={result.items}
            columns={columns}
            gridTemplateColumns={MONITORING_TABLE_COLUMNS}
            minWidth={MONITORING_TABLE_MIN_WIDTH}
            getRowKey={(record) => `${record.id}:${record.timeUpdated}`}
            emptyMessage="モニタリングデータはありません。"
            onRowClick={(record) => onViewDetail(record, page, limit)}
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
