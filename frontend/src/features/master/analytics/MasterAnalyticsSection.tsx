import { useState, type ReactNode } from 'react';
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from 'recharts';
import { Check, Clock } from 'lucide-react';
import { ChartCard, TimeSeriesLineChart, formatChartPeriod } from '@/components/data-display/charts';
import { SkeletonList, SkeletonTable } from '@/components/feedback/loading';
import { AdminPageHeader } from '@/components/layout/AdminPageHeader';
import { AdminTableCard } from '@/components/layout/AdminTableCard';
import { SegmentButton, SegmentGroup } from '@/components/navigation/PageTabs';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ChartContainer, ChartTooltip, type ChartConfig } from '@/components/ui/chart';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';
import { MasterStatCard } from '@/features/master/shared/MasterStatCard';
import { formatChange, formatTons } from '@/features/master/shared/masterFormatters';
import type {
  AnalysisAxis,
  AnalyticsPeriod,
  CalendarRow,
  CalendarStatus,
  EmissionRow,
  RankingRow,
} from './masterAnalyticsModel';
import { useMasterAnalyticsData, type SelectorCard } from './useMasterAnalyticsData';

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
const panelCardClassName = 'rounded-xl border-border shadow-app-sm';

const monthNumber = (month: string) => Number(month.split('-')[1]);
const shortMonth = (month: string, index: number) =>
  index === 0 || monthNumber(month) === 1 ? `${month.slice(2, 4)}年${monthNumber(month)}月` : `${monthNumber(month)}月`;
const formatDate = (value?: string | null) => (value ? formatChartPeriod(value.slice(0, 10)) : '—');
const formatTonsUnit = (value: number | null | undefined) =>
  value === null || value === undefined ? '—' : `${formatTons(value)} tCO₂`;

