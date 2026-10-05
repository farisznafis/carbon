import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  XAxis,
  YAxis,
} from 'recharts';
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart';
import { cn } from '@/lib/utils';
import { formatChartPeriod, formatEmissionValue } from './chartFormatters';

export type TimeSeriesLineSeries = {
  /** Data key in each row; also used as the CSS color variable suffix. */
  key: string;
  label: string;
  color: string;
};

export type TimeSeriesReferenceLine = {
  key: string;
  label: string;
  value: number;
  color: string;
};

export type TimeSeriesRow = {
  period: string;
} & Record<string, string | number | null>;

type TimeSeriesLineChartProps = {
  data: readonly TimeSeriesRow[];
  series: readonly TimeSeriesLineSeries[];
  /** Constant values drawn as dashed horizontal lines, e.g. the baseline. */
  referenceLines?: readonly TimeSeriesReferenceLine[];
  unit?: string;
  yAxisLabel?: string;
  xAxisLabel?: string;
  ariaLabel: string;
  className?: string;
};

/**
 * Renders a period-based line chart with an accessible legend. Missing values
 * break the line instead of being drawn as zero.
 */
export function TimeSeriesLineChart({
  data,
  series,
  referenceLines = [],
  unit = 't-CO₂',
  yAxisLabel = `排出量（${unit}）`,
  xAxisLabel = '算定期間',
  ariaLabel,
  className,
}: Readonly<TimeSeriesLineChartProps>) {
  const chartConfig = Object.fromEntries(
    [...series, ...referenceLines].map((entry) => [
      entry.key,
      { label: entry.label, color: entry.color },
    ]),
  ) satisfies ChartConfig;
  const labelByKey = new Map(
    series.map((entry) => [entry.key, entry.label]),
  );
  const legendEntries = [
    ...series.map((entry) => ({ ...entry, dashed: false })),
    ...referenceLines.map((entry) => ({ ...entry, dashed: true })),
  ];

  return (
    <figure className={cn('w-full', className)} aria-label={ariaLabel}>
      <p className="mb-2 ml-1 text-subtle text-muted-foreground">
        {yAxisLabel}
      </p>
      <ChartContainer config={chartConfig} className="aspect-auto h-72 w-full">
        <LineChart
          accessibilityLayer
          data={[...data]}
          margin={{ top: 8, right: 16, bottom: 0, left: 0 }}
        >
          <CartesianGrid vertical={false} />
          <XAxis
            dataKey="period"
            tickLine={false}
            axisLine={false}
            minTickGap={24}
            tickMargin={12}
            tickFormatter={formatChartPeriod}
          />
          <YAxis
            tickLine={false}
            axisLine={false}
            width={56}
            tickFormatter={(value: number) => formatEmissionValue(value)}
          />
          <ChartTooltip
            cursor={false}
            content={
              <ChartTooltipContent
                labelFormatter={(label) => formatChartPeriod(String(label))}
                formatter={(value, name, item) => (
                  <div className="flex min-w-48 items-center justify-between gap-4">
                    <span className="flex items-center gap-2 text-foreground">
                      <span
                        className="h-2.5 w-2.5 shrink-0 rounded-sm"
                        style={{ backgroundColor: item.color }}
                        aria-hidden="true"
                      />
                      {labelByKey.get(String(name)) ?? name}
                    </span>
                    <span className="font-mono font-medium tabular-nums text-foreground">
                      {formatEmissionValue(Number(value))} {unit}
                    </span>
                  </div>
                )}
              />
            }
          />
          {referenceLines.map((line) => (
            <ReferenceLine
              key={line.key}
              y={line.value}
              stroke={`var(--color-${line.key})`}
              strokeDasharray="6 4"
              strokeWidth={1.5}
              ifOverflow="extendDomain"
            />
          ))}
          {series.map((entry) => (
            <Line
              key={entry.key}
              dataKey={entry.key}
              name={entry.key}
              type="monotone"
              stroke={`var(--color-${entry.key})`}
              strokeWidth={2}
              dot={{ r: 3 }}
              activeDot={{ r: 5 }}
              connectNulls={false}
            />
          ))}
        </LineChart>
      </ChartContainer>
      <p className="mt-1 text-center text-subtle text-muted-foreground">
        {xAxisLabel}
      </p>
      <figcaption>
        <ul
          aria-label="凡例"
          className="mt-4 flex flex-wrap items-center justify-center gap-x-5 gap-y-2 text-subtle text-foreground"
        >
          {legendEntries.map((entry) => (
            <li key={entry.key} className="flex min-w-0 items-center gap-2">
              <span
                aria-hidden="true"
                className={cn(
                  'shrink-0',
                  entry.dashed ? 'h-0 w-4 border-t-2 border-dashed' : 'h-2.5 w-2.5 rounded-sm',
                )}
                style={
                  entry.dashed
                    ? { borderColor: entry.color }
                    : { backgroundColor: entry.color }
                }
              />
              <span className="max-w-56 truncate" title={entry.label}>
                {entry.label}
              </span>
            </li>
          ))}
        </ul>
      </figcaption>
    </figure>
  );
}
