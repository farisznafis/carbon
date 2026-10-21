import { useMemo, useRef, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from 'recharts';
import { CheckCircle2 } from 'lucide-react';
import type { UserNotification } from '@/api/notifications';
import { ActionButton } from '@/components/actions';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { StatusBadge, type StatusBadgeTone } from '@/components/data-display/StatusBadge';
import { NotificationDetailDialog } from '@/components/feedback/NotificationDetailDialog';
import { SkeletonChart, SkeletonList, SkeletonTable } from '@/components/feedback/loading';
import { AdminPageHeader } from '@/components/layout/AdminPageHeader';
import { AdminTableCard } from '@/components/layout/AdminTableCard';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ChartContainer, ChartTooltip, type ChartConfig } from '@/components/ui/chart';
import { toast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';
import { paths } from '@/routes/paths';
import { useNotificationStore } from '@/stores/notificationStore';
import { MasterStatCard } from '@/features/master/shared/MasterStatCard';
import { formatChange, formatRelativeTime, formatTons } from '@/features/master/shared/masterFormatters';
import type { ActionItem, MonthlyEmission, SiteRow } from './masterHomeModel';
import { useMasterHomeData } from './useMasterHomeData';

const SITES_ID = 'master-home-sites';
const NOTICES_ID = 'master-home-notices';
const CHART_MONTHS = 6;
const COLLAPSED_ACTION_LIMIT = 6;
const NOTICE_LIMIT = 5;

const panelTitleClassName = 'text-subsection-title font-bold';
const panelCardClassName = 'scroll-mt-24 rounded-lg border-border';
/** On xl the side panels split the chart's height and scroll inside instead of growing. */
const sidePanelClassName = 'flex flex-col xl:min-h-0 xl:flex-1 xl:overflow-hidden';
const sidePanelBodyClassName = 'xl:min-h-0 xl:flex-1 xl:overflow-y-auto';

const scrollToSection = (id: string) =>
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });

const monthNumber = (month: string) => Number(month.split('-')[1]);
const formatMonth = (month: string) => `${month.split('-')[0]}年${monthNumber(month)}月`;

const formatJstDate = (value: string | null) => {
  if (!value) return '—';
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value.replace(/-/g, '/');
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('ja-JP', { timeZone: 'Asia/Tokyo', year: 'numeric', month: '2-digit', day: '2-digit' }).format(date);
};

function MonthlyEmissionTooltip({ active, payload }: { active?: boolean; payload?: { payload: MonthlyEmission }[] }) {
  const row = payload?.[0]?.payload;
  if (!active || !row) return null;
  return (
    <div className="grid min-w-40 gap-1 rounded-lg border border-border bg-background px-3 py-2 text-xs shadow-xl">
      <p className="font-medium text-foreground">{formatMonth(row.month)}</p>
      <p className="font-mono tabular-nums text-foreground">{formatTons(row.empj)} tCO₂</p>
      <p className="text-muted-foreground">{row.siteCount}拠点のデータ</p>
    </div>
  );
}

const monthlyChartConfig = { empj: { label: '排出量', color: 'hsl(var(--chart-1))' } } satisfies ChartConfig;

function MonthlyEmissionCard({
  series,
  loading,
  error,
  onOpenAnalytics,
}: {
  series: MonthlyEmission[];
  loading: boolean;
  error: boolean;
  onOpenAnalytics: () => void;
}) {
  const recent = series.slice(-CHART_MONTHS);
  return (
    <Card className={cn(panelCardClassName, 'flex h-full flex-col xl:min-h-[30rem]')}>
      <CardHeader className="gap-1.5">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <CardTitle className={panelTitleClassName}>月別の排出量</CardTitle>
          <ActionButton kind="link" onClick={onOpenAnalytics}>
            アナリティクスで詳しく見る
          </ActionButton>
        </div>
        <CardDescription>参加中の全拠点・全プロジェクトの合計（直近{CHART_MONTHS}か月）</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-1 flex-col">
        {loading ? (
          <SkeletonChart className="min-h-64" />
        ) : error && recent.length === 0 ? (
          <p role="alert" className="py-12 text-center text-small text-destructive-strong">排出量を取得できませんでした。</p>
        ) : recent.length === 0 ? (
          <p className="py-12 text-center text-small text-muted-foreground">まだ集計結果がありません</p>
        ) : (
          <>
            <p className="mb-2 ml-1 text-subtle text-muted-foreground">tCO₂</p>
            <ChartContainer config={monthlyChartConfig} className="aspect-auto h-64 w-full flex-1">
              <BarChart accessibilityLayer data={recent} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
                <CartesianGrid vertical={false} />
                <XAxis
                  dataKey="month"
                  tickLine={false}
                  axisLine={false}
                  tickMargin={10}
                  tickFormatter={(month: string) => `${monthNumber(month)}月`}
                />
                <YAxis tickLine={false} axisLine={false} width={48} />
                <ChartTooltip cursor={{ fill: 'hsl(var(--muted))', opacity: 0.4 }} content={<MonthlyEmissionTooltip />} />
                <Bar dataKey="empj" fill="var(--color-empj)" radius={[4, 4, 0, 0]} maxBarSize={48} />
              </BarChart>
            </ChartContainer>
          </>
        )}
      </CardContent>
    </Card>
  );
}