function ControlField({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-small font-bold text-foreground">{label}</span>
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

function SelectorCardItem({
  axis,
  card,
  active,
  onSelect,
}: {
  axis: AnalysisAxis;
  card: SelectorCard;
  active: boolean;
  onSelect: () => void;
}) {
  const row = (label: string, value: ReactNode) => (
    <div className="flex items-baseline justify-between gap-3 text-subtle">
      <dt className="text-foreground">{label}</dt>
      <dd className="font-bold tabular-nums text-foreground">{value}</dd>
    </div>
  );
  return (
    <Card
      onClick={onSelect}
      className={cn(
        panelCardClassName,
        'flex cursor-pointer flex-col gap-4 p-card transition-colors',
        active ? 'border-2 border-primary' : 'hover:border-primary/40',
      )}
    >
      <h3 className="truncate text-body font-bold text-foreground" title={card.name}>{card.name}</h3>
      <dl className="space-y-2">
        {row(axis === 'project' ? '期間の排出量（自社）' : '期間の排出量', formatTonsUnit(card.total))}
        {row(axis === 'project' ? '参加拠点' : '参加プロジェクト', axis === 'project' ? `${card.members.length}拠点` : `${card.members.length}件`)}
        {axis === 'site' && row('最終提出日', formatDate(card.lastSubmittedAt))}
      </dl>
      {axis === 'project' && (
        <ul className="flex flex-wrap gap-1.5" aria-label="参加拠点">
          {card.members.map((name) => (
            <li key={name} className="rounded-md bg-muted px-2 py-0.5 text-xs text-foreground">{name}</li>
          ))}
        </ul>
      )}
      {axis === 'project' && card.projectReduction != null && (
        <div className="border-t border-border pt-3">
          {row('プロジェクト全体の削減量', formatTonsUnit(card.projectReduction))}
        </div>
      )}
      <Button
        type="button"
        variant={active ? 'default' : 'outline'}
        className="mt-auto w-full rounded-full"
        aria-pressed={active}
        onClick={(event) => {
          event.stopPropagation();
          onSelect();
        }}
      >
        {active ? 'グラフに表示中' : 'グラフに表示'}
      </Button>
    </Card>
  );
}

const CALENDAR_STATUS: Record<CalendarStatus, { label: string; className: string; icon: ReactNode }> = {
  processed: {
    label: '処理済',
    className: 'bg-primary text-primary-foreground',
    icon: <Check className="h-3.5 w-3.5" strokeWidth={3} />,
  },
  submitted: {
    label: '提出済・未処理',
    className: 'border-2 border-primary bg-background text-primary',
    icon: <Clock className="h-3.5 w-3.5" strokeWidth={2.5} />,
  },
  missing: {
    label: '未提出',
    className: 'border-2 border-dashed border-warning bg-warning-subtle text-warning-strong',
    icon: <span className="text-xs font-bold leading-none">!</span>,
  },
  outOfScope: { label: '対象外', className: 'bg-muted', icon: null },
};

function StatusSwatch({ status, className }: { status: CalendarStatus; className?: string }) {
  const presentation = CALENDAR_STATUS[status];
  return (
    <span className={cn('flex items-center justify-center rounded-md', presentation.className, className)} aria-hidden="true">
      {presentation.icon}
    </span>
  );
}

function SubmissionCalendar({
  rows,
  months,
  loading,
  error,
}: {
  rows: CalendarRow[];
  months: string[];
  loading: boolean;
  error: boolean;
}) {
  const gridTemplateColumns = `minmax(11rem,14rem) repeat(${months.length}, minmax(2.25rem,1fr)) 4.5rem`;
  return (
    <AdminTableCard
      title="モニタリング提出カレンダー"
      description="記録日（dateRecorded）の月ごとの提出状況"
      action={
        <ul aria-label="凡例" className="flex flex-wrap items-center gap-x-4 gap-y-2 text-subtle text-foreground">
          {(Object.keys(CALENDAR_STATUS) as CalendarStatus[]).map((status) => (
            <li key={status} className="flex items-center gap-1.5">
              <StatusSwatch status={status} className="h-5 w-5" />
              {CALENDAR_STATUS[status].label}
            </li>
          ))}
        </ul>
      }
    >
      {loading ? (
        <SkeletonTable columns={3} rows={3} />
      ) : error ? (
        <p role="alert" className="py-6 text-center text-small text-destructive">モニタリングデータを取得できませんでした。</p>
      ) : rows.length === 0 ? (
        <p className="py-6 text-center text-small text-muted-foreground">対象の拠点・プロジェクトはありません</p>
      ) : (
        <div className="overflow-x-auto">
          <div role="table" aria-label="モニタリング提出カレンダー" className="min-w-max space-y-2">
            <div role="row" className="grid items-end gap-1.5 text-subtle text-muted-foreground" style={{ gridTemplateColumns }}>
              <span role="columnheader" className="sr-only">拠点 / プロジェクト</span>
              {months.map((month, index) => (
                <span role="columnheader" key={month} className="text-center tabular-nums" aria-label={formatChartPeriod(month)}>
                  {shortMonth(month, index)}
                </span>
              ))}
              <span role="columnheader" className="text-right">提出率</span>
            </div>
            {rows.map((row) => (
              <div role="row" key={`${row.pair.siteId}-${row.pair.projectId}`} className="grid items-center gap-1.5" style={{ gridTemplateColumns }}>
                <span role="rowheader" className="min-w-0 pr-2">
                  <span className="block truncate text-body font-bold text-foreground">{row.pair.siteName}</span>
                  <span className="block truncate text-subtle text-muted-foreground">{row.pair.projectName}</span>
                </span>
                {row.cells.map((cell) => (
                  <span role="cell" key={cell.month}>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <button
                          type="button"
                          className="block w-full rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                          aria-label={`${formatChartPeriod(cell.month)} ${CALENDAR_STATUS[cell.status].label}`}
                        >
                          <StatusSwatch status={cell.status} className="h-8 w-full" />
                        </button>
                      </TooltipTrigger>
                      <TooltipContent className="space-y-0.5 text-xs">
                        <p className="font-bold">{formatChartPeriod(cell.month)}：{CALENDAR_STATUS[cell.status].label}</p>
                        <p>記録数：{cell.count}件</p>
                        <p>最終記録日：{formatDate(cell.lastRecordedAt)}</p>
                        <p>発電量合計：{cell.elPvTotal === null ? '—' : `${new Intl.NumberFormat('ja-JP').format(cell.elPvTotal)} kWh`}</p>
                      </TooltipContent>
                    </Tooltip>
                  </span>
                ))}
                <span role="cell" className="text-right text-body font-bold tabular-nums text-foreground">
                  {row.submitted} / {row.eligible}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </AdminTableCard>
  );
}

/** Master アナリティクス: compare emissions and submissions by project or by site. */
export function MasterAnalyticsSection() {
  const [axis, setAxis] = useState<AnalysisAxis>('project');
  const [period, setPeriod] = useState<AnalyticsPeriod>('12');
  const [selected, setSelected] = useState<Record<AnalysisAxis, string | null>>({ project: null, site: null });
  const data = useMasterAnalyticsData({ axis, period, selectedId: selected[axis] });
  const select = (nextAxis: AnalysisAxis, id: string) => {
    setAxis(nextAxis);
    setSelected((current) => ({ ...current, [nextAxis]: id }));
  };

  const controls = (
    <div className="flex flex-wrap items-end gap-4">
      <ControlField label="分析軸">
        <SegmentGroup aria-label="分析軸">
          {AXIS_OPTIONS.map((option) => (
            <SegmentButton key={option.value} active={axis === option.value} onClick={() => setAxis(option.value)}>
              {option.label}
            </SegmentButton>
          ))}
        </SegmentGroup>
      </ControlField>
      <ControlField label="期間">
        <Select value={period} onValueChange={(value) => setPeriod(value as AnalyticsPeriod)}>
          <SelectTrigger className="w-44" aria-label="期間">
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
      action={controls}
    />
  );

  if (data.error) {
    return (
      <div className="space-y-section">
        {header}
        <div role="alert" className="rounded-xl border border-destructive/30 bg-destructive/5 p-6 text-center text-small text-destructive">
          {data.error instanceof Error ? data.error.message : 'データの取得に失敗しました'}
        </div>
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
      <div className="space-y-3">
        {header}
        {active && (
          <p className="text-subtle text-muted-foreground">
            表示中：<span className="font-bold text-foreground">{active.name}</span>
            （{active.members.length}{memberUnit}）・{periodLabel}
          </p>
        )}
      </div>

      {!data.isBaseLoading && !data.hasTargets ? (
        <div className="rounded-xl border border-border bg-card p-8 text-center text-small text-muted-foreground shadow-app-sm">
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

          <section aria-label={axis === 'project' ? 'プロジェクト' : '拠点'} className="space-y-4">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <h2 className="text-subsection-title font-bold text-foreground">{axis === 'project' ? 'プロジェクト' : '拠点'}</h2>
              <p className="text-subtle text-muted-foreground">カードを選択すると上のグラフが切り替わります</p>
            </div>
            {data.isEmissionLoading ? (
              <SkeletonList items={3} />
            ) : (
              <div className="grid gap-grid sm:grid-cols-2 xl:grid-cols-3">
                {data.cards.map((card) => (
                  <SelectorCardItem
                    key={card.id}
                    axis={axis}
                    card={card}
                    active={card.id === active?.id}
                    onSelect={() => select(axis, card.id)}
                  />
                ))}
              </div>
            )}
          </section>

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

          <SubmissionCalendar
            rows={data.calendarRows}
            months={data.monitoringMonths}
            loading={data.isRecordsLoading}
            error={data.recordsError}
          />
        </>
      )}
    </div>
  );
}
