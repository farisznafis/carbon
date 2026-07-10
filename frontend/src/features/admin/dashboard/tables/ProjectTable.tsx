import {
  Action,
  Column,
  ProTable,
} from '@/components/data-display/ProTable/ProTable';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { ProjectStatus } from '@/api/admin/projects';
import type { Project } from '@/api/projects';

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

export function ProjectTable({
  projects,
  onEdit,
  onUpdate,
  onRowClick,
  onStatusChange,
  updatingProjectStatusId,
}: ProjectTableProps) {
  const columns: Column<Project>[] = [
    {
      key: 'projectName',
      label: 'プロジェクト名称',
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[180px]',
    },
    {
      key: 'methodCategory',
      label: '方法論区分',
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[140px]',
    },
    {
      key: 'methodNumber',
      label: '方法論番号',
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[140px]',
    },
    {
      key: 'methodName',
      label: '方法論名称',
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[180px]',
    },
    {
      key: 'methodMainEmission',
      label: '排出削減活動',
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[160px]',
    },
    {
      key: 'methodologyVersion',
      label: 'Version',
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[120px]',
    },
    {
      key: 'projectStatus',
      label: 'ステータス',
      sortable: true,
      truncate: true,
      maxWidth: 'max-w-[180px]',
      render: (_, row) => (
        <div
          className="w-36 flex items-center justify-center "
          onClick={(event) => event.stopPropagation()}
          onPointerDown={(event) => event.stopPropagation()}
        >
          <Select
            value={row.projectStatus}
            disabled={updatingProjectStatusId === row.id}
            onValueChange={(value) =>
              onStatusChange?.(row, value as ProjectStatus)
            }
          >
            <SelectTrigger
              className={`h-8 rounded-full border-0 px-3 text-xs font-medium shadow-none focus:ring-1 ${getProjectStatusClassName(row.projectStatus)}`}
              title="ステータスを変更"
            >
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {getStatusOptions(row.projectStatus).map((status) => (
                <SelectItem key={status} value={status}>
                  {status}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      ),
    },
  ];

  const actions: Action<Project>[] = [
    {
      label: '詳細',
      onClick: (row) => onEdit?.(row),
      colorClass: 'text-white',
    },
    {
      label: '更新',
      onClick: (row) => onUpdate?.(row),
      colorClass: 'text-white',
    },
  ];

  return (
    <ProTable
      data={projects}
      columns={columns}
      actions={actions}
      height="max-h-[320px]"
      emptyState={
        <p className="text-muted-foreground">
          まだプロジェクトが登録されていません
        </p>
      }
      onRowClick={onRowClick}
    />
  );
}
