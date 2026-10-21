import { useLayoutEffect, useRef, useState, type CSSProperties } from 'react';
import {
  ArrowRight,
  Building2,
  CheckCircle2,
  ClipboardCheck,
  FolderKanban,
  RotateCcw,
} from 'lucide-react';
import type {
  AdminCompanyListItem,
  ParticipationReviewDetail,
} from '@/api/admin';
import type { BatchHistoryItem, BatchHistoryPagination } from '@/api/batch';
import type { MonitoringCancelRequestListItem } from '@/api/monitoring';
import type { Project } from '@/api/projects';
import { RowActionButton } from '@/components/data-display/RowActionButton';
import { CompanyStatusBadge } from '@/features/admin/company-detail/CompanyStatusBadge';
import { ErrorBoundary } from '@/components/feedback/ErrorBoundary';
import { NoticeBanner } from '@/components/feedback/NoticeBanner';
import { NotificationsList } from '@/components/feedback/NotificationsList';
import { AdminPageHeader } from '@/components/layout/AdminPageHeader';
import { AdminTableCard } from '@/components/layout/AdminTableCard';
import {
  SkeletonList,
  SkeletonMetricCard,
} from '@/components/feedback/loading';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { formatAdminCompanyDateTime } from '@/features/admin/company-detail/companyDetailDisplay';
import { AdminProjectStatusTable } from '@/features/admin/dashboard/tables/AdminProjectStatusTable';
import { BatchHistoryStatusBadge } from '@/features/admin/dashboard/tables/BatchHistoryStatusBadge';
import { BatchHistoryTable } from '@/features/admin/dashboard/tables/BatchHistoryTable';
import { CancelRequestHistoryTable } from '@/features/admin/dashboard/tables/CancelRequestHistoryTable';
import { formatBatchHistoryDateTime } from '@/features/admin/dashboard/tables/batchHistoryFormatters';
import type { AdminOperatorPermissionCode } from '@/api/admin/operators';
import { PermissionDeniedNotice } from '@/features/admin/permissions/PermissionDeniedNotice';
import { useNotifications } from '@/hooks/useNotifications';
import { toast } from '@/hooks/use-toast';
import { ActionButton } from '@/components/actions';

export type AdminHomePanel = 'cancel' | 'batch';

/** Matches AdminTableCard headings so every Home panel shares one title size. */
const panelTitleClassName = 'text-subsection-title font-bold';

/** Tracks an element's rendered height so adjacent desktop content can align to it. */
function useElementHeight<TElement extends HTMLElement>() {
  const elementRef = useRef<TElement>(null);
  const [height, setHeight] = useState(0);

  useLayoutEffect(() => {
    const element = elementRef.current;

    if (!element) return;

    /** Stores a rounded height to avoid updates caused only by sub-pixel changes. */
    const updateHeight = () => {
      const nextHeight = Math.ceil(element.getBoundingClientRect().height);
      setHeight((currentHeight) =>
        currentHeight === nextHeight ? currentHeight : nextHeight,
      );
    };

    updateHeight();

    if (typeof ResizeObserver === 'undefined') return;

    const resizeObserver = new ResizeObserver(updateHeight);
    resizeObserver.observe(element);

    return () => resizeObserver.disconnect();
  }, []);

  return { elementRef, height };
}

