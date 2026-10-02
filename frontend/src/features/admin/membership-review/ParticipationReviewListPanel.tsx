import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import {
  getAdminProjectDetail,
  getParticipationReviewsByStatus,
  type ParticipationReviewDetail,
  type ParticipationReviewStatus,
} from '@/api/admin';
import { isForbiddenApiError } from '@/api/_shared/errors';
import { ActionButton, RowActionButton } from '@/components/actions';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { SkeletonTable } from '@/components/feedback/loading';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { formatAdminCompanyDateTime } from '@/features/admin/company-detail/companyDetailDisplay';
import { resolveAdminProjectNavigationState } from '@/features/admin/navigation/adminProjectNavigation';
import { PermissionDeniedNotice } from '@/features/admin/permissions/PermissionDeniedNotice';
import { usePagination } from '@/hooks/usePagination';
import { paths } from '@/routes/paths';
import { isParticipationReviewForProject } from './participationReviewProjectMatch';

const INITIAL_ITEMS_PER_PAGE = 10;
const REVIEW_GRID_COLUMNS =
  'minmax(180px, 1fr) minmax(180px, 1fr) minmax(190px, 0.9fr) minmax(150px, 0.75fr) minmax(110px, 0.5fr)';

const REVIEW_STATUSES: ParticipationReviewStatus[] = [
  'OPEN',
  'APPROVED',
  'REJECTED',
];
const EMPTY_REVIEWS_BY_STATUS: Record<
  ParticipationReviewStatus,
  ParticipationReviewDetail[]
> = {
  OPEN: [],
  APPROVED: [],
  REJECTED: [],
};
const REVIEW_STATUS_CONTENT: Record<
  ParticipationReviewStatus,
  { label: string; description: string; emptyMessage: string }
> = {
  OPEN: {
    label: '承認待ち',
    description: '管理者の承認を待っている入会届を表示しています。',
    emptyMessage: '承認待ちの入会届はありません。',
  },
  APPROVED: {
    label: '承認済み',
    description: '2名の管理者による承認が完了した入会届を表示しています。',
    emptyMessage: '承認済みの入会届はありません。',
  },
  REJECTED: {
    label: '却下',
    description: '管理者によって却下された入会届を表示しています。',
    emptyMessage: '却下された入会届はありません。',
  },
};

/** Resolves the membership status query to a supported status filter. */
function resolveReviewStatus(value: string | null): ParticipationReviewStatus {
  return REVIEW_STATUSES.includes(value as ParticipationReviewStatus)
    ? (value as ParticipationReviewStatus)
    : 'OPEN';
}

/** Returns the badge text shown for a row in the selected status filter. */
function getStatusBadgeLabel(
  status: ParticipationReviewStatus,
  review: ParticipationReviewDetail,
): string {
  if (status === 'OPEN') {
    return review.review.primaryApplReviewerName.trim()
      ? '二次確認待ち'
      : '一次確認待ち';
  }
  return REVIEW_STATUS_CONTENT[status].label;
}

