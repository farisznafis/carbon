import type {
  ProjectPlanProgressSummary,
  ProjectStatus,
} from '@/api/admin/projects';
import type { Project } from '@/api/projects';
import { useState } from 'react';
import { ArrowRight } from 'lucide-react';
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
import { ProjectCompletionDialog } from './ProjectCompletionDialog';

type ProjectTableProps = {
  projects: Project[];
  onEdit?: (project: Project) => void;
  onUpdate?: (project: Project) => void;
  onRowClick?: (project: Project) => void;
  onStatusChange?: (
    project: Project,
    status: ProjectStatus,
  ) => void | Promise<void>;
  updatingProjectStatusId?: string | null;
  /** Plan progress by project id, fetched in one batch by the parent. */
  planProgress?: ReadonlyMap<string, ProjectPlanProgressSummary>;
  /**
   * Opens the plan. The second argument is the page to land on: the first
   * unfinished one while the plan is incomplete, and undefined once it is done
   * so the operator can choose which page to update.
   */
  onOpenPlan?: (project: Project, slug?: string) => void;
};

const projectTableGridColumns =
  'minmax(240px,1.6fr) minmax(104px,.45fr) minmax(104px,.45fr) minmax(200px,1.1fr) minmax(160px,.8fr) minmax(150px,.7fr) minmax(184px,.85fr)';

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

function isTerminalProjectStatus(status: string) {
  const normalizedStatus = status.trim().toUpperCase();
  return ['終了', 'ENDED', 'INACTIVE'].includes(normalizedStatus);
}

function displayValue(value: unknown) {
  if (value === null || value === undefined || value === '') return '-';
  return String(value);
}

/** Plain single-line cell for the methodology columns. */
function TextCell({ value }: { value: unknown }) {
  return (
    <span className="block truncate text-body text-foreground">
      {displayValue(value)}
    </span>
  );
}

export function ProjectTable({
  projects,
  onEdit,
  onUpdate,
  onRowClick,
  onStatusChange,
  updatingProjectStatusId,
  planProgress,
  onOpenPlan,
}: ProjectTableProps) {
  const [completionProject, setCompletionProject] = useState<Project | null>(
    null,
  );

  const handleStatusChange = (project: Project, status: ProjectStatus) => {
    if (status === '終了') {
      setCompletionProject(project);
      return;
    }

    void onStatusChange?.(project, status);
  };

  const confirmCompletion = async () => {
    if (!completionProject || !onStatusChange) return;
    await onStatusChange(completionProject, '終了');
    setCompletionProject(null);
  };

  /**
   * Controls sit inside a clickable row, so their clicks must not also trigger
   * the row's own click.
   */
  const stopRowActivation = {
    onClick: (event: { stopPropagation: () => void }) =>
      event.stopPropagation(),
    onPointerDown: (event: { stopPropagation: () => void }) =>
      event.stopPropagation(),
  };

  const columns: DataGridTableColumn<Project>[] = [
    {
      id: 'projectName',
      label: 'プロジェクト名',
      sortable: true,
      sortValue: (project) => project.projectName,
      render: (project) => (
        <span className="block truncate text-body font-bold text-foreground">
          {displayValue(project.projectName)}
        </span>
      ),
    },
    {
      id: 'methodCategory',
      label: '方法論区分',
      sortable: true,
      sortValue: (project) => project.methodCategory,
      render: (project) => <TextCell value={project.methodCategory} />,
    },
    {
      id: 'methodNumber',
      label: '方法論番号',
      sortable: true,
      sortValue: (project) => project.methodNumber,
      render: (project) => <TextCell value={project.methodNumber} />,
    },
    {
      id: 'methodName',
      label: '方法論名',
      sortable: true,
      sortValue: (project) => project.methodName,
      render: (project) => <TextCell value={project.methodName} />,
    },
    {
      id: 'planProgress',
      label: '作成の進捗状況',
      // className: 'font-bold',
      render: (project) => {
        const progress = planProgress?.get(String(project.id));

        // Absent while the batch is still loading, or when the plan API is
        // unavailable — the row must still render either way.
        if (!progress || progress.totalForms === 0) {
          return <span className="text-body text-muted-foreground">-</span>;
        }

        const resumeSlug = progress.complete
          ? undefined
          : (progress.nextIncompleteSlug ?? undefined);

        return (
          <div className="flex min-w-0 items-center gap-3">
            <span className="whitespace-nowrap text-body text-foreground">
              <span className="font-bold">{progress.completedForms}</span>
              <span className="text-muted-foreground">
                /{progress.totalForms}
              </span>
            </span>
            {onOpenPlan && (
              <span {...stopRowActivation}>
                <Button
                  type="button"
                  variant="link"
                  className="h-auto gap-1 px-0 text-table font-bold"
                  onClick={() => onOpenPlan(project, resumeSlug)}
                >
                  {progress.complete ? '計画書更新' : '続きから'}
                  <ArrowRight className="h-3.5 w-3.5" />
                </Button>
              </span>
            )}
          </div>
        );
      },
    },
    {
      id: 'projectStatus',
      label: 'ステータス',
      sortable: true,
      sortValue: (project) => project.projectStatus,
      render: (project) => {
        const statusColor = getProjectStatusColors(project.projectStatus);

        return (
          <span className="block min-w-0" {...stopRowActivation}>
            <Select
              value={project.projectStatus}
              disabled={
                updatingProjectStatusId === project.id ||
                isTerminalProjectStatus(project.projectStatus)
              }
              onValueChange={(value) =>
                handleStatusChange(project, value as ProjectStatus)
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
      label: '操作',
      render: (project) => (
        <div
          className="flex flex-wrap justify-start gap-2"
          {...stopRowActivation}
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
    <>
      <DataGridTable
        rows={projects}
        columns={columns}
        gridTemplateColumns={projectTableGridColumns}
        minWidth={1240}
        getRowKey={(project) => project.id}
        emptyMessage="まだプロジェクトが登録されていません。"
        onRowClick={onRowClick}
      />
      <ProjectCompletionDialog
        project={completionProject}
        isSubmitting={
          Boolean(completionProject) &&
          updatingProjectStatusId === completionProject?.id
        }
        onOpenChange={(open) => {
          if (!open) setCompletionProject(null);
        }}
        onConfirm={() => void confirmCompletion()}
      />
    </>
  );
}
