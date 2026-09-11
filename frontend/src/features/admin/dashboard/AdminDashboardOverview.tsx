import { ArrowRight, Building2, CheckCircle2, ClipboardCheck, FolderKanban, RotateCcw } from 'lucide-react';
import type { AdminCompanyListItem, ParticipationReviewDetail } from '@/api/admin';
import type { BatchHistoryItem, BatchHistoryPagination } from '@/api/batch';
import type { MonitoringCancelRequestListItem } from '@/api/monitoring';
import type { Project } from '@/api/projects';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { ErrorBoundary } from '@/components/feedback/ErrorBoundary';
import { SkeletonList, SkeletonMetricCard } from '@/components/feedback/loading';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { formatAdminCompanyDateTime } from '@/features/admin/company-detail/companyDetailDisplay';
import { AdminProjectStatusTable } from '@/features/admin/dashboard/tables/AdminProjectStatusTable';
import { BatchHistoryStatusBadge } from '@/features/admin/dashboard/tables/BatchHistoryStatusBadge';
import { BatchHistoryTable } from '@/features/admin/dashboard/tables/BatchHistoryTable';
import { CancelRequestHistoryTable } from '@/features/admin/dashboard/tables/CancelRequestHistoryTable';
import { formatBatchHistoryDateTime } from '@/features/admin/dashboard/tables/batchHistoryFormatters';
import { CancelRequestStatusBadge } from '@/features/admin/monitoring/CancelRequestStatusBadge';
import { formatCancelRequestDateTime } from '@/features/admin/monitoring/cancelRequestFormatters';

export type AdminHomePanel = 'cancel' | 'batch';

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
  onOpenProjectMembership: (project: Project) => void;
  onOpenCompanyApplication: (company: AdminCompanyListItem) => void;
  onOpenCancelRequest: (request: MonitoringCancelRequestListItem) => void;
  onNavigateProjects: () => void;
  onNavigateCompanies: () => void;
};

type MetricCardProps = {
  label: string;
  value: number;
  description: string;
  icon: React.ReactNode;
  loading: boolean;
  error: string | null;
  onClick: () => void;
};

/** Renders one independently loading metric card. */
function MetricCard({ label, value, description, icon, loading, error, onClick }: Readonly<MetricCardProps>) {
  if (loading) return <SkeletonMetricCard />;

  return (
    <Card className="rounded-xl border-border shadow-app-sm">
      <CardContent className="flex min-h-36 items-start justify-between gap-4 p-5">
        <div className="min-w-0">
          <p className="text-small text-foreground">{label}</p>
          {error ? (
            <p role="alert" className="mt-3 text-small text-destructive">取得できませんでした</p>
          ) : (
            <p className="mt-3 text-3xl font-bold leading-none text-foreground">
              {value.toLocaleString('ja-JP')}<span className="ml-1 text-sm font-medium text-foreground">件</span>
            </p>
          )}
          <p className="mt-3 text-subtle text-foreground">{description}</p>
        </div>
        <div className="flex flex-col items-end gap-8">
          <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-accent text-primary" aria-hidden="true">{icon}</span>
          <Button type="button" variant="ghost" size="sm" onClick={onClick} className="px-1 text-primary-strong">確認 <ArrowRight /></Button>
        </div>
      </CardContent>
    </Card>
  );
}

