import { useMemo } from 'react';
import type { ParticipationReviewDetail } from '@/api/admin';
import type { Project } from '@/api/projects';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { SkeletonTable } from '@/components/feedback/loading';
import { Button } from '@/components/ui/button';
import { isParticipationReviewForProject } from '@/features/admin/membership-review/participationReviewProjectMatch';
import { usePagination } from '@/hooks/usePagination';

const PROJECT_STATUS_PAGE_SIZE = 5;
const projectStatusGridColumns =
  'minmax(240px,1.5fr) minmax(120px,.7fr) minmax(210px,1fr) minmax(96px,.45fr)';

type AdminProjectStatusTableProps = {
  projects: Project[];
  openReviews: ParticipationReviewDetail[];
  projectsLoading: boolean;
  projectsError: string | null;
  openReviewsError: string | null;
  onOpenMembership: (project: Project) => void;
};

/** Resolves a design-system tone for a project status. */
function getProjectStatusTone(status: string) {
  const normalizedStatus = status.trim().toUpperCase();

  if (['ACTIVE', 'RUNNING', '実施中'].includes(normalizedStatus)) return 'success' as const;
  if (['REJECTED', '却下', 'ENDED', 'INACTIVE', '終了'].includes(normalizedStatus)) return 'danger' as const;
  if (['CREATED', '作成済み'].includes(normalizedStatus)) return 'neutral' as const;
  return 'warning' as const;
}

/** Formats the actual project reduction returned by the API. */
function formatEmissionReduction(value: number | undefined) {
  return value === undefined ? '—' : `${value.toLocaleString('ja-JP')} t-CO₂`;
}

/** Counts OPEN membership reviews that belong to one project. */
function countOpenReviews(
  project: Project,
  openReviews: ParticipationReviewDetail[],
) {
  return openReviews.filter((review) =>
    isParticipationReviewForProject(review, project.id, project),
  ).length;
}

/** Renders the paginated project-priority table on Admin Home. */
export function AdminProjectStatusTable({
  projects,
  openReviews,
  projectsLoading,
  projectsError,
  openReviewsError,
  onOpenMembership,
}: Readonly<AdminProjectStatusTableProps>) {
  const prioritizedProjects = useMemo(() => {
    if (openReviewsError) return projects;

    return [...projects].sort((leftProject, rightProject) => {
      const reviewDifference =
        countOpenReviews(rightProject, openReviews) -
        countOpenReviews(leftProject, openReviews);

      return reviewDifference || leftProject.projectName.localeCompare(
        rightProject.projectName,
        'ja',
      );
    });
  }, [openReviews, openReviewsError, projects]);

  const {
    currentPage,
    totalPages,
    totalItems,
    itemsPerPage,
    paginatedItems,
    goToPage,
    setItemsPerPage,
  } = usePagination(prioritizedProjects, {
    itemsPerPage: PROJECT_STATUS_PAGE_SIZE,
  });

  const columns = useMemo<DataGridTableColumn<Project>[]>(
    () => [
      {
        id: 'projectName',
        label: 'プロジェクト名',
        sortable: true,
        sortValue: (project) => project.projectName,
        render: (project) => (
          <div className="min-w-0">
            <p className="truncate text-body font-bold text-foreground">
              {project.projectName}
            </p>
            {!openReviewsError && countOpenReviews(project, openReviews) > 0 && (
              <p className="mt-1 text-subtle text-primary-strong">
                入会届 {countOpenReviews(project, openReviews)}件
              </p>
            )}
          </div>
        ),
      },
      {
        id: 'status',
        label: 'ステータス',
        sortable: true,
        sortValue: (project) => project.projectStatus,
        render: (project) => (
          <StatusBadge tone={getProjectStatusTone(project.projectStatus)}>
            {project.projectStatus || '—'}
          </StatusBadge>
        ),
      },
      {
        id: 'reduction',
        label: '削減量（t-CO₂）',
        sortable: true,
        sortValue: (project) => project.emissionReduction,
        render: (project) => (
          <div className="space-y-2">
            <p className="text-body font-bold text-foreground">
              {formatEmissionReduction(project.emissionReduction)}
            </p>
            <div className="flex h-5 items-center rounded-full bg-muted/60 px-3">
              <span className="text-[11px] text-muted-foreground">グラフ準備中</span>
            </div>
          </div>
        ),
      },
      {
        id: 'action',
        label: '操作',
        render: (project) => (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onOpenMembership(project)}
          >
            入会届
          </Button>
        ),
      },
    ],
    [onOpenMembership, openReviews, openReviewsError],
  );

  if (projectsLoading) {
    return <SkeletonTable columns={4} rows={PROJECT_STATUS_PAGE_SIZE} minWidth={760} />;
  }

  if (projectsError) {
    return (
      <div role="alert" className="rounded-xl border border-destructive/30 bg-destructive/5 p-6 text-center text-small text-destructive">
        {projectsError}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {openReviewsError && (
        <p role="status" className="rounded-lg border border-warning-border bg-warning-subtle px-4 py-3 text-subtle text-warning-strong">
          入会届の優先順位を計算できませんでした。プロジェクトは取得順で表示しています。
        </p>
      )}
      <DataGridTable
        rows={paginatedItems}
        columns={columns}
        gridTemplateColumns={projectStatusGridColumns}
        minWidth={760}
        getRowKey={(project) => project.id}
        emptyMessage="登録済みのプロジェクトはありません。"
      />
      <PaginationControls
        currentPage={currentPage}
        totalPages={totalPages}
        totalItems={totalItems}
        itemsPerPage={itemsPerPage}
        rowsPerPageOptions={[5, 10, 20]}
        onPageChange={goToPage}
        onItemsPerPageChange={setItemsPerPage}
      />
    </div>
  );
}
