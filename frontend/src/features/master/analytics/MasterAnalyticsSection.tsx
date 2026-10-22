import { useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from 'recharts';
import { ChartCard, TimeSeriesLineChart, formatChartPeriod } from '@/components/data-display/charts';
import { SkeletonList } from '@/components/feedback/loading';
import { AdminPageHeader } from '@/components/layout/AdminPageHeader';
import { SegmentButton, SegmentGroup } from '@/components/navigation/PageTabs';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ChartContainer, ChartTooltip, type ChartConfig } from '@/components/ui/chart';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { ActionButton } from '@/components/actions';
import { cn } from '@/lib/utils';
import { paths } from '@/routes/paths';
import { MasterStatCard } from '@/features/master/shared/MasterStatCard';
import { formatChange, formatTons } from '@/features/master/shared/masterFormatters';
import type {
  AnalysisAxis,
  AnalyticsPeriod,
  EmissionRow,
  RankingRow,
} from './masterAnalyticsModel';
import { NoSiteEmptyState } from '@/features/master/components/MasterDashboardComponents';
import { useMasterAnalyticsData } from './useMasterAnalyticsData';

const PERIOD_OPTIONS: { value: AnalyticsPeriod; label: string }[] = [
  { value: '6', label: '直近6か月' },
  { value: '12', label: '直近12か月' },
  { value: 'all', label: '全期間' },
];
const AXIS_OPTIONS: { value: AnalysisAxis; label: string }[] = [
  { value: 'project', label: 'プロジェクト' },
  { value: 'site', label: '拠点' },
];
const LINE_COLORS = ['--chart-1', '--chart-2', '--chart-3', '--chart-4', '--chart-5'];
const EMISSION_SERIES = [
  { key: 'main', label: '主たる排出', color: 'hsl(var(--chart-1))' },
  { key: 'control', label: '制御機器', color: 'hsl(var(--chart-3))' },
  { key: 'battery', label: '蓄電池', color: 'hsl(var(--chart-4))' },
  { key: 'other', label: 'その他', color: 'hsl(var(--chart-5))' },
] as const;

const panelTitleClassName = 'text-subsection-title font-bold';
const panelCardClassName = 'rounded-lg border-border';

const monthNumber = (month: string) => Number(month.split('-')[1]);
const formatDate = (value?: string | null) => (value ? formatChartPeriod(value.slice(0, 10)) : '—');
const formatTonsUnit = (value: number | null | undefined) =>
  value === null || value === undefined ? '—' : `${formatTons(value)} tCO₂`;

function ControlField({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex min-w-0 flex-col gap-1.5">
      <span className="text-subtle font-bold text-muted-foreground">{label}</span>
      {children}
    </div>
  );
}

function EmissionTooltip({ active, payload }: { active?: boolean; payload?: { payload: EmissionRow }[] }) {
  const row = payload?.[0]?.payload;
  if (!active || !row) return null;
  return (
    <div className="grid min-w-52 gap-1.5 rounded-lg border border-border bg-background px-3 py-2 text-xs shadow-xl">
      <p className="font-medium text-foreground">{formatChartPeriod(row.period)}</p>
      {row.total === null ? (
        <p className="text-muted-foreground">データなし</p>
      ) : (
        <>
          {EMISSION_SERIES.filter((series) => series.key !== 'other' || row.other !== 0).map((series) => (
            <div key={series.key} className="flex items-center justify-between gap-4">
              <span className="flex items-center gap-2 text-foreground">
                <span className="h-2.5 w-2.5 rounded-sm" style={{ backgroundColor: series.color }} aria-hidden="true" />
                {series.label}
              </span>
              <span className="font-mono tabular-nums text-foreground">{formatTonsUnit(row[series.key])}</span>
            </div>
          ))}
          <div className="flex justify-between gap-4 border-t border-border pt-1.5 font-medium text-foreground">
            <span>合計</span>
            <span className="font-mono tabular-nums">{formatTonsUnit(row.total)}</span>
          </div>
        </>
      )}
    </div>
  );
}