type AdminDashboardOverviewProps = {
  projects: Project[];
  projectsLoading: boolean;
  projectsError: string | null;
  openMembershipReviews: ParticipationReviewDetail[];
  openMembershipReviewsError: string | null;
  applicationTotal: number;
  applicationsLoading: boolean;
  applicationsError: string | null;
  pendingApplications: AdminCompanyListItem[];
  pendingApplicationTotal: number;
  pendingApplicationsLoading: boolean;
  pendingApplicationsError: string | null;
  rejectedApplications: AdminCompanyListItem[];
  rejectedApplicationTotal: number;
  rejectedApplicationsLoading: boolean;
  rejectedApplicationsError: string | null;
  participatingCompanyTotal: number;
  participatingCompaniesLoading: boolean;
  participatingCompaniesError: string | null;
  cancelRequests: MonitoringCancelRequestListItem[];
  cancelRequestsLoading: boolean;
  cancelRequestsError: string | null;
  batchHistory: BatchHistoryItem[];
  batchHistoryPagination: BatchHistoryPagination;
  batchHistoryLoading: boolean;
  batchHistoryError: string | null;
  activePanel: AdminHomePanel | null;
  onPanelChange: (panel: AdminHomePanel | null) => void;
  onOpenProjectDetail: (project: Project) => void;
  onOpenProjectMembership: (project: Project) => void;
  onOpenCompanyApplication: (company: AdminCompanyListItem) => void;
  onOpenCancelRequest: (request: MonitoringCancelRequestListItem) => void;
  onNavigateProjects: () => void;
  onNavigateCompanies: () => void;
  onNavigateApplications: () => void;
  canReadProjects: boolean;
  canReadCompanies: boolean;
  canReviewCompanies: boolean;
  canCancelMonitoring: boolean;
  canReadBatchHistory: boolean;
};

type MetricCardProps = {
  label: string;
  value: number;
  description?: string;
  icon: React.ReactNode;
  loading: boolean;
  error: string | null;
  onClick: () => void;
  permissionGranted: boolean;
  requiredPermission: AdminOperatorPermissionCode;
};

/** Renders one independently loading metric card; the whole card opens its destination. */
function MetricCard({
  label,
  value,
  description,
  // icon,
  loading,
  error,
  onClick,
  permissionGranted,
  requiredPermission,
}: Readonly<MetricCardProps>) {
  if (loading) {
    return <SkeletonMetricCard className='min-h-[6.5rem]' />;
  }

  const header = (
    <span className='flex items-start justify-between gap-3'>
      {/* Same anatomy as the master MasterStatCard: description on its own line, number pinned below. */}
      <span className="flex min-w-0 flex-col gap-0.5">
        <span className="text-body !font-bold text-foreground">
          {label}
        </span>

        {description && (
          <span className="text-subtle text-muted-foreground">
            {description}
          </span>
        )}
      </span>
      {permissionGranted && (
        <ArrowRight
          className='mt-0.5 h-4 w-4 shrink-0 text-primary-strong transition-transform group-hover:translate-x-0.5'
          aria-hidden='true'
        />
      )}
    </span>
  );

  if (!permissionGranted || error) {
    return (
      <Card className='h-full rounded-lg border-border'>
        <div className='flex h-full flex-col gap-3 p-card'>
          {header}
          {!permissionGranted ? (
            <PermissionDeniedNotice
              permission={requiredPermission}
              compact
              className='w-full'
            />
          ) : (
            <div
              role='alert'
              className='w-full rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3 text-small text-destructive-strong'
            >
              取得できませんでした
            </div>
          )}
        </div>
      </Card>
    );
  }

  return (
    <Card className='h-full rounded-lg border-border transition-colors hover:border-primary/40'>
      <button
        type='button'
        onClick={onClick}
        className='group flex h-full w-full flex-col gap-3 rounded-lg p-card text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'
      >
        {header}
        <span className='mt-auto flex items-baseline gap-1.5'>
          <span className='text-metric font-bold leading-none tracking-tight tabular-nums text-foreground'>
            {value.toLocaleString('ja-JP')}
          </span>
          <span className='text-base font-bold text-foreground'>件</span>
        </span>
      </button>
    </Card>
  );
}

/** Sorts dated records newest first while keeping invalid dates stable. */
function compareNewestFirst(leftValue: string, rightValue: string) {
  const leftTime = new Date(leftValue).getTime();
  const rightTime = new Date(rightValue).getTime();
  return (
    (Number.isNaN(rightTime) ? 0 : rightTime) -
    (Number.isNaN(leftTime) ? 0 : leftTime)
  );
}

