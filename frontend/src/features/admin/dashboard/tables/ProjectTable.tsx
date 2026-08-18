import type {
  ProjectPlanProgressSummary,
  ProjectStatus,
} from '@/api/admin/projects';
import type { Project } from '@/api/projects';
import { useState, type KeyboardEvent, type ReactNode } from 'react';
import { ClipboardList, FileText, Paperclip } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { cn } from '@/lib/utils';
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

/**
 * 方法論区分・番号・名 on one line under the project name. Blanks are dropped so
 * a partially filled project does not render dangling separators.
 */
function methodologySummary(project: Project) {
  return [project.methodCategory, project.methodNumber, project.methodName]
    .map((part) => (part ?? '').toString().trim())
    .filter(Boolean)
    .join(' • ');
}

/** One labelled cell of the panel along the bottom of a card. */
function ProjectCardFact({
  icon,
  label,
  children,
}: {
  icon: ReactNode;
  label: string;
  children: ReactNode;
}) {
  return (
    <div className="flex min-w-0 flex-1 items-center gap-3 px-4 py-3">
      <span className="shrink-0 text-muted-foreground" aria-hidden="true">
        {icon}
      </span>
      <div className="min-w-0 space-y-1">
        <p className="text-subtle text-muted-foreground">{label}</p>
        {children}
      </div>
    </div>
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

  const handleCardKeyDown = (
    event: KeyboardEvent<HTMLDivElement>,
    project: Project,
  ) => {
    if (!onRowClick) return;

    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onRowClick(project);
    }
  };

  const renderCard = (project: Project) => {
    const progress = planProgress?.get(String(project.id));
    const resumeSlug = progress?.complete
      ? undefined
      : (progress?.nextIncompleteSlug ?? undefined);
    const statusColor = getProjectStatusColors(project.projectStatus);
    const summary = methodologySummary(project);

    return (
      <div
        role={onRowClick ? 'button' : undefined}
        tabIndex={onRowClick ? 0 : undefined}
        onClick={() => onRowClick?.(project)}
        onKeyDown={(event) => handleCardKeyDown(event, project)}
        className={cn(
          'rounded-xl border border-border bg-card',
          onRowClick && 'cursor-pointer transition hover:bg-muted/30',
        )}
      >
        <div className="flex flex-wrap items-start gap-3 px-5 py-4">
          <span
            className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-border text-muted-foreground"
            aria-hidden="true"
          >
            <FileText className="h-4 w-4" />
          </span>

          <div className="min-w-0 flex-1">
            <h3 className="truncate text-body font-bold text-foreground">
              {displayValue(project.projectName)}
            </h3>
            {summary && (
              <p className="mt-1 truncate text-subtle text-muted-foreground">
                {summary}
              </p>
            )}
          </div>

          {/* Controls sit inside the card, so their clicks must not also
              trigger the card's own row click. */}
          <div
            className="flex flex-wrap items-center gap-2"
            onClick={(event) => event.stopPropagation()}
            onPointerDown={(event) => event.stopPropagation()}
          >
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
                className="h-9 w-[150px] rounded-full border px-4 text-table font-bold shadow-none focus:ring-1"
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
        </div>

        <div className="mx-5 mb-4 flex items-stretch rounded-lg border border-border bg-muted/30">
          <ProjectCardFact
            icon={<ClipboardList className="h-5 w-5" />}
            label="計画書"
          >
            {progress && progress.totalForms > 0 ? (
              <p className="text-body text-foreground">
                <span className="font-bold">{progress.completedForms}</span>
                <span className="text-muted-foreground">
                  {' '}
                  / {progress.totalForms}
                </span>
              </p>
            ) : (
              // Absent while the batch is still loading, or when the plan API
              // is unavailable — the card must still render either way.
              <p className="text-body text-muted-foreground">-</p>
            )}
          </ProjectCardFact>

          <div className="my-3 w-px shrink-0 bg-border" aria-hidden="true" />

          <ProjectCardFact
            icon={<Paperclip className="h-5 w-5" />}
            label="操作"
          >
            <div
              onClick={(event) => event.stopPropagation()}
              onPointerDown={(event) => event.stopPropagation()}
            >
              {onOpenPlan && progress ? (
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => onOpenPlan(project, resumeSlug)}
                >
                  {progress.complete ? '計画書更新' : '続きから'}
                </Button>
              ) : (
                <p className="text-body text-muted-foreground">-</p>
              )}
            </div>
          </ProjectCardFact>
        </div>
      </div>
    );
  };

  return (
    <>
      {projects.length === 0 ? (
        <div className="rounded-xl border border-border bg-card px-5 py-8 text-center text-small text-foreground">
          まだプロジェクトが登録されていません。
        </div>
      ) : (
        <ul className="space-y-4">
          {projects.map((project) => (
            <li key={project.id}>{renderCard(project)}</li>
          ))}
        </ul>
      )}
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