/** Sorts dated records newest first while keeping invalid dates stable. */
function compareNewestFirst(leftValue: string, rightValue: string) {
  const leftTime = new Date(leftValue).getTime();
  const rightTime = new Date(rightValue).getTime();
  return (Number.isNaN(rightTime) ? 0 : rightTime) - (Number.isNaN(leftTime) ? 0 : leftTime);
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
}>) {
  const recentApplications = [
    ...[...pendingApplications]
      .sort((left, right) => compareNewestFirst(left.createdAt, right.createdAt)),
    ...[...rejectedApplications]
      .sort((left, right) => compareNewestFirst(left.createdAt, right.createdAt)),
  ].slice(0, 3);
  const isLoading = pendingLoading || rejectedLoading;
  const hasError = Boolean(pendingError || rejectedError);

  return (
    <Card className="min-h-72 rounded-xl border-border shadow-app-sm">
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle>企業申請</CardTitle>
        <Button type="button" variant="ghost" size="sm" onClick={onOpenAll} className="text-primary-strong">すべて見る <ArrowRight /></Button>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-lg border border-border bg-muted/20 px-4 py-3">
            <p className="text-subtle text-muted-foreground">確認待ち</p>
            <p className="mt-1 text-xl font-bold text-foreground">{pendingLoading || pendingError ? '—' : `${pendingTotal.toLocaleString('ja-JP')}件`}</p>
          </div>
          <div className="rounded-lg border border-border bg-muted/20 px-4 py-3">
            <p className="text-subtle text-muted-foreground">却下</p>
            <p className="mt-1 text-xl font-bold text-foreground">{rejectedLoading || rejectedError ? '—' : `${rejectedTotal.toLocaleString('ja-JP')}件`}</p>
          </div>
        </div>

        {!pendingLoading && !pendingError && pendingTotal === 0 && (
          <div className="flex items-center gap-2 rounded-lg border border-success-border bg-success-subtle px-4 py-3 text-small text-success-strong">
            <CheckCircle2 className="h-4 w-4 shrink-0" aria-hidden="true" />
            現在、確認が必要な企業申請はありません。
          </div>
        )}

        {hasError && (
          <div className="space-y-1" role="alert">
            {pendingError && <p className="text-subtle text-destructive">{pendingError}</p>}
            {rejectedError && <p className="text-subtle text-destructive">{rejectedError}</p>}
          </div>
        )}

        {isLoading ? <SkeletonList items={3} /> : recentApplications.length > 0 ? (
          <ul className="divide-y divide-border">
            {recentApplications.map((company) => {
              const isPending = company.status === 'PENDING';

              return (
                <li key={company.id ?? company.companyId} className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="truncate text-body font-bold text-foreground">{company.nameJA || '企業名未登録'}</p>
                      <StatusBadge tone={isPending ? 'warning' : 'danger'}>{isPending ? '申請受付' : '却下'}</StatusBadge>
                    </div>
                    <p className="mt-1 text-subtle text-muted-foreground">受付日 {formatAdminCompanyDateTime(company.createdAt) || '—'}</p>
                  </div>
                  <Button type="button" variant="link" size="sm" onClick={() => onOpen(company)}>
                    {isPending ? '確認' : '詳細'} <ArrowRight />
                  </Button>
                </li>
              );
            })}
          </ul>
        ) : !hasError ? (
          <p className="py-3 text-center text-small text-muted-foreground">企業申請の履歴はありません。</p>
        ) : null}
      </CardContent>
    </Card>
  );
}