/** Renders actionable applications first and fills remaining rows with recent history. */
function CompanyApplicationSummary({
  pendingApplications,
  pendingTotal,
  pendingLoading,
  pendingError,
  rejectedApplications,
  rejectedTotal,
  rejectedLoading,
  rejectedError,
  onOpen,
  onOpenAll,
  permissionGranted,
}: Readonly<{
  pendingApplications: AdminCompanyListItem[];
  pendingTotal: number;
  pendingLoading: boolean;
  pendingError: string | null;
  rejectedApplications: AdminCompanyListItem[];
  rejectedTotal: number;
  rejectedLoading: boolean;
  rejectedError: string | null;
  onOpen: (company: AdminCompanyListItem) => void;
  onOpenAll: () => void;
  permissionGranted: boolean;
}>) {
  const recentApplications = [
    ...[...pendingApplications].sort((left, right) =>
      compareNewestFirst(left.createdAt, right.createdAt),
    ),
    ...[...rejectedApplications].sort((left, right) =>
      compareNewestFirst(left.createdAt, right.createdAt),
    ),
  ].slice(0, 3);
  const isLoading = pendingLoading || rejectedLoading;
  const hasError = Boolean(pendingError || rejectedError);

  return (
    <Card className='min-h-72 rounded-lg border-border'>
      <CardHeader className='flex-row items-center justify-between space-y-0'>
        <CardTitle className={panelTitleClassName}>企業申請</CardTitle>
        <ActionButton kind='link' onClick={onOpenAll}>
          すべて見る
        </ActionButton>
      </CardHeader>
      <CardContent className='space-y-4'>
        {!permissionGranted ? (
          <PermissionDeniedNotice permission='PARTICIPATING_COMPANY_APPLICATION_REVIEW' />
        ) : (
          <>
            <div className='grid grid-cols-2 gap-3'>
              <div className='rounded-lg bg-muted px-4 py-3'>
                <p className='text-subtle text-muted-foreground'>確認待ち</p>
                <p className='mt-1 text-xl font-bold text-foreground'>
                  {pendingLoading || pendingError
                    ? '—'
                    : `${pendingTotal.toLocaleString('ja-JP')}件`}
                </p>
              </div>
              <div className='rounded-lg bg-muted px-4 py-3'>
                <p className='text-subtle text-muted-foreground'>却下</p>
                <p className='mt-1 text-xl font-bold text-foreground'>
                  {rejectedLoading || rejectedError
                    ? '—'
                    : `${rejectedTotal.toLocaleString('ja-JP')}件`}
                </p>
              </div>
            </div>

            {!pendingLoading && !pendingError && pendingTotal === 0 && (
              <div className='flex items-center gap-2 rounded-lg border border-success-border bg-success-subtle px-4 py-3 text-small text-success-strong'>
                <CheckCircle2
                  className='h-4 w-4 shrink-0'
                  aria-hidden='true'
                />
                現在、確認が必要な企業申請はありません。
              </div>
            )}

            {hasError && (
              <div
                className='space-y-1'
                role='alert'
              >
                {pendingError && (
                  <p className='text-subtle text-destructive-strong'>{pendingError}</p>
                )}
                {rejectedError && (
                  <p className='text-subtle text-destructive-strong'>
                    {rejectedError}
                  </p>
                )}
              </div>
            )}

            {isLoading ? (
              <SkeletonList items={3} />
            ) : recentApplications.length > 0 ? (
              <ul className='divide-y divide-border'>
                {recentApplications.map((company) => {
                  const isPending = company.status === 'PENDING';

                  return (
                    <li
                      key={company.id ?? company.companyId}
                      className='flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between'
                    >
                      <div className='min-w-0'>
                        <div className='flex flex-wrap items-center gap-2'>
                          <p className='truncate text-body font-bold text-foreground'>
                            {company.nameJA || '企業名未登録'}
                          </p>
                          <CompanyStatusBadge status={company.status} />
                        </div>
                        <p className='mt-1 text-subtle text-muted-foreground'>
                          受付日{' '}
                          {formatAdminCompanyDateTime(company.createdAt) || '—'}
                        </p>
                      </div>
                      {isPending ? (
                        <RowActionButton
                          intent='review'
                          className='self-start sm:self-auto'
                          onClick={() => onOpen(company)}
                        >
                          申請内容を確認
                        </RowActionButton>
                      ) : (
                        <RowActionButton
                          className='self-start sm:self-auto'
                          onClick={() => onOpen(company)}
                        />
                      )}
                    </li>
                  );
                })}
              </ul>
            ) : !hasError ? (
              <p className='py-3 text-center text-small text-muted-foreground'>
                企業申請の履歴はありません。
              </p>
            ) : null}
          </>
        )}
      </CardContent>
    </Card>
  );
}