function EmissionBreakdownCard({
  rows,
  subtitle,
  loading,
  error,
}: {
  rows: EmissionRow[];
  subtitle: string;
  loading: boolean;
  error: boolean;
}) {
  const hasOther = rows.some((row) => row.other !== 0);
  const series = EMISSION_SERIES.filter((entry) => entry.key !== 'other' || hasOther);
  const config = Object.fromEntries(
    series.map((entry) => [entry.key, { label: entry.label, color: entry.color }]),
  ) satisfies ChartConfig;

  return (
    <ChartCard
      title="排出量の内訳"
      description={subtitle}
      isLoading={loading}
      error={error ? '排出量を取得できませんでした。' : null}
      isEmpty={rows.every((row) => row.total === null)}
      emptyMessage="この期間の集計結果はありません"
    >
      <p className="mb-2 ml-1 text-subtle text-muted-foreground">tCO₂</p>
      <ChartContainer config={config} className="aspect-auto h-64 w-full">
        <BarChart accessibilityLayer data={rows} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          <CartesianGrid vertical={false} />
          <XAxis dataKey="period" tickLine={false} axisLine={false} tickMargin={10} minTickGap={8} tickFormatter={(month: string) => `${monthNumber(month)}月`} />
          <YAxis tickLine={false} axisLine={false} width={48} />
          <ChartTooltip cursor={{ fill: 'hsl(var(--muted))', opacity: 0.4 }} content={<EmissionTooltip />} />
          {series.map((entry, index) => (
            <Bar
              key={entry.key}
              dataKey={entry.key}
              stackId="empj"
              fill={`var(--color-${entry.key})`}
              radius={index === series.length - 1 ? [3, 3, 0, 0] : 0}
              maxBarSize={32}
            />
          ))}
        </BarChart>
      </ChartContainer>
      <ul aria-label="凡例" className="mt-4 flex flex-wrap items-center gap-x-5 gap-y-2 text-subtle text-foreground">
        {series.map((entry) => (
          <li key={entry.key} className="flex items-center gap-2">
            <span className="h-2.5 w-2.5 rounded-sm" style={{ backgroundColor: entry.color }} aria-hidden="true" />
            {entry.label}
          </li>
        ))}
      </ul>
      {hasOther && (
        <p className="mt-2 text-subtle text-muted-foreground">その他：排出量の合計と3項目の合計との差分です。</p>
      )}
    </ChartCard>
  );
}

