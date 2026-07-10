import type { ProjectStatus } from '@/api/admin/projects';
import type { Project } from '@/api/projects';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  AdminGridTable,
  type AdminGridTableColumn,
} from '@/features/admin/dashboard/tables/AdminGridTable';

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

const projectTableGridColumns =
  'minmax(180px, 1.4fr) minmax(130px, 0.9fr) minmax(120px, 0.8fr) minmax(170px, 1.1fr) minmax(130px, 0.8fr) minmax(180px, 1fr)';

function getProjectStatusClassName(status: Project['projectStatus']) {
  const normalized = String(status).toLowerCase();

  if (normalized.includes('実施中') || normalized === 'active') {
    return 'bg-emerald-100 text-emerald-800';
  }

  if (normalized.includes('却下') || normalized === 'rejected') {
    return 'bg-red-100 text-red-800';
  }

  if (normalized.includes('終了') || normalized === 'inactive') {
    return 'bg-slate-100 text-slate-800';
  }

  if (normalized.includes('検証') || normalized.includes('妥当性確認')) {
    return 'bg-sky-100 text-sky-800';
  }

  return 'bg-amber-100 text-amber-800';
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
  const columns: AdminGridTableColumn<Project>[] = [
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
      render: (project) => (
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
              className={`h-8 w-full rounded-full border-0 px-3 text-table font-bold shadow-none focus:ring-1 ${getProjectStatusClassName(project.projectStatus)}`}
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
      ),
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
    <AdminGridTable
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