/**
 * Announces pending monitoring-data cancel requests. Renders nothing when none
 * are waiting; load errors are shown by the データ取消依頼 metric card.
 */
function CancelRequestBanner({
  pendingCount,
  loading,
  error,
  onOpen,
}: Readonly<{
  pendingCount: number;
  loading: boolean;
  error: string | null;
  onOpen: () => void;
}>) {
  if (loading || error || pendingCount === 0) return null;

  return (
    <NoticeBanner
      tone='warning'
      action={
        <ActionButton kind='review' onClick={onOpen}>
          確認する
        </ActionButton>
      }
    >
      未処理のモニタリングデータ取消依頼が{pendingCount.toLocaleString('ja-JP')}件あります。
    </NoticeBanner>
  );
}

/** Renders the newest batch execution summary. */
function BatchHistorySummary({
  items,
  loading,
  error,
  onOpenPanel,
  permissionGranted,
}: Readonly<{
  items: BatchHistoryItem[];
  loading: boolean;
  error: string | null;
  onOpenPanel: () => void;
  permissionGranted: boolean;
}>) {
  const latestBatch = [...items].sort((left, right) =>
    compareNewestFirst(left.start_time, right.start_time),
  )[0];

  return (
    <Card className='rounded-lg border-border'>
      <CardHeader className='flex-row items-center justify-between space-y-0'>
        <CardTitle className={panelTitleClassName}>バッチ処理</CardTitle>
        <ActionButton kind='link' onClick={onOpenPanel}>
          履歴を見る
        </ActionButton>
      </CardHeader>
      <CardContent>
        {!permissionGranted ? (
          <PermissionDeniedNotice permission='MONITORING_BATCH_HISTORY_READ' />
        ) : loading ? (
          <SkeletonList items={1} />
        ) : error ? (
          <p
            role='alert'
            className='py-6 text-center text-small text-destructive-strong'
          >
            {error}
          </p>
        ) : !latestBatch ? (
          <p className='py-6 text-center text-small text-foreground'>
            バッチ処理履歴はありません。
          </p>
        ) : (
          <div className='flex items-center justify-between gap-4 rounded-lg bg-muted p-4'>
            <div>
              <p className='text-subtle text-foreground'>最新の開始日時</p>
              <p className='mt-2 text-body font-bold text-foreground'>
                {formatBatchHistoryDateTime(latestBatch.start_time)}
              </p>
            </div>
            <BatchHistoryStatusBadge status={latestBatch.status} />
          </div>
        )}
      </CardContent>
    </Card>
  );
}

/** Renders the notification feed from the shared navbar store without refetching it. */
function NotificationSummary() {
  const { notifications, loading, error, markAsRead, deleteNotification } =
    useNotifications();

  /** Marks one notification as read and keeps failures local to this card. */
  const handleMarkAsRead = async (
    notificationId: Parameters<typeof markAsRead>[0],
  ) => {
    try {
      await markAsRead(notificationId);
    } catch {
      toast({
        title: 'エラー',
        description: '通知を既読にできませんでした。',
        variant: 'destructive',
      });
    }
  };

  /** Deletes one notification and keeps failures local to this card. */
  const handleDeleteNotification = async (
    notificationId: Parameters<typeof deleteNotification>[0],
  ) => {
    try {
      await deleteNotification(notificationId);
    } catch {
      toast({
        title: 'エラー',
        description: '通知を削除できませんでした。',
        variant: 'destructive',
      });
    }
  };

  return (
    <Card className='rounded-lg border-border xl:flex xl:min-h-0 xl:flex-1 xl:flex-col xl:overflow-hidden'>
      <CardHeader className='shrink-0'>
        <CardTitle className={panelTitleClassName}>お知らせ</CardTitle>
      </CardHeader>
      <CardContent className='min-h-0 xl:flex-1 xl:overflow-y-auto'>
        <NotificationsList
          notifications={notifications}
          loading={loading}
          error={error}
          onMarkAsRead={handleMarkAsRead}
          onDelete={handleDeleteNotification}
        />
      </CardContent>
    </Card>
  );
}

