import { useCallback, useMemo } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import type {
  ParticipationReviewDetail,
  ParticipationReviewStatus,
} from '@/api/admin';
import { ActionButton } from '@/components/actions';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { SkeletonTable } from '@/components/feedback/loading';
import {
  SegmentTabsList,
  SegmentTabsTrigger,
  TabCount,
} from '@/components/navigation/PageTabs';
import { Tabs, TabsContent } from '@/components/ui/tabs';
import { formatAdminCompanyDateTime } from '@/features/admin/company-detail/companyDetailDisplay';
import { resolveAdminProjectNavigationState } from '@/features/admin/navigation/adminProjectNavigation';
import { PermissionDeniedNotice } from '@/features/admin/permissions/PermissionDeniedNotice';
import type { ParticipationReviewsByStatus } from '@/hooks/admin/useProjectParticipationReviews';
import { usePagination } from '@/hooks/usePagination';
import { paths } from '@/routes/paths';
import { MembershipStatusBadge } from './MembershipStatusBadge';
import {
  REVIEW_STATUSES,
  REVIEW_STATUS_CONTENT,
  resolveReviewStatus,
} from './participationReviewStatusContent';

const INITIAL_ITEMS_PER_PAGE = 10;
const REVIEW_GRID_COLUMNS =
  'minmax(180px, 1fr) minmax(180px, 1fr) minmax(190px, 0.9fr) minmax(150px, 0.75fr)';

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

type ParticipationReviewListPanelProps = {
  projectId: string;
  /** Already narrowed to this project (see useProjectParticipationReviews). */
  reviewsByStatus: ParticipationReviewsByStatus;
  isLoading: boolean;
  isError: boolean;
  isForbidden: boolean;
  onRetry: () => void;
};

/** Lists one project's membership applications by status; embedded in the project detail 入会届 tab. */
export function ParticipationReviewListPanel({
  projectId,
  reviewsByStatus,
  isLoading,
  isError,
  isForbidden,
  onRetry,
}: Readonly<ParticipationReviewListPanelProps>) {
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigationState = resolveAdminProjectNavigationState(location.state);
  const activeStatus = resolveReviewStatus(searchParams.get('status'));
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
          <MembershipStatusBadge status={activeStatus}>
            {getStatusBadgeLabel(activeStatus, review)}
          </MembershipStatusBadge>
        ),
      },
    ],
    [activeStatus],
  );

  if (isForbidden) {
    return (
      <PermissionDeniedNotice permission="PARTICIPATING_COMPANY_APPLICATION_REVIEW" />
    );
  }

  if (isError) {
    return (
      <div className="space-y-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center">
        <p role="alert" className="text-small text-destructive">
          入会届一覧を取得できませんでした。
        </p>
        <ActionButton kind="retry" onClick={onRetry} />
      </div>
    );
  }

  return (
    <Tabs value={activeStatus} onValueChange={changeStatus} className="space-y-5">
      <SegmentTabsList aria-label="入会届のステータス">
        {REVIEW_STATUSES.map((status) => {
          const count = reviewsByStatus[status].length;

          return (
            <SegmentTabsTrigger
              key={status}
              value={status}
              tone={REVIEW_STATUS_CONTENT[status].tone}
            >
              {REVIEW_STATUS_CONTENT[status].label}
              <TabCount count={count} />
            </SegmentTabsTrigger>
          );
        })}
      </SegmentTabsList>

      {REVIEW_STATUSES.map((status) => (
        <TabsContent key={status} value={status} className="space-y-4">
          <p className="text-small text-foreground">
            {REVIEW_STATUS_CONTENT[status].description}
          </p>

          {isLoading ? (
            <SkeletonTable
              columns={4}
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
