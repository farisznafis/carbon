import { AlertTriangle, Building2, ClipboardCheck, FolderKanban, RotateCcw } from 'lucide-react';
import type { AdminCompanyListItem } from '@/api/admin';
import type { BatchHistoryItem } from '@/api/batch';
import type { MonitoringCancelRequestListItem } from '@/api/monitoring';
import type { Project } from '@/api/projects';
import type { UserNotification } from '@/api/notifications';
import {
  SkeletonList,
  SkeletonMetricCard,
} from '@/components/feedback/loading';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils';

type AdminDashboardOverviewProps = {
  projects: Project[];
  projectsLoading: boolean;
  projectsError: string | null;
  applications: AdminCompanyListItem[];
  applicationTotal: number;
  applicationsLoading: boolean;
  applicationsError: string | null;
  participatingCompanyTotal: number;
  participatingCompaniesLoading: boolean;
  participatingCompaniesError: string | null;
  cancelRequests: MonitoringCancelRequestListItem[];
  cancelRequestsLoading: boolean;
  cancelRequestsError: string | null;
  batchHistory: BatchHistoryItem[];
  batchHistoryLoading: boolean;
  notifications: UserNotification[];
  notificationsLoading: boolean;
  notificationsError: string | null;
};

type MetricCardProps = {
  label: string;
  value: number;
  description: string;
  icon: React.ReactNode;
  loading: boolean;
  error: string | null;
};

/** Renders one overview metric without blocking neighboring data sources. */
function MetricCard({ label, value, description, icon, loading, error }: MetricCardProps) {
  if (loading) return <SkeletonMetricCard />;

  return (
    <Card className="min-h-36 rounded-xl border-border shadow-app-sm">
      <CardContent className="flex h-full items-start justify-between gap-4 p-5">
        <div>
          <p className="text-small text-muted-foreground">{label}</p>
          {error ? (
            <p role="alert" className="mt-4 text-small text-destructive">取得できませんでした</p>
          ) : (
            <p className="mt-3 text-3xl font-bold leading-none text-foreground">{value.toLocaleString('ja-JP')}</p>
          )}
          <p className="mt-3 text-subtle text-muted-foreground">{description}</p>
        </div>
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-secondary text-primary" aria-hidden="true">
          {icon}
        </span>
      </CardContent>
    </Card>
  );
}

/** Renders a clearly labeled visual preview while aggregate trend data is not connected. */
function ReductionTrendPreview() {
  return (
    <Card className="min-h-[360px] rounded-xl border-border shadow-app-sm">
      <CardHeader className="flex-row items-start justify-between space-y-0">
        <div>
          <CardTitle>削減量の推移</CardTitle>
          <p className="mt-2 text-subtle text-muted-foreground">データ連携準備中</p>
        </div>
        <span className="rounded-full border border-border bg-muted/40 px-3 py-1 text-subtle text-foreground">表示イメージ</span>
      </CardHeader>
      <CardContent>
        <div className="relative h-[230px] overflow-hidden rounded-lg border-b border-l border-border bg-muted/15" aria-label="削減量推移の表示イメージ">
          <svg viewBox="0 0 600 220" className="h-full w-full" aria-hidden="true" preserveAspectRatio="none">
            <path d="M0 180 C90 168 130 130 205 142 C300 158 325 76 410 94 C490 110 515 44 600 52" fill="none" stroke="hsl(var(--primary))" strokeWidth="4" opacity="0.72" />
            <path d="M0 180 C90 168 130 130 205 142 C300 158 325 76 410 94 C490 110 515 44 600 52 L600 220 L0 220 Z" fill="hsl(var(--primary) / 0.08)" />
          </svg>
        </div>
      </CardContent>
    </Card>
  );
}

/** Renders one compact summary row with optional error styling. */
function SummaryRow({ label, value, error }: { label: string; value: number; error?: boolean }) {
  return (
    <div className="flex min-h-14 items-center justify-between gap-4 border-b border-border py-3 last:border-b-0">
      <span className="text-small text-foreground">{label}</span>
      <span className={cn('font-bold text-foreground', error && 'text-destructive')}>
        {error ? '取得失敗' : `${value.toLocaleString('ja-JP')}件`}
      </span>
    </div>
  );
}