/** Renders URL-driven monitoring dialogs using the existing full table components. */
function MonitoringDialogs({
  activePanel,
  onPanelChange,
  cancelRequests,
  cancelRequestsLoading,
  cancelRequestsError,
  batchHistory,
  batchHistoryPagination,
  batchHistoryLoading,
  batchHistoryError,
  onOpenCancelRequest,
}: Readonly<
  Pick<
    AdminDashboardOverviewProps,
    | 'activePanel'
    | 'onPanelChange'
    | 'cancelRequests'
    | 'cancelRequestsLoading'
    | 'cancelRequestsError'
    | 'batchHistory'
    | 'batchHistoryPagination'
    | 'batchHistoryLoading'
    | 'batchHistoryError'
    | 'onOpenCancelRequest'
  >
>) {
  return (
    <>
      <Dialog
        open={activePanel === 'cancel'}
        onOpenChange={(open) => onPanelChange(open ? 'cancel' : null)}
      >
        <DialogContent className='max-h-[90dvh] max-w-[min(96vw,80rem)] overflow-y-auto'>
          <DialogHeader>
            <DialogTitle>取消一覧</DialogTitle>
            <DialogDescription>
              取消対象のモニタリングデータを確認できます。
            </DialogDescription>
          </DialogHeader>
          <ErrorBoundary>
            <CancelRequestHistoryTable
              initialRequests={cancelRequests}
              initialLoading={cancelRequestsLoading}
              initialLoadError={cancelRequestsError}
              onViewRequest={onOpenCancelRequest}
            />
          </ErrorBoundary>
        </DialogContent>
      </Dialog>
      <Dialog
        open={activePanel === 'batch'}
        onOpenChange={(open) => onPanelChange(open ? 'batch' : null)}
      >
        <DialogContent className='max-h-[90dvh] max-w-[min(96vw,80rem)] overflow-y-auto'>
          <DialogHeader>
            <DialogTitle>バッチ処理</DialogTitle>
            <DialogDescription>
              バッチ処理の実行状況と履歴を確認できます。
            </DialogDescription>
          </DialogHeader>
          <BatchHistoryTable
            initialItems={batchHistory}
            initialPagination={batchHistoryPagination}
            initialLoading={batchHistoryLoading}
            initialLoadError={batchHistoryError}
          />
        </DialogContent>
      </Dialog>
    </>
  );
}

