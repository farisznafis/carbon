import { useCallback, useEffect, useMemo, useState } from 'react';
import { Eye, RefreshCw } from 'lucide-react';
import {
  getAdminProjectMonitoringRecords,
  type MonitoringRecordListItem,
  type MonitoringRecordListResult,
} from '@/api/monitoring';
import type { Project } from '@/api/projects';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { logger } from '@/lib/logger';
import {
  formatMonitoringDate,
  formatMonitoringNumber,
  formatMonitoringValue,
} from '@/features/admin/monitoring/monitoringRecordDisplay';

const DEFAULT_PAGE = 1;
const DEFAULT_LIMIT = 10;
const PAGE_SIZE_OPTIONS = [10, 20, 50];

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
  projects: Project[];
  projectsLoading: boolean;
  projectsError: string | null;
  selectedProjectId: string;
  initialPage?: number;
  initialLimit?: number;
  onProjectChange: (projectId: string) => void;
  onViewDetail: (
    record: MonitoringRecordListItem,
    page: number,
    limit: number,
  ) => void;
};

export function ProjectMonitoringTable({
  projects,
  projectsLoading,
  projectsError,
  selectedProjectId,
  initialPage = DEFAULT_PAGE,
  initialLimit = DEFAULT_LIMIT,
  onProjectChange,
  onViewDetail,
}: ProjectMonitoringTableProps) {
  const [page, setPage] = useState(initialPage);
  const [limit, setLimit] = useState(initialLimit);
  const [result, setResult] =
    useState<MonitoringRecordListResult>(EMPTY_RESULT);
  const [isLoading, setIsLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  const loadMonitoringRecords = useCallback(async () => {
    if (!selectedProjectId) {
      setResult(EMPTY_RESULT);
      setLoadError(null);
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setLoadError(null);
      const nextResult = await getAdminProjectMonitoringRecords({
        projectId: selectedProjectId,
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
  }, [limit, page, selectedProjectId]);

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
        id: 'siteID',
        label: '拠点ID',
        sortable: true,
        render: (record) => formatMonitoringValue(record.siteID),
        sortValue: (record) => String(record.siteID),
      },
      {
        id: 'participatingUserID',
        label: '参加ユーザID',
        sortable: true,
        render: (record) => formatMonitoringValue(record.participatingUserID),
        sortValue: (record) => record.participatingUserID,
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
        label: 'アクション',
        render: (record) => (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onViewDetail(record, page, limit)}
          >
            <Eye />
            詳細
          </Button>
        ),
      },
    ],
    [limit, onViewDetail, page],
  );

  const handleProjectChange = (projectId: string) => {
    setPage(DEFAULT_PAGE);
    setLimit(DEFAULT_LIMIT);
    setResult(EMPTY_RESULT);
    onProjectChange(projectId);
  };

  return (
    <div className="space-y-4">
      <div className="max-w-md space-y-2">
        <label
          htmlFor="monitoring-project-select"
          className="text-body font-medium text-foreground"
        >
          プロジェクト
        </label>
        <Select
          value={selectedProjectId}
          onValueChange={handleProjectChange}
          disabled={projectsLoading || Boolean(projectsError)}
        >
          <SelectTrigger id="monitoring-project-select">
            <SelectValue placeholder="プロジェクトを選択してください" />
          </SelectTrigger>
          <SelectContent>
            {projects.map((project) => (
              <SelectItem key={project.id} value={String(project.id)}>
                {project.projectName || String(project.id)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        {projectsLoading && (
          <p className="text-small text-muted-foreground">
            プロジェクトを読み込んでいます...
          </p>
        )}
        {projectsError && (
          <p role="alert" className="text-small text-destructive">
            {projectsError}
          </p>
        )}
      </div>

      {!selectedProjectId ? (
        <div className="rounded-lg border border-dashed border-border bg-muted/20 px-6 py-10 text-center text-small text-muted-foreground">
          表示するプロジェクトを選択してください。
        </div>
      ) : isLoading ? (
        <div className="rounded-lg border border-border py-12 text-center text-small text-muted-foreground">
          モニタリングデータを読み込んでいます...
        </div>
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
            gridTemplateColumns="120px 80px 110px minmax(120px,1fr) 100px 110px 110px 120px 90px 80px 96px"
            minWidth={1280}
            getRowKey={(record) => record.id}
            emptyMessage="モニタリングデータはありません。"
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