/** Renders the new Admin Dashboard overview with independently resolving sections. */
export function AdminDashboardOverview(props: AdminDashboardOverviewProps) {
  const pendingApplications = props.applications.filter((company) => company.status === 'PENDING');
  const pendingCancelRequests = props.cancelRequests.filter((request) => request.status === 'PENDING');
  const activeBatchCount = props.batchHistory.filter((batch) => batch.is_active).length;
  const actionItems = [
    ...pendingApplications.slice(0, 3).map((company) => ({
      id: `company-${company.companyId}`,
      title: company.nameJA || '企業申請',
      detail: '企業参加申請の確認が必要です',
    })),
    ...pendingCancelRequests.slice(0, 3).map((request) => ({
      id: `cancel-${request.id}`,
      title: request.siteName || 'モニタリング取消依頼',
      detail: '取消依頼の確認が必要です',
    })),
  ].slice(0, 5);

  return (
    <div className="space-y-10">
      <section aria-labelledby="admin-overview-metrics" className="space-y-4">
        <div>
          <h2 id="admin-overview-metrics" className="text-foreground">運用状況</h2>
          <p className="mt-2 text-subtle text-foreground">主要な登録状況と対応件数を確認できます。</p>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <MetricCard label="プロジェクト" value={props.projects.length} description="登録済みプロジェクト" icon={<FolderKanban />} loading={props.projectsLoading} error={props.projectsError} />
          <MetricCard label="参加企業" value={props.participatingCompanyTotal} description="承認済みの企業" icon={<Building2 />} loading={props.participatingCompaniesLoading} error={props.participatingCompaniesError} />
          <MetricCard label="企業申請" value={props.applicationTotal} description="申請中・差戻しの企業" icon={<ClipboardCheck />} loading={props.applicationsLoading} error={props.applicationsError} />
          <MetricCard label="取消依頼" value={props.cancelRequests.length} description="モニタリング取消依頼" icon={<RotateCcw />} loading={props.cancelRequestsLoading} error={props.cancelRequestsError} />
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-[0.85fr_1.4fr]">
        <Card className="rounded-xl border-border shadow-app-sm">
          <CardHeader><CardTitle>対応サマリー</CardTitle></CardHeader>
          <CardContent>
            {props.applicationsLoading || props.cancelRequestsLoading || props.batchHistoryLoading ? (
              <SkeletonList items={3} />
            ) : (
              <div>
                <SummaryRow label="審査待ちの企業申請" value={pendingApplications.length} error={Boolean(props.applicationsError)} />
                <SummaryRow label="未処理の取消依頼" value={pendingCancelRequests.length} error={Boolean(props.cancelRequestsError)} />
                <SummaryRow label="実行中のバッチ" value={activeBatchCount} />
              </div>
            )}
          </CardContent>
        </Card>
        <ReductionTrendPreview />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="min-h-80 rounded-xl border-border shadow-app-sm">
          <CardHeader><CardTitle>要対応一覧</CardTitle></CardHeader>
          <CardContent>
            {props.applicationsLoading || props.cancelRequestsLoading ? (
              <SkeletonList />
            ) : props.applicationsError && props.cancelRequestsError ? (
              <p role="alert" className="flex items-center gap-2 text-small text-destructive"><AlertTriangle aria-hidden="true" />対応情報を取得できませんでした。</p>
            ) : actionItems.length === 0 ? (
              <p className="py-8 text-center text-small text-foreground">現在、対応が必要な項目はありません。</p>
            ) : (
              <ul className="divide-y divide-border">
                {actionItems.map((item) => <li key={item.id} className="py-3"><p className="text-small font-bold text-foreground">{item.title}</p><p className="mt-1 text-subtle text-muted-foreground">{item.detail}</p></li>)}
              </ul>
            )}
          </CardContent>
        </Card>

        <Card className="min-h-80 rounded-xl border-border shadow-app-sm">
          <CardHeader><CardTitle>最近の更新</CardTitle></CardHeader>
          <CardContent>
            {props.notificationsLoading ? (
              <SkeletonList />
            ) : props.notificationsError ? (
              <p role="alert" className="text-small text-destructive">{props.notificationsError}</p>
            ) : props.notifications.length === 0 ? (
              <p className="py-8 text-center text-small text-foreground">最近の更新はありません。</p>
            ) : (
              <ul className="divide-y divide-border">
                {props.notifications.slice(0, 5).map((notification) => <li key={notification.id} className="py-3"><p className="text-small font-bold text-foreground">{notification.title || '更新のお知らせ'}</p><p className="mt-1 line-clamp-2 text-subtle text-muted-foreground">{notification.message}</p></li>)}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
