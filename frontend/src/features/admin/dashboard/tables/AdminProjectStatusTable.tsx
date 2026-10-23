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
import { ArrowRight } from 'lucide-react';
import { getProjectStatusTone, normalizeProjectStatus } from '../projectStatusPresentation';

const PROJECT_STATUS_PAGE_SIZE = 5;
const DUMMY_REDUCTION_VALUES = [108, 132, 96, 20, 10] as const;
const MAX_DUMMY_REDUCTION_VALUE = Math.max(...DUMMY_REDUCTION_VALUES);
/** Narrow minimums keep the table unscrolled in the Home main column down to ~1280px viewports. */
const projectStatusGridColumns =
  'minmax(180px,1.5fr) minmax(112px,auto) minmax(152px,1fr)';
const PROJECT_STATUS_TABLE_MIN_WIDTH = 600;

type ProjectStatusRow = {
  project: Project;
  reductionValue: number;
  /** Bar length relative to the largest value, as a percentage. */
  reductionRatio: number;
};

type AdminProjectStatusTableProps = {
  projects: Project[];
  openReviews: ParticipationReviewDetail[];
  projectsLoading: boolean;
  projectsError: string | null;
  openReviewsError: string | null;
  onOpenProjectDetail: (project: Project) => void;
  onOpenMembership: (project: Project) => void;
};

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
  onOpenProjectDetail,
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

  const projectRows = useMemo<ProjectStatusRow[]>(
    () => paginatedItems.map((project, index) => {
      const reductionValue = DUMMY_REDUCTION_VALUES[index % DUMMY_REDUCTION_VALUES.length];

      return {
        project,
        reductionValue,
        reductionRatio: (reductionValue / MAX_DUMMY_REDUCTION_VALUE) * 100,
      };
    }),
    [paginatedItems],
  );

  const columns = useMemo<DataGridTableColumn<ProjectStatusRow>[]>(
    () => [
      {
        id: 'projectName',
        label: 'プロジェクト名',
        sortable: true,
        sortValue: (row) => row.project.projectName,
        render: ({ project }) => {
          const openReviewCount = countOpenReviews(project, openReviews);

          return (
            <div className="min-w-0">
              <p className="truncate text-body font-bold text-foreground">
                {project.projectName}
              </p>
              {!openReviewsError && openReviewCount > 0 && (
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="mt-1 h-auto gap-1.5 p-0 text-warning-strong hover:bg-transparent hover:text-warning-strong"
                  aria-label={`${project.projectName}の承認待ち入会届${openReviewCount}件を確認`}
                  onClick={() => onOpenMembership(project)}
                >
                  <StatusBadge tone="warning" className="py-0.5">
                    要確認
                  </StatusBadge>
                  <span>入会届 {openReviewCount}件</span>
                  <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
                </Button>
              )}
            </div>
          );
        },
      },
      {
        id: 'status',
        label: 'ステータス',
        sortable: true,
        sortValue: (row) => normalizeProjectStatus(row.project.projectStatus),
        render: ({ project }) => (
          <StatusBadge tone={getProjectStatusTone(project.projectStatus)}>
            {normalizeProjectStatus(project.projectStatus) || '—'}
          </StatusBadge>
        ),
      },
      {
        id: 'reduction',
        label: '削減量（t-CO₂）',
        sortable: true,
        sortValue: (row) => row.reductionValue,
        render: (row) => (
          <div className="flex min-w-0 items-center gap-3">
            <div className="min-w-0 flex-1" aria-hidden="true">
              <div
                className="h-2.5 min-w-1 rounded-sm bg-primary"
                style={{ width: `${row.reductionRatio}%` }}
              />
            </div>
            <span className="whitespace-nowrap text-body text-muted-foreground">
              {row.reductionValue.toLocaleString('ja-JP')} t-CO₂
            </span>
          </div>
        ),
      },
    ],
    [onOpenMembership, openReviews, openReviewsError],
  );

  if (projectsLoading) {
    return <SkeletonTable columns={3}rows={PROJECT_STATUS_PAGE_SIZE} minWidth={PROJECT_STATUS_TABLE_MIN_WIDTH} />;
  }

  if (projectsError) {
    return (
      <div role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center text-small text-destructive-strong">
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
        rows={projectRows}
        columns={columns}
        gridTemplateColumns={projectStatusGridColumns}
        minWidth={PROJECT_STATUS_TABLE_MIN_WIDTH}
        getRowKey={(row) => row.project.id}
        onRowClick={({ project }) => onOpenProjectDetail(project)}
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