/** Lists one project's membership applications by status; embedded in the project detail 入会届 tab. */
export function ParticipationReviewListPanel({
  projectId,
}: Readonly<{ projectId: string }>) {
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigationState = resolveAdminProjectNavigationState(location.state);
  const activeStatus = resolveReviewStatus(searchParams.get('status'));
  const [reviewsByStatus, setReviewsByStatus] = useState(EMPTY_REVIEWS_BY_STATUS);
  const [isLoading, setIsLoading] = useState(true);
  const [hasLoadError, setHasLoadError] = useState(false);
  const [hasPermissionError, setHasPermissionError] = useState(false);

  /** Loads all statuses at once, keeping only this project's applications. */
  const loadReviews = useCallback(async () => {
    setIsLoading(true);
    setHasLoadError(false);
    setHasPermissionError(false);

    try {
      const [project, statusReviews] = await Promise.all([
        getAdminProjectDetail(projectId),
        getParticipationReviewsByStatus(),
      ]);
      const belongsToProject = (review: ParticipationReviewDetail) =>
        isParticipationReviewForProject(review, projectId, project);

      setReviewsByStatus({
        OPEN: statusReviews.OPEN.filter(belongsToProject),
        APPROVED: statusReviews.APPROVED.filter(belongsToProject),
        REJECTED: statusReviews.REJECTED.filter(belongsToProject),
      });
    } catch (error) {
      setHasPermissionError(isForbiddenApiError(error));
      setHasLoadError(true);
    } finally {
      setIsLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    void loadReviews();
  }, [loadReviews]);

  const {
    currentPage,
    totalPages,
    totalItems,
    paginatedItems,
    goToPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(reviewsByStatus[activeStatus], {
    itemsPerPage: INITIAL_ITEMS_PER_PAGE,
  });

  const viewDetail = useCallback(
    (review: ParticipationReviewDetail) => {
      navigate(
        paths.adminProjectMembershipDetail(
          projectId,
          review.participationId,
          activeStatus,
        ),
        { state: navigationState },
      );
    },
    [activeStatus, navigate, navigationState, projectId],
  );

  /** Switches the status filter while keeping the page's other query state (e.g. the active tab). */
  const changeStatus = (value: string) => {
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.set('status', resolveReviewStatus(value));
    setSearchParams(nextSearchParams, { replace: true, state: location.state });
    goToPage(1);
  };

  const columns = useMemo<DataGridTableColumn<ParticipationReviewDetail>[]>(
    () => [
      {
        id: 'companyName',
        label: '会社名',
        sortable: true,
        sortValue: (review) => review.companyName,
        render: (review) => (
          <span className="block truncate font-bold text-foreground">
            {review.companyName || '―'}
          </span>
        ),
      },
      {
        id: 'siteName',
        label: '拠点名',
        sortable: true,
        sortValue: (review) => review.siteName,
        render: (review) => (
          <span className="block truncate text-foreground">
            {review.siteName || '―'}
          </span>
        ),
      },
      {
        id: 'submittedAt',
        label: '申請日時',
        sortable: true,
        sortValue: (review) => review.submittedAt,
        render: (review) => (
          <span className="block truncate text-foreground">
            {formatAdminCompanyDateTime(review.submittedAt)}
          </span>
        ),
      },
      {
        id: 'status',
        label: 'ステータス',
        sortable: true,
        sortValue: (review) => getStatusBadgeLabel(activeStatus, review),
        render: (review) => (
          <Badge
            variant={activeStatus === 'REJECTED' ? 'destructive' : 'outline'}
            className={
              activeStatus === 'APPROVED'
                ? 'border-success/30 bg-success/10 text-success'
                : activeStatus === 'OPEN'
                  ? 'border-primary/30 bg-primary/5 text-primary'
                  : undefined
            }
          >
            {getStatusBadgeLabel(activeStatus, review)}
          </Badge>
        ),
      },
      {
        id: 'actions',
        label: '操作',
        render: (review) => (
          <RowActionButton onClick={() => viewDetail(review)} />
        ),
      },
    ],
    [activeStatus, viewDetail],
  );

  if (hasPermissionError) {
    return (
      <PermissionDeniedNotice permission="PARTICIPATING_COMPANY_APPLICATION_REVIEW" />
    );
  }

  if (hasLoadError) {
    return (
      <div className="space-y-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center">
        <p role="alert" className="text-small text-destructive">
          入会届一覧を取得できませんでした。
        </p>
        <ActionButton kind="retry" onClick={() => void loadReviews()} />
      </div>
    );
  }

  return (
    <Tabs value={activeStatus} onValueChange={changeStatus} className="space-y-5">
      <TabsList className="grid h-auto w-full grid-cols-3 sm:w-fit">
        {REVIEW_STATUSES.map((status) => (
          <TabsTrigger key={status} value={status}>
            {REVIEW_STATUS_CONTENT[status].label}（
            {reviewsByStatus[status].length}）
          </TabsTrigger>
        ))}
      </TabsList>

      {REVIEW_STATUSES.map((status) => (
        <TabsContent key={status} value={status} className="space-y-4">
          <p className="text-small text-foreground">
            {REVIEW_STATUS_CONTENT[status].description}
          </p>

          {isLoading ? (
            <SkeletonTable
              columns={5}
              rows={3}
              gridTemplateColumns={REVIEW_GRID_COLUMNS}
              minWidth={930}
            />
          ) : (
            <>
              <DataGridTable
                rows={paginatedItems}
                columns={columns}
                gridTemplateColumns={REVIEW_GRID_COLUMNS}
                minWidth={930}
                getRowKey={(review) => String(review.participationId)}
                emptyMessage={REVIEW_STATUS_CONTENT[status].emptyMessage}
                onRowClick={viewDetail}
              />
              <PaginationControls
                currentPage={currentPage}
                totalPages={totalPages}
                totalItems={totalItems}
                itemsPerPage={itemsPerPage}
                onPageChange={goToPage}
                onItemsPerPageChange={setItemsPerPage}
              />
            </>
          )}
        </TabsContent>
      ))}
    </Tabs>
  );
}
