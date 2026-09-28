import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import {
  getAdminProjectDetail,
  getParticipationReviewsByStatus,
  type ParticipationReviewDetail,
  type ParticipationReviewStatus,
} from '@/api/admin';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import {
  floatingPageBackButtonClassName,
  PageBackButton,
} from '@/components/navigation/PageBackButton';
import { NotFoundPage } from '@/components/feedback/NotFoundPage';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@/components/ui/tabs';
import { formatAdminCompanyDateTime } from '@/features/admin/company-detail/companyDetailDisplay';
import { usePagination } from '@/hooks/usePagination';
import { paths } from '@/routes/paths';
import { isParticipationReviewForProject } from './participationReviewProjectMatch';
import { isForbiddenApiError } from '@/api/_shared/errors';
import { PermissionDeniedNotice } from '@/features/admin/permissions/PermissionDeniedNotice';
import { resolveAdminProjectNavigationState } from '@/features/admin/navigation/adminProjectNavigation';

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

/** Resolves the membership status query to a supported tab. */
function resolveReviewStatus(value: string | null): ParticipationReviewStatus {
  return REVIEW_STATUSES.includes(value as ParticipationReviewStatus)
    ? (value as ParticipationReviewStatus)
    : 'OPEN';
}

/** Returns the badge text shown for a row in the selected status tab. */
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

/** Displays the project-scoped participation review list. */
export default function ParticipationReviewListPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { projectId = '' } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigationState = resolveAdminProjectNavigationState(location.state);
  const activeStatus = resolveReviewStatus(searchParams.get('status'));
  const [reviewsByStatus, setReviewsByStatus] = useState<
    Record<ParticipationReviewStatus, ParticipationReviewDetail[]>
  >(EMPTY_REVIEWS_BY_STATUS);
  const [isLoading, setIsLoading] = useState(true);
  const [hasLoadError, setHasLoadError] = useState(false);
  const [hasPermissionError, setHasPermissionError] = useState(false);

  /** Loads all membership statuses without comparing incompatible public and internal project IDs. */
  const loadReviews = useCallback(async () => {
    if (!projectId) {
      setHasLoadError(true);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setHasLoadError(false);
    setHasPermissionError(false);

    try {const [project, statusReviews] = await Promise.all([
        getAdminProjectDetail(projectId),
        getParticipationReviewsByStatus(),
      ]);

      setReviewsByStatus({
        OPEN: statusReviews.OPEN.filter((review) =>
          isParticipationReviewForProject(review, projectId, project),
        ),
        APPROVED: statusReviews.APPROVED.filter((review) =>
          isParticipationReviewForProject(review, projectId, project),
        ),
        REJECTED: statusReviews.REJECTED.filter((review) =>
          isParticipationReviewForProject(review, projectId, project),
        ),
      });
      // setReviewsByStatus(await getParticipationReviewsByStatus());
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

  const activeReviews = reviewsByStatus[activeStatus];
  const {
    currentPage,
    totalPages,
    totalItems,
    paginatedItems,
    goToPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(activeReviews, {
    itemsPerPage: INITIAL_ITEMS_PER_PAGE,
  });

  const viewDetail = useCallback(
    (review: ParticipationReviewDetail) => {
      navigate(
        paths.adminProjectMembershipDetail(
          projectId,
          // String(review.projectId),
          review.participationId,
          activeStatus,
        ),
        { state: navigationState },
      );
    },
    [activeStatus, navigate, navigationState, projectId],
    // [activeStatus, navigate],
  );

  const columns = useMemo<
    DataGridTableColumn<ParticipationReviewDetail>[]
  >(
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
          <div
            onClick={(event) => event.stopPropagation()}
            onPointerDown={(event) => event.stopPropagation()}
          >
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => viewDetail(review)}
            >
              詳細
            </Button>
          </div>
        ),
      },
    ],
    [activeStatus, viewDetail],
  );

  /** Returns to Admin Home when this list was opened from its project summary table. */
  const returnToProject = () =>
    navigate(navigationState?.returnTo ?? paths.adminProjectDetail(projectId));

  if (hasPermissionError) {
    return (
      <main className="mx-auto w-full max-w-[1120px] space-y-6 px-4 py-10 sm:px-6">
        <header><h1>入会届一覧</h1><p className="mt-2 text-small text-foreground">レビュー対象となる入会届を確認します。</p></header>
        <PermissionDeniedNotice permission="PARTICIPATING_COMPANY_APPLICATION_REVIEW" />
      </main>
    );
  }

  if (hasLoadError) {
    return <NotFoundPage />;
  }

  return (
    <main className="relative min-h-screen bg-background px-4 py-10 sm:px-6 lg:px-8">
      <PageBackButton
        onClick={returnToProject}
        label=""
        aria-label="プロジェクト詳細へ戻る"
        className={floatingPageBackButtonClassName}
      />

      <div className="mx-auto w-full max-w-[1120px] space-y-8 pt-16 sm:pt-0">
        <header className="space-y-2">
          <h1>入会届一覧</h1>
          <p className="text-small text-foreground">
            レビュー対象となる入会届を確認します。
          </p>
        </header>

        <Tabs
          value={activeStatus}
          onValueChange={(value) => {
            const nextStatus = resolveReviewStatus(value);
            const nextSearchParams = new URLSearchParams(searchParams);
            nextSearchParams.set('status', nextStatus);
            setSearchParams(nextSearchParams);
            goToPage(1);
          }}
          className="space-y-5"
        >
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
                <div className="rounded-xl border border-border bg-card py-12 text-center text-small text-foreground">
                  入会届一覧を読み込んでいます...
                </div>
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
      </div>
    </main>
  );
}