const ACTION_PRESENTATION: Record<ActionItem['kind'], { tag: string; tone: StatusBadgeTone; link: string }> = {
  pending: { tag: '入会申請中', tone: 'info', link: '入会届を見る' },
  unsubmitted: { tag: '今月未提出', tone: 'warning', link: '拠点を開く' },
  noSite: { tag: '拠点未設定', tone: 'danger', link: '社員を編集' },
  unread: { tag: '未読', tone: 'neutral', link: 'お知らせを見る' },
};

function ActionPanel({ actions, loading }: { actions: ActionItem[]; loading: boolean }) {
  const navigate = useNavigate();
  const [expanded, setExpanded] = useState(false);
  const visible = expanded ? actions : actions.slice(0, COLLAPSED_ACTION_LIMIT);

  const open = (action: ActionItem) => {
    if (action.kind === 'pending') navigate(paths.masterDashboardMembership);
    else if (action.kind === 'unsubmitted' && action.siteId) navigate(paths.siteDashboard(action.siteId));
    else if (action.kind === 'noSite' && action.userId)
      navigate(paths.userEdit(action.userId), { state: { returnTo: paths.masterDashboard } });
    else if (action.kind === 'unread') scrollToSection(NOTICES_ID);
  };

  return (
    <Card className={cn(panelCardClassName, sidePanelClassName, actions.length > 0 && 'border-primary/60')}>
      <CardHeader className="shrink-0 flex-row items-center gap-2 space-y-0">
        <CardTitle className={panelTitleClassName}>対応が必要なこと</CardTitle>
        {!loading && actions.length > 0 && (
          <span className="inline-flex h-6 min-w-6 items-center justify-center rounded-full bg-primary-strong px-1.5 text-xs font-bold tabular-nums text-primary-foreground">
            {actions.length}
          </span>
        )}
      </CardHeader>
      <CardContent className={sidePanelBodyClassName}>
        {loading ? (
          <SkeletonList items={3} />
        ) : actions.length === 0 ? (
          <div className="flex items-center gap-2 rounded-lg border border-success-border bg-success-subtle px-4 py-3 text-small text-success-strong">
            <CheckCircle2 className="h-4 w-4 shrink-0" aria-hidden="true" />
            対応が必要なことはありません
          </div>
        ) : (
          <>
            <ul className="divide-y divide-border">
              {visible.map((action) => {
                const presentation = ACTION_PRESENTATION[action.kind];
                return (
                  <li key={action.key} className="py-2.5">
                    {/* Tag and link share the first line so the message gets the full width. */}
                    <div className="flex items-center justify-between gap-2">
                      <StatusBadge tone={presentation.tone}>{presentation.tag}</StatusBadge>
                      <ActionButton kind="link" hideIcon className="-my-1 -mr-2 h-7" onClick={() => open(action)}>
                        {presentation.link}
                      </ActionButton>
                    </div>
                    <p className="mt-1 break-words text-body text-foreground">{action.text}</p>
                  </li>
                );
              })}
            </ul>
            {actions.length > COLLAPSED_ACTION_LIMIT && (
              <ActionButton kind="link" className="mt-2" onClick={() => setExpanded((value) => !value)}>
                {expanded ? '折りたたむ' : `すべて表示（${actions.length}件）`}
              </ActionButton>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
}

function NoticePanel() {
  const notifications = useNotificationStore((state) => state.notifications);
  const loading = useNotificationStore((state) => state.loading);
  const error = useNotificationStore((state) => state.error);
  const markAsRead = useNotificationStore((state) => state.markAsRead);
  const setPanelOpen = useNotificationStore((state) => state.setPanelOpen);
  const [selected, setSelected] = useState<UserNotification | null>(null);
  const returnFocusRef = useRef<HTMLButtonElement | null>(null);
  // shortcut: reuses the store's first page (size 10) instead of a separate limit=5 request.
  const latest = useMemo(() => notifications.slice(0, NOTICE_LIMIT), [notifications]);

  const openNotice = (notification: UserNotification, button: HTMLButtonElement) => {
    returnFocusRef.current = button;
    setSelected(notification);
    if (!notification.read) {
      markAsRead(notification.id).catch(() =>
        toast({ title: 'エラー', description: '通知を既読にできませんでした。', variant: 'destructive' }),
      );
    }
  };

  return (
    <Card id={NOTICES_ID} className={cn(panelCardClassName, sidePanelClassName)}>
      <CardHeader className="shrink-0 flex-row items-center justify-between space-y-0">
        <CardTitle className={panelTitleClassName}>お知らせ</CardTitle>
        <ActionButton kind="link" hideIcon onClick={() => setPanelOpen(true)}>
          すべて見る
        </ActionButton>
      </CardHeader>
      <CardContent className={sidePanelBodyClassName}>
        {loading && latest.length === 0 ? (
          <SkeletonList items={3} />
        ) : error ? (
          <p role="alert" className="py-6 text-center text-small text-destructive-strong">{error}</p>
        ) : latest.length === 0 ? (
          <p className="py-6 text-center text-small text-muted-foreground">お知らせはありません</p>
        ) : (
          <ul className="divide-y divide-border">
            {latest.map((notification) => (
              <li key={notification.id}>
                <button
                  type="button"
                  onClick={(event) => openNotice(notification, event.currentTarget)}
                  className="flex w-full gap-3 rounded-lg py-3 text-left transition-colors hover:bg-muted/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <span
                    className={cn('mt-2 h-2 w-2 shrink-0 rounded-full', notification.read ? 'bg-border' : 'bg-primary')}
                    aria-hidden="true"
                  />
                  <span className="min-w-0 flex-1">
                    <span className={cn('block truncate text-body text-foreground', !notification.read && 'font-bold')}>
                      {!notification.read && <span className="sr-only">未読：</span>}
                      {notification.title || 'お知らせ'}
                    </span>
                    <span className="mt-0.5 block truncate text-subtle text-muted-foreground">{notification.message}</span>
                    <span className="mt-1 block text-subtle text-muted-foreground">
                      {formatRelativeTime(notification.createdAt)}
                    </span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
      <NotificationDetailDialog
        notification={selected}
        open={selected !== null}
        onOpenChange={(open) => {
          if (!open) setSelected(null);
        }}
        returnFocusRef={returnFocusRef}
      />
    </Card>
  );
}

const THIS_MONTH_BADGE: Record<SiteRow['thisMonth'], ReactNode> = {
  submitted: <StatusBadge tone="success">提出済</StatusBadge>,
  unsubmitted: <StatusBadge tone="warning">未提出</StatusBadge>,
  none: <StatusBadge tone="neutral">対象なし</StatusBadge>,
};

const SITE_TABLE_MIN_WIDTH = 720;
const siteGridColumns = 'minmax(10rem,1.4fr) minmax(9rem,1.2fr) 9rem 6.5rem 8rem 6rem';

function SiteTable({ rows, loading, monitoringLoading }: { rows: SiteRow[]; loading: boolean; monitoringLoading: boolean }) {
  const navigate = useNavigate();
  const columns = useMemo<DataGridTableColumn<SiteRow>[]>(
    () => [
      {
        id: 'siteName',
        label: '拠点名',
        sortable: true,
        sortValue: (row) => row.site.siteName,
        render: (row) => <span className="truncate text-body font-bold text-foreground">{row.site.siteName}</span>,
      },
      { id: 'address', label: '所在地', render: (row) => `${row.site.prefectureName}${row.site.cityName}` || '—' },
      {
        id: 'projectCount',
        label: '参加プロジェクト',
        sortable: true,
        sortValue: (row) => row.projectCount,
        render: (row) => <span className="tabular-nums">{row.projectCount}</span>,
      },
      {
        id: 'userCount',
        label: 'ユーザー',
        sortable: true,
        sortValue: (row) => row.userCount,
        render: (row) => <span className="tabular-nums">{row.userCount}</span>,
      },
      {
        id: 'lastSubmittedAt',
        label: '最終提出日',
        sortable: true,
        sortValue: (row) => row.lastSubmittedAt ?? '',
        render: (row) => <span className="tabular-nums">{monitoringLoading ? '…' : formatJstDate(row.lastSubmittedAt)}</span>,
      },
      { id: 'thisMonth', label: '今月', render: (row) => (monitoringLoading ? '…' : THIS_MONTH_BADGE[row.thisMonth]) },
    ],
    [monitoringLoading],
  );

  return (
    <div id={SITES_ID} className="min-w-0 scroll-mt-24">
      <AdminTableCard title="拠点ごとの状況" description="行を選択すると拠点ダッシュボードを開きます">
        {loading ? (
          <SkeletonTable columns={6} rows={3} minWidth={SITE_TABLE_MIN_WIDTH} />
        ) : (
          <DataGridTable
            rows={rows}
            columns={columns}
            gridTemplateColumns={siteGridColumns}
            minWidth={SITE_TABLE_MIN_WIDTH}
            getRowKey={(row) => row.site.siteId}
            onRowClick={(row) => navigate(paths.siteDashboard(row.site.siteId))}
            getRowActionLabel={(row) => `${row.site.siteName}の拠点ダッシュボードを開く`}
            emptyMessage="まだ拠点が登録されていません。"
          />
        )}
      </AdminTableCard>
    </div>
  );
}

/** Master ホーム: this month's numbers, to-dos, per-site status and latest notices. */
export function MasterHomeSection() {
  const navigate = useNavigate();
  const home = useMasterHomeData();
  const header = (
    <AdminPageHeader title="ホーム" description="会社全体の拠点・プロジェクトの状況と、対応が必要なことを確認できます。" />
  );

  if (home.error) {
    return (
      <div className="space-y-section">
        {header}
        <div role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center text-small text-destructive-strong">
          {home.error instanceof Error ? home.error.message : 'ダッシュボードデータの取得に失敗しました'}
        </div>
      </div>
    );
  }

  const latest = home.monthlyEmissions.at(-1);
  const previous = home.monthlyEmissions.at(-2);
  const emissionChange =
    latest && previous && previous.empj !== 0 ? ((latest.empj - previous.empj) / previous.empj) * 100 : null;
  const openAnalytics = () => navigate(`${paths.masterDashboard}?tab=analytics`);

  return (
    <div className="space-y-section">
      {header}

      <section aria-label="今月の状況" className="grid items-stretch gap-grid sm:grid-cols-2 xl:grid-cols-4">
        <MasterStatCard
          label="今月の排出量"
          value={formatTons(latest?.empj)}
          unit={latest ? 'tCO₂' : undefined}
          note={
            latest
              ? [`${monthNumber(latest.month)}月処理分`, formatChange('前月比', emissionChange)].filter(Boolean).join('・')
              : 'まだ集計結果がありません'
          }
          loading={home.isEmissionLoading}
          onClick={openAnalytics}
        />
        <MasterStatCard
          label="対象拠点"
          value={home.siteCount.toLocaleString('ja-JP')}
          unit="拠点"
          note={`うちプロジェクト参加 ${home.participatingSiteCount}拠点`}
          loading={home.isBaseLoading}
          onClick={() => navigate(`${paths.masterDashboard}?tab=sites`)}
        />
        <MasterStatCard
          label="参加プロジェクト"
          value={home.approvedProjectCount.toLocaleString('ja-JP')}
          unit="件"
          note={home.pendingProjectCount > 0 ? `入会申請中 ${home.pendingProjectCount}件` : undefined}
          loading={home.isBaseLoading}
          onClick={() => navigate(paths.masterDashboardMembership)}
        />
        <MasterStatCard
          label="今月のモニタリング提出"
          value={home.targets.length > 0 ? home.submittedCount.toLocaleString('ja-JP') : '対象なし'}
          unit={home.targets.length > 0 ? `/ ${home.targets.length}件` : undefined}
          note={home.targets.length > 0 ? '提出済み' : undefined}
          loading={home.isMonitoringLoading}
          onClick={() => scrollToSection(SITES_ID)}
        />
      </section>

      <div className="grid gap-grid xl:grid-cols-[minmax(0,1.5fr)_minmax(20rem,1fr)]">
        <MonthlyEmissionCard
          series={home.monthlyEmissions}
          loading={home.isEmissionLoading}
          error={home.emissionError}
          onOpenAnalytics={openAnalytics}
        />
        {/* Out of flow on xl, so the row height follows 月別の排出量. */}
        <div className="relative min-w-0">
          <div className="flex flex-col gap-grid xl:absolute xl:inset-0">
            <ActionPanel actions={home.actions} loading={home.isMonitoringLoading} />
            <NoticePanel />
          </div>
        </div>
      </div>

      <SiteTable rows={home.siteRows} loading={home.isBaseLoading} monitoringLoading={home.isMonitoringLoading} />
    </div>
  );
}