function RankingCard({
  axis,
  rows,
  loading,
  onSelect,
}: {
  axis: AnalysisAxis;
  rows: RankingRow[];
  loading: boolean;
  onSelect: (row: RankingRow) => void;
}) {
  const max = rows[0]?.total ?? 0;
  return (
    <Card className={cn(panelCardClassName, 'h-full')}>
      <CardHeader className="gap-1.5">
        <CardTitle className={panelTitleClassName}>{axis === 'project' ? '拠点別ランキング' : 'プロジェクト別ランキング'}</CardTitle>
        <CardDescription>期間の排出量が多い順・選択すると{axis === 'project' ? '拠点' : 'プロジェクト'}で表示します</CardDescription>
      </CardHeader>
      <CardContent>
        {loading ? (
          <SkeletonList items={3} />
        ) : rows.length === 0 ? (
          <p className="py-6 text-center text-small text-muted-foreground">この期間の集計結果はありません</p>
        ) : (
          <ul className="divide-y divide-border">
            {rows.map((row) => (
              <li key={row.id}>
                <button
                  type="button"
                  onClick={() => onSelect(row)}
                  className="block w-full rounded-lg py-3 text-left transition-colors hover:bg-muted/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <span className="flex items-baseline justify-between gap-3">
                    <span className="min-w-0 truncate text-body font-bold text-foreground">{row.label}</span>
                    <span className="shrink-0 text-body font-bold tabular-nums text-foreground">{formatTonsUnit(row.total)}</span>
                  </span>
                  <span className="mt-2 block h-2 overflow-hidden rounded-full bg-muted" aria-hidden="true">
                    <span className="block h-full rounded-full bg-primary" style={{ width: `${max > 0 ? (row.total / max) * 100 : 0}%` }} />
                  </span>
                  <span className="mt-1.5 block text-subtle text-muted-foreground">全体の {row.share.toFixed(1)}%</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

/** Master アナリティクス: compare emissions and submissions by project or by site. */
export function MasterAnalyticsSection() {
  const [axis, setAxis] = useState<AnalysisAxis>('project');
  const [period, setPeriod] = useState<AnalyticsPeriod>('12');
  const [selected, setSelected] = useState<Record<AnalysisAxis, string | null>>({ project: null, site: null });
  const navigate = useNavigate();
  const data = useMasterAnalyticsData({ axis, period, selectedId: selected[axis] });
  const targetLabel = axis === 'project' ? 'プロジェクト' : '拠点';
  const select = (nextAxis: AnalysisAxis, id: string) => {
    setAxis(nextAxis);
    setSelected((current) => ({ ...current, [nextAxis]: id }));
  };

  const controls = (
    <div className="flex flex-wrap items-end gap-x-4 gap-y-3">
      <ControlField label="分析軸">
        <SegmentGroup aria-label="分析軸">
          {AXIS_OPTIONS.map((option) => (
            <SegmentButton key={option.value} active={axis === option.value} onClick={() => setAxis(option.value)}>
              {option.label}
            </SegmentButton>
          ))}
        </SegmentGroup>
      </ControlField>
      {/* A dropdown, not cards: the chart only ever shows one target, and the card details repeated the summary. */}
      <ControlField label={targetLabel}>
        <Select
          value={data.activeCard?.id ?? ''}
          onValueChange={(id) => select(axis, id)}
          disabled={data.cards.length === 0}
        >
          <SelectTrigger selectSize="sm" className="w-[min(24rem,80vw)]" aria-label={targetLabel}>
            <SelectValue placeholder="—" />
          </SelectTrigger>
          <SelectContent>
            {data.cards.map((card) => (
              <SelectItem key={card.id} value={card.id}>
                {card.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </ControlField>
      <ControlField label="期間">
        <Select value={period} onValueChange={(value) => setPeriod(value as AnalyticsPeriod)}>
          <SelectTrigger selectSize="sm" className="w-40" aria-label="期間">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {PERIOD_OPTIONS.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </ControlField>
    </div>
  );
  const header = (
    <AdminPageHeader
      title="アナリティクス"
      description="プロジェクトまたは拠点を軸に、排出量の推移と内訳、モニタリングの提出状況を比較できます。"
    />
  );

  if (data.error) {
    return (
      <div className="space-y-section">
        {header}
        {controls}
        <div role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center text-small text-destructive-strong">
          {data.error instanceof Error ? data.error.message : 'データの取得に失敗しました'}
        </div>
      </div>
    );
  }

  // 拠点がなければ分析軸・期間を選んでも何も表示できないため、操作部品ごと隠す
  if (!data.isBaseLoading && !data.hasSites) {
    return (
      <div className="space-y-section">
        {header}
        <NoSiteEmptyState description="拠点を登録してプロジェクトに入会すると、排出量の推移や提出状況を分析できます。" />
      </div>
    );
  }

  const active = data.activeCard;
  const memberUnit = axis === 'project' ? '拠点' : 'プロジェクト';
  const firstMonth = data.emissionMonths[0];
  const lastMonth = data.emissionMonths.at(-1);
  const periodLabel = firstMonth && lastMonth ? `${formatChartPeriod(firstMonth)}〜${formatChartPeriod(lastMonth)}処理分` : '';
  const trendTitle = active
    ? `${active.name}：${axis === 'project' ? '拠点別' : 'プロジェクト別'}の排出量推移`
    : '排出量の推移';

  return (
    <div className="space-y-section">
      <div className="space-y-4">
        {header}
        {/* Controls get a full row (one line at laptop width), and the 表示中 line sits under them
            at full width instead of being squeezed beside them. */}
        {controls}
        <div>
          {active ? (
            <div className="flex min-w-0 flex-wrap items-center gap-x-3 gap-y-1">
              <p className="min-w-0 text-subtle text-muted-foreground">
                表示中：<span className="font-bold text-foreground">{active.name}</span>
                （{active.members.length}{memberUnit}）・{periodLabel}
                {axis === 'project' && active.projectReduction != null && (
                  <>
                    ・プロジェクト全体の削減量{' '}
                    <span className="font-bold tabular-nums text-foreground">{formatTonsUnit(active.projectReduction)}</span>
                  </>
                )}
                {axis === 'site' && <>・最終提出日 {formatDate(active.lastSubmittedAt)}</>}
              </p>
              {/* Only sites have a page of their own; a project's detail lives under each site. */}
              {axis === 'site' && (
                <ActionButton kind="link" onClick={() => navigate(paths.siteDashboard(active.id))}>
                  拠点ダッシュボード
                </ActionButton>
              )}
            </div>
          ) : null}
        </div>
      </div>

      {!data.isBaseLoading && !data.hasTargets ? (
        <div className="rounded-lg border border-border bg-card p-8 text-center text-small text-muted-foreground">
          参加中の拠点・プロジェクトがありません。入会が承認されると、ここに分析が表示されます。
        </div>
      ) : (
        <>
          <section aria-label="期間の概要" className="grid items-stretch gap-grid sm:grid-cols-3">
            <MasterStatCard
              label="期間の排出量"
              value={formatTons(data.summary.total)}
              unit={data.summary.total !== null ? 'tCO₂' : undefined}
              note={formatChange('前期間比', data.summary.change) ?? undefined}
              loading={data.isEmissionLoading}
            />
            <MasterStatCard
              label="月平均の排出量"
              value={formatTons(data.summary.average)}
              unit={data.summary.average !== null ? 'tCO₂' : undefined}
              note={data.summary.monthsWithData > 0 ? `データのある${data.summary.monthsWithData}か月の平均` : undefined}
              loading={data.isEmissionLoading}
            />
            <MasterStatCard
              label="モニタリング提出率"
              value={data.submissionRate === null ? '—' : Math.round(data.submissionRate)}
              unit={data.submissionRate === null ? undefined : '%'}
              note="提出が必要な月のうち提出済みの割合"
              loading={data.isRecordsLoading}
            />
          </section>

          <ChartCard
            title={trendTitle}
            description={
              axis === 'project'
                ? '選択したプロジェクトについて、参加拠点ごとの月別排出量を比較します'
                : '選択した拠点について、プロジェクトごとの月別排出量を比較します'
            }
            isLoading={data.isEmissionLoading}
            error={data.emissionError ? '排出量を取得できませんでした。' : null}
            isEmpty={data.summary.total === null}
            emptyMessage="この期間の集計結果はありません"
          >
            <TimeSeriesLineChart
              data={data.trend.rows}
              series={data.trend.series.map((series, index) => ({
                ...series,
                color: `hsl(var(${LINE_COLORS[index % LINE_COLORS.length]}))`,
              }))}
              unit="tCO₂"
              yAxisLabel="排出量（tCO₂）"
              xAxisLabel="処理月"
              ariaLabel={trendTitle}
            />
          </ChartCard>

          <div className="grid gap-grid xl:grid-cols-[minmax(0,1.65fr)_minmax(18rem,1fr)]">
            <EmissionBreakdownCard
              rows={data.emissionRows}
              subtitle={active ? `${active.name}・月別` : '月別'}
              loading={data.isEmissionLoading}
              error={data.emissionError}
            />
            <RankingCard
              axis={axis}
              rows={data.ranking}
              loading={data.isEmissionLoading}
              onSelect={(row) => select(axis === 'project' ? 'site' : 'project', row.id)}
            />
          </div>
        </>
      )}
    </div>
  );
}