/** Renders the Figma-aligned Admin Home from independent existing data sources. */
/** Renders the admin overview metrics, tables, and supporting panels. */
export function AdminDashboardOverview(
  props: Readonly<AdminDashboardOverviewProps>,
) {
  const { elementRef: mainColumnRef, height: mainColumnHeight } =
    useElementHeight<HTMLDivElement>();
  const pendingCancelCount = props.cancelRequests.filter(
    (request) => request.status.toUpperCase() === 'PENDING',
  ).length;
  const permittedActivePanel =
    (props.activePanel === 'cancel' && props.canCancelMonitoring) ||
    (props.activePanel === 'batch' && props.canReadBatchHistory)
      ? props.activePanel
      : null;
  const alignedSidebarStyle =
    mainColumnHeight > 0
      ? ({
          '--admin-home-main-column-height': `${mainColumnHeight}px`,
        } as CSSProperties)
      : undefined;

  return (
    <div className='space-y-section'>
      <AdminPageHeader
        title='ホーム'
        // description="運用業務を担当するユーザーのアカウントを管理します。"
      />
      {props.canCancelMonitoring && (
        <CancelRequestBanner
          pendingCount={pendingCancelCount}
          loading={props.cancelRequestsLoading}
          error={props.cancelRequestsError}
          onOpen={() => props.onPanelChange('cancel')}
        />
      )}
      <section
        aria-label='主要な登録状況'
        className='
          grid grid-cols-2 items-stretch
          gap-3
          sm:gap-grid
          lg:grid-cols-4
        '
      >
        <MetricCard
          label='プロジェクト'
          value={props.projects.length}
          description='登録済み'
          icon={<FolderKanban />}
          loading={props.canReadProjects && props.projectsLoading}
          error={props.projectsError}
          onClick={props.onNavigateProjects}
          permissionGranted={props.canReadProjects}
          requiredPermission='PROJECT_READ'
        />
        <MetricCard
          label='参加企業'
          value={props.participatingCompanyTotal}
          description='稼働中・一時停止中'
          icon={<Building2 />}
          loading={
            props.canReadCompanies && props.participatingCompaniesLoading
          }
          error={props.participatingCompaniesError}
          onClick={props.onNavigateCompanies}
          permissionGranted={props.canReadCompanies}
          requiredPermission='PARTICIPATING_COMPANY_READ'
        />
        <MetricCard
          label='企業申請'
          value={props.applicationTotal}
          description='申請中・却下'
          icon={<ClipboardCheck />}
          loading={props.canReviewCompanies && props.applicationsLoading}
          error={props.applicationsError}
          onClick={props.onNavigateApplications}
          permissionGranted={props.canReviewCompanies}
          requiredPermission='PARTICIPATING_COMPANY_APPLICATION_REVIEW'
        />
        <MetricCard
          label='データ取消依頼'
          value={pendingCancelCount}
          description='確認待ち'
          icon={<RotateCcw />}
          loading={props.canCancelMonitoring && props.cancelRequestsLoading}
          error={props.cancelRequestsError}
          onClick={() => props.onPanelChange('cancel')}
          permissionGranted={props.canCancelMonitoring}
          requiredPermission='MONITORING_CANCEL'
        />
      </section>
      <div
        className='
          grid
          items-start
          gap-grid
          xl:grid-cols-[minmax(0,1.65fr)_minmax(18rem,0.8fr)]
        '
      >
        <div
          ref={mainColumnRef}
          className='min-w-0 space-y-grid'
        >
          <AdminTableCard
            title='プロジェクト一覧'
            count={props.projects.length}
            // description='確認が必要な入会届が多いプロジェクトから表示します。'
          >
            {props.canReadProjects ? (
              <AdminProjectStatusTable
                projects={props.projects}
                openReviews={props.openMembershipReviews}
                projectsLoading={props.projectsLoading}
                projectsError={props.projectsError}
                openReviewsError={props.openMembershipReviewsError}
                onOpenProjectDetail={props.onOpenProjectDetail}
                onOpenMembership={props.onOpenProjectMembership}
              />
            ) : (
              <PermissionDeniedNotice permission='PROJECT_READ' />
            )}
          </AdminTableCard>
          <CompanyApplicationSummary
            pendingApplications={props.pendingApplications}
            pendingTotal={props.pendingApplicationTotal}
            pendingLoading={props.pendingApplicationsLoading}
            pendingError={props.pendingApplicationsError}
            rejectedApplications={props.rejectedApplications}
            rejectedTotal={props.rejectedApplicationTotal}
            rejectedLoading={props.rejectedApplicationsLoading}
            rejectedError={props.rejectedApplicationsError}
            onOpen={props.onOpenCompanyApplication}
            onOpenAll={props.onNavigateApplications}
            permissionGranted={props.canReviewCompanies}
          />
        </div>
        <div
          style={alignedSidebarStyle}
          className='flex min-w-0 flex-col gap-grid xl:h-[var(--admin-home-main-column-height)] xl:min-h-0 xl:max-h-[var(--admin-home-main-column-height)] xl:overflow-hidden'
        >
          <NotificationSummary />
          <BatchHistorySummary
            items={props.batchHistory}
            loading={props.batchHistoryLoading}
            error={props.batchHistoryError}
            onOpenPanel={() => props.onPanelChange('batch')}
            permissionGranted={props.canReadBatchHistory}
          />
        </div>
      </div>
      <MonitoringDialogs
        activePanel={permittedActivePanel}
        onPanelChange={props.onPanelChange}
        cancelRequests={props.cancelRequests}
        cancelRequestsLoading={props.cancelRequestsLoading}
        cancelRequestsError={props.cancelRequestsError}
        batchHistory={props.batchHistory}
        batchHistoryPagination={props.batchHistoryPagination}
        batchHistoryLoading={props.batchHistoryLoading}
        batchHistoryError={props.batchHistoryError}
        onOpenCancelRequest={props.onOpenCancelRequest}
      />
    </div>
  );
}