/** Renders a compact cancellation summary that opens the full table dialog. */
function CancelRequestSummary({ requests, loading, error, onOpenPanel, onOpenRequest }: Readonly<{
  requests: MonitoringCancelRequestListItem[];
  loading: boolean;
  error: string | null;
  onOpenPanel: () => void;
  onOpenRequest: (request: MonitoringCancelRequestListItem) => void;
}>) {
  const pendingRequests = requests
    .filter((request) => request.status.toUpperCase() === 'PENDING')
    .sort((left, right) => compareNewestFirst(left.requestedAt, right.requestedAt));

  return (
    <Card className="rounded-xl border-border shadow-app-sm">
      <CardHeader className="flex-row items-start justify-between space-y-0">
        <div><CardTitle>取消一覧</CardTitle><p className="mt-2 text-subtle text-foreground">未処理 {pendingRequests.length.toLocaleString('ja-JP')}件</p></div>
        <Button type="button" variant="ghost" size="sm" onClick={onOpenPanel} className="text-primary-strong">すべて見る <ArrowRight /></Button>
      </CardHeader>
      <CardContent>
        {loading ? <SkeletonList items={2} /> : error ? (
          <p role="alert" className="py-6 text-center text-small text-destructive">{error}</p>
        ) : pendingRequests.length === 0 ? (
          <p className="py-6 text-center text-small text-foreground">未処理の取消依頼はありません。</p>
        ) : (
          <ul className="divide-y divide-border">
            {pendingRequests.slice(0, 3).map((request) => (
              <li key={request.id} className="flex items-center justify-between gap-4 py-4">
                <button type="button" onClick={() => onOpenRequest(request)} className="min-w-0 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
                  <p className="truncate text-body font-bold text-foreground">{request.siteName || '拠点名未登録'}</p>
                  <p className="mt-1 text-subtle text-foreground">{formatCancelRequestDateTime(request.requestedAt)}</p>
                </button>
                <CancelRequestStatusBadge status={request.status} />
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

/** Renders the newest batch execution summary. */
function BatchHistorySummary({ items, loading, error, onOpenPanel }: Readonly<{
  items: BatchHistoryItem[];
  loading: boolean;
  error: string | null;
  onOpenPanel: () => void;
}>) {
  const latestBatch = [...items].sort((left, right) => compareNewestFirst(left.start_time, right.start_time))[0];

  return (
    <Card className="rounded-xl border-border shadow-app-sm">
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle>バッチ処理</CardTitle>
        <Button type="button" variant="ghost" size="sm" onClick={onOpenPanel} className="text-primary-strong">履歴を見る <ArrowRight /></Button>
      </CardHeader>
      <CardContent>
        {loading ? <SkeletonList items={1} /> : error ? (
          <p role="alert" className="py-6 text-center text-small text-destructive">{error}</p>
        ) : !latestBatch ? (
          <p className="py-6 text-center text-small text-foreground">バッチ処理履歴はありません。</p>
        ) : (
          <div className="flex items-center justify-between gap-4 rounded-lg border border-border bg-muted/20 p-4">
            <div><p className="text-subtle text-foreground">最新の開始日時</p><p className="mt-2 text-body font-bold text-foreground">{formatBatchHistoryDateTime(latestBatch.start_time)}</p></div>
            <BatchHistoryStatusBadge status={latestBatch.status} />
          </div>
        )}
      </CardContent>
    </Card>
  );
}

/** Renders URL-driven monitoring dialogs using the existing full table components. */
function MonitoringDialogs({ activePanel, onPanelChange, cancelRequests, cancelRequestsLoading, cancelRequestsError, batchHistory, batchHistoryPagination, batchHistoryLoading, batchHistoryError, onOpenCancelRequest }: Readonly<Pick<AdminDashboardOverviewProps,
  'activePanel' | 'onPanelChange' | 'cancelRequests' | 'cancelRequestsLoading' | 'cancelRequestsError' | 'batchHistory' | 'batchHistoryPagination' | 'batchHistoryLoading' | 'batchHistoryError' | 'onOpenCancelRequest'
>>) {
  return (
    <>
      <Dialog open={activePanel === 'cancel'} onOpenChange={(open) => onPanelChange(open ? 'cancel' : null)}>
        <DialogContent className="max-h-[90dvh] max-w-[min(96vw,80rem)] overflow-y-auto">
          <DialogHeader><DialogTitle>取消一覧</DialogTitle><DialogDescription>取消対象のモニタリングデータを確認できます。</DialogDescription></DialogHeader>
          <ErrorBoundary>
            <CancelRequestHistoryTable initialRequests={cancelRequests} initialLoading={cancelRequestsLoading} initialLoadError={cancelRequestsError} onViewRequest={onOpenCancelRequest} />
          </ErrorBoundary>
        </DialogContent>
      </Dialog>
      <Dialog open={activePanel === 'batch'} onOpenChange={(open) => onPanelChange(open ? 'batch' : null)}>
        <DialogContent className="max-h-[90dvh] max-w-[min(96vw,80rem)] overflow-y-auto">
          <DialogHeader><DialogTitle>バッチ処理</DialogTitle><DialogDescription>バッチ処理の実行状況と履歴を確認できます。</DialogDescription></DialogHeader>
          <BatchHistoryTable initialItems={batchHistory} initialPagination={batchHistoryPagination} initialLoading={batchHistoryLoading} initialLoadError={batchHistoryError} />
        </DialogContent>
      </Dialog>
    </>
  );
}

/** Renders the Figma-aligned Admin Home from independent existing data sources. */
export function AdminDashboardOverview(props: Readonly<AdminDashboardOverviewProps>) {
  const pendingCancelCount = props.cancelRequests.filter((request) => request.status.toUpperCase() === 'PENDING').length;

  return (
    <div className="space-y-8">
      <header className="space-y-2">
        <h1 className="text-foreground">ホーム</h1>
        <p className="mt-2 text-base text-foreground">運用業務を担当するユーザーのアカウントを管理します。</p>
      </header>
      <section aria-label="主要な登録状況" className="grid gap-4 sm:grid-cols-2 min-[1100px]:grid-cols-4">
        <MetricCard label="プロジェクト" value={props.projects.length} description="登録済みプロジェクト" icon={<FolderKanban />} loading={props.projectsLoading} error={props.projectsError} onClick={props.onNavigateProjects} />
        <MetricCard label="参加企業" value={props.participatingCompanyTotal} description="稼働中・一時停止中" icon={<Building2 />} loading={props.participatingCompaniesLoading} error={props.participatingCompaniesError} onClick={props.onNavigateCompanies} />
        <MetricCard label="企業申請" value={props.applicationTotal} description="申請中・却下" icon={<ClipboardCheck />} loading={props.applicationsLoading} error={props.applicationsError} onClick={props.onNavigateCompanies} />
        <MetricCard label="取消依頼" value={pendingCancelCount} description="確認待ちの取消依頼" icon={<RotateCcw />} loading={props.cancelRequestsLoading} error={props.cancelRequestsError} onClick={() => props.onPanelChange('cancel')} />
      </section>
      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1.65fr)_minmax(20rem,0.85fr)]">
        <div className="space-y-6">
          <Card className="rounded-xl border-border shadow-app-sm">
            <CardHeader><CardTitle>プロジェクト一覧</CardTitle><p className="text-subtle text-foreground">確認が必要な入会届が多いプロジェクトから表示します。</p></CardHeader>
            <CardContent>
              <AdminProjectStatusTable projects={props.projects} openReviews={props.openMembershipReviews} projectsLoading={props.projectsLoading} projectsError={props.projectsError} openReviewsError={props.openMembershipReviewsError} onOpenMembership={props.onOpenProjectMembership} />
            </CardContent>
          </Card>
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
            onOpenAll={props.onNavigateCompanies}
          />
        </div>
        <div className="space-y-6">
          <CancelRequestSummary requests={props.cancelRequests} loading={props.cancelRequestsLoading} error={props.cancelRequestsError} onOpenPanel={() => props.onPanelChange('cancel')} onOpenRequest={props.onOpenCancelRequest} />
          <BatchHistorySummary items={props.batchHistory} loading={props.batchHistoryLoading} error={props.batchHistoryError} onOpenPanel={() => props.onPanelChange('batch')} />
        </div>
      </div>
      <MonitoringDialogs activePanel={props.activePanel} onPanelChange={props.onPanelChange} cancelRequests={props.cancelRequests} cancelRequestsLoading={props.cancelRequestsLoading} cancelRequestsError={props.cancelRequestsError} batchHistory={props.batchHistory} batchHistoryPagination={props.batchHistoryPagination} batchHistoryLoading={props.batchHistoryLoading} batchHistoryError={props.batchHistoryError} onOpenCancelRequest={props.onOpenCancelRequest} />
    </div>
  );
}
