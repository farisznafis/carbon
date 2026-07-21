import type { ProjectStatus } from '@/api/admin/projects';
import type { Project } from '@/api/projects';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

type ProjectTableProps = {
  projects: Project[];
  onEdit?: (project: Project) => void;
  onUpdate?: (project: Project) => void;
  onRowClick?: (project: Project) => void;
  onStatusChange?: (project: Project, status: ProjectStatus) => void;
  updatingProjectStatusId?: string | null;
};

const projectStatusOptions: ProjectStatus[] = [
  '作成済み',
  '準備中',
  '変更中',
  '妥当性確認処理',
  '却下',
  '実施中',
  '検証中',
  'クレジット申請中',
  '終了',
];

const statusColors: Record<
  ProjectStatus,
  { bg: string; text: string; border: string }
> = {
  作成済み: {
    bg: '#F1F5F9',
    text: '#475569',
    border: '#CBD5E1',
  },
  準備中: {
    bg: '#FEF3C7',
    text: '#92400E',
    border: '#FCD34D',
  },
  変更中: {
    bg: '#DBEAFE',
    text: '#1D4ED8',
    border: '#93C5FD',
  },
  妥当性確認処理: {
    bg: '#E0E7FF',
    text: '#4338CA',
    border: '#A5B4FC',
  },
  却下: {
    bg: '#FEE2E2',
    text: '#B91C1C',
    border: '#FCA5A5',
  },
  実施中: {
    bg: '#DCFCE7',
    text: '#15803D',
    border: '#86EFAC',
  },
  検証中: {
    bg: '#CCFBF1',
    text: '#0F766E',
    border: '#5EEAD4',
  },
  クレジット申請中: {
    bg: '#F3E8FF',
    text: '#7E22CE',
    border: '#D8B4FE',
  },
  終了: {
    bg: '#E5E7EB',
    text: '#374151',
    border: '#D1D5DB',
  },
};

const projectStatusAliases: Partial<Record<string, ProjectStatus>> = {
  CREATED: '作成済み',
  PREPARING: '準備中',
  CHANGING: '変更中',
  VALIDATING: '妥当性確認処理',
  REJECTED: '却下',
  ACTIVE: '実施中',
  RUNNING: '実施中',
  VERIFYING: '検証中',
  VERIFICATION: '検証中',
  CREDIT_APPLICATION: 'クレジット申請中',
  ENDED: '終了',
  INACTIVE: '終了',
};

const projectTableGridColumns =
  'minmax(180px, 1.2fr) minmax(86px, 0.55fr) minmax(86px, 0.55fr) minmax(260px, 1.8fr) minmax(130px, 0.8fr) minmax(180px, 1fr)';

function isProjectStatus(status: string): status is ProjectStatus {
  return projectStatusOptions.includes(status as ProjectStatus);
}

function getProjectStatusColors(status: Project['projectStatus']) {
  const normalizedStatus = String(status).trim();

  if (isProjectStatus(normalizedStatus)) {
    return statusColors[normalizedStatus];
  }

  const aliasedStatus = projectStatusAliases[normalizedStatus.toUpperCase()];
  return statusColors[aliasedStatus ?? '作成済み'];
}

function getStatusOptions(currentStatus: string): string[] {
  if (
    !currentStatus ||
    projectStatusOptions.includes(currentStatus as ProjectStatus)
  ) {
    return projectStatusOptions;
  }

  return [currentStatus, ...projectStatusOptions];
}

function displayValue(value: unknown) {
  if (value === null || value === undefined || value === '') return '-';
  return String(value);
}

export function ProjectTable({
  projects,
  onEdit,
  onUpdate,
  onRowClick,
  onStatusChange,
  updatingProjectStatusId,
}: ProjectTableProps) {
  const columns: DataGridTableColumn<Project>[] = [
    {
      id: 'projectName',
      label: 'プロジェクト名',
      sortable: true,
      sortValue: (project) => project.projectName,
      render: (project) => (
        <span className="block truncate text-body font-bold text-slate-800">
          {displayValue(project.projectName)}
        </span>
      ),
    },
    {
      id: 'methodCategory',
      label: '方法論区分',
      sortable: true,
      sortValue: (project) => project.methodCategory,
      render: (project) => (
        <span className="block truncate text-body text-foreground">
          {displayValue(project.methodCategory)}
        </span>
      ),
    },
    {
      id: 'methodNumber',
      label: '方法論番号',
      sortable: true,
      sortValue: (project) => project.methodNumber,
      render: (project) => (
        <span className="block truncate text-body text-foreground">
          {displayValue(project.methodNumber)}
        </span>
      ),
    },
    {
      id: 'methodName',
      label: '方法論名',
      sortable: true,
      sortValue: (project) => project.methodName,
      render: (project) => (
        <span className="block truncate text-body text-foreground">
          {displayValue(project.methodName)}
        </span>
      ),
    },
    {
      id: 'projectStatus',
      label: 'ステータス',
      sortable: true,
      sortValue: (project) => project.projectStatus,
      render: (project) => {
        const statusColor = getProjectStatusColors(project.projectStatus);

        return (
          <span
            className="block min-w-0"
            onClick={(event) => event.stopPropagation()}
            onPointerDown={(event) => event.stopPropagation()}
          >
            <Select
              value={project.projectStatus}
              disabled={updatingProjectStatusId === project.id}
              onValueChange={(value) =>
                onStatusChange?.(project, value as ProjectStatus)
              }
            >
              <SelectTrigger
                className="h-8 w-full rounded-full border px-3 text-table font-bold shadow-none focus:ring-1"
                style={{
                  backgroundColor: statusColor.bg,
                  color: statusColor.text,
                  borderColor: statusColor.border,
                }}
                title="ステータスを変更"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {getStatusOptions(project.projectStatus).map((status) => (
                  <SelectItem key={status} value={status}>
                    {status}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </span>
        );
      },
    },
    {
      id: 'actions',
      label: 'アクション',
      render: (project) => (
        <div
          className="flex justify-start gap-2"
          onClick={(event) => event.stopPropagation()}
          onPointerDown={(event) => event.stopPropagation()}
        >
          <Button
            type="button"
            variant="primary"
            onClick={() => onEdit?.(project)}
          >
            詳細
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => onUpdate?.(project)}
          >
            更新
          </Button>
        </div>
      ),
    },
  ];

  return (
    <DataGridTable
      rows={projects}
      columns={columns}
      gridTemplateColumns={projectTableGridColumns}
      minWidth={980}
      getRowKey={(project) => project.id}
      emptyMessage="まだプロジェクトが登録されていません。"
      onRowClick={onRowClick}
    />
  );
}
