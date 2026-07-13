import type { ProjectStatus } from '@/api/admin/projects';
import type { Project } from '@/api/projects';
import {
  AdminGridTable,
  type AdminGridTableColumn,
} from '@/components/data-display/AdminGridTable';
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

const projectTableGridColumns =
  'minmax(180px, 1.2fr) minmax(86px, 0.55fr) minmax(86px, 0.55fr) minmax(260px, 1.8fr) minmax(130px, 0.8fr) minmax(180px, 1fr)';

function getProjectStatusClassName(status: Project['projectStatus']) {
  const normalized = String(status).toLowerCase();

  if (
    normalized.includes('却下') ||
    normalized.includes('蜊ｴ荳') ||
    normalized === 'rejected'
  ) {
    return 'bg-red-100 text-red-800';
  }

  if (
    normalized.includes('終了') ||
    normalized.includes('邨ゆｺ') ||
    normalized === 'inactive' ||
    normalized === 'ended'
  ) {
    return 'bg-slate-100 text-slate-800';
  }

  if (
    normalized.includes('実施中') ||
    normalized.includes('螳滓命荳ｭ') ||
    normalized === 'active' ||
    normalized === 'running'
  ) {
    return 'bg-emerald-100 text-emerald-800';
  }

  if (
    normalized.includes('クレジット申請') ||
    normalized.includes('繧ｯ繝ｬ繧ｸ繝') ||
    normalized.includes('credit')
  ) {
    return 'bg-violet-100 text-violet-800';
  }

  if (
    normalized.includes('変更') ||
    normalized.includes('螟画峩') ||
    normalized.includes('change')
  ) {
    return 'bg-orange-100 text-orange-800';
  }

  if (
    normalized.includes('検証') ||
    normalized.includes('讀懆ｨｼ') ||
    normalized.includes('verify') ||
    normalized.includes('verification')
  ) {
    return 'bg-blue-100 text-blue-800';
  }

  if (
    normalized.includes('妥当性確認') ||
    normalized.includes('螯･蠖捺') ||
    normalized.includes('validation')
  ) {
    return 'bg-sky-100 text-sky-800';
  }

  if (
    normalized.includes('作成') ||
    normalized.includes('菴懈') ||
    normalized.includes('created')
  ) {
    return 'bg-stone-100 text-stone-800';
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
