import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  getMyParticipationReviews,
  getPendingParticipationReviews,
  type ParticipationReviewDetail,
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
import { getParticipationReviewStage } from './participationReviewDisplay';

const INITIAL_ITEMS_PER_PAGE = 10;
const REVIEW_GRID_COLUMNS =
  'minmax(180px, 1fr) minmax(180px, 1fr) minmax(190px, 0.9fr) minmax(150px, 0.75fr) minmax(110px, 0.5fr)';

type ReviewTab = 'mine' | 'pending';

const REVIEW_TAB_DESCRIPTIONS: Record<ReviewTab, string> = {
  mine: 'あなたがまだ承認していない、対応が必要な入会届を表示しています。',
  pending:
    '他の管理者の承認待ちを含む、最終承認前の入会届をすべて表示しています。',
};

/** Displays the project-scoped participation review list. */
export default function ParticipationReviewListPage() {
  const navigate = useNavigate();
  const { projectId = '' } = useParams();
  const [activeTab, setActiveTab] = useState<ReviewTab>('mine');
  const [myReviews, setMyReviews] = useState<ParticipationReviewDetail[]>([]);
  const [pendingReviews, setPendingReviews] = useState<
    ParticipationReviewDetail[]
  >([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasLoadError, setHasLoadError] = useState(false);

  /** Loads both review scopes and switches failures to the shared 404 state. */
  const loadReviews = useCallback(async () => {
    if (!projectId) {
      setHasLoadError(true);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setHasLoadError(false);

    try {
      const [mine, pending] = await Promise.all([
        getMyParticipationReviews(),
        getPendingParticipationReviews(),
      ]);

      setMyReviews(mine);
      setPendingReviews(pending);
    } catch {
      setHasLoadError(true);
    } finally {
      setIsLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    void loadReviews();
  }, [loadReviews]);

  const activeReviews = activeTab === 'mine' ? myReviews : pendingReviews;
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
          String(review.projectId),
          review.participationId,
        ),
      );
    },
    [navigate],
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
        id: 'stage',
        label: 'レビュー段階',
        sortable: true,
        sortValue: getParticipationReviewStage,
        render: (review) => (
          <Badge
            variant="outline"
            className="border-primary/30 bg-primary/5 text-primary"
          >
            {getParticipationReviewStage(review)}
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
    [viewDetail],
  );

  const returnToProject = () => navigate(paths.adminProjectDetail(projectId));

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
          value={activeTab}
          onValueChange={(value) => {
            setActiveTab(value as ReviewTab);
            goToPage(1);
          }}
          className="space-y-5"
        >
          <TabsList className="grid h-auto w-full grid-cols-2 sm:w-fit">
            <TabsTrigger value="mine">
              自分の承認待ち（{myReviews.length}）
            </TabsTrigger>
            <TabsTrigger value="pending">
              承認待ち全件（{pendingReviews.length}）
            </TabsTrigger>
          </TabsList>

          {(['mine', 'pending'] as const).map((tab) => (
            <TabsContent key={tab} value={tab} className="space-y-4">
              <p className="text-small text-foreground">
                {REVIEW_TAB_DESCRIPTIONS[tab]}
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
                    emptyMessage={
                      tab === 'mine'
                        ? '現在、あなたの承認待ちの入会届はありません。'
                        : '承認待ちの入会届はありません。'
                    }
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
