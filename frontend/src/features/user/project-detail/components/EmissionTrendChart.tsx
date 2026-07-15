import {
  CartesianGrid,
  Line,
  LineChart,
  XAxis,
  YAxis,
} from 'recharts';
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart';
import type { EmissionTrendPoint } from '@/api/batch';
import { cn } from '@/lib/utils';

const EMISSION_SERIES_KEY = 'y';
const EMISSION_SERIES_LABEL = 'プロジェクト実施後排出量';

const emissionTrendChartConfig = {
  [EMISSION_SERIES_KEY]: {
    label: EMISSION_SERIES_LABEL,
    color: 'hsl(var(--primary))',
  },
} satisfies ChartConfig;

const emissionNumberFormatter = new Intl.NumberFormat('ja-JP', {
  maximumFractionDigits: 3,
});

function formatProcessingDate(processingDate: string) {
  const dateParts = processingDate.match(/^(\d{4})-(\d{2})-(\d{2})/);

  if (dateParts) {
    return `${dateParts[1]}年${dateParts[2]}月${dateParts[3]}日`;
  }

  return processingDate;
}

type EmissionTrendChartProps = {
  data: EmissionTrendPoint[];
  isLoading: boolean;
  error: string | null;
  className?: string;
};

export function EmissionTrendChart({
  data,
  isLoading,
  error,
  className,
}: EmissionTrendChartProps) {
  return (
    <section
      className={cn(
        'flex h-full min-h-[360px] flex-col rounded-2xl border border-border bg-card p-5 shadow-app-sm',
        className,
      )}
      aria-labelledby='emission-trend-title'
    >
      <h3 id='emission-trend-title' className='leading-8 text-foreground'>
        プロジェクト実施後排出量の推移
      </h3>

      <div className='mt-4 flex min-h-[280px] flex-1 items-center justify-center'>
        {isLoading ? (
          <p className='text-subtle text-muted-foreground'>読み込み中...</p>
        ) : error ? (
          <p className='text-subtle text-destructive' role='alert'>
            {error}
          </p>
        ) : data.length === 0 ? (
          <p className='text-subtle text-muted-foreground'>
            排出量データがありません。
          </p>
        ) : (
          <div className='w-full'>
            <p className='text-subtle mb-2 ml-1 text-muted-foreground'>
              排出量（tCO₂）
            </p>
            <ChartContainer
              config={emissionTrendChartConfig}
              className='h-[230px] w-full aspect-auto'
            >
              <LineChart
                accessibilityLayer
                data={data}
                margin={{ top: 8, right: 16, bottom: 8, left: 8 }}
              >
                <CartesianGrid vertical={false} />
                <XAxis
                  dataKey='x'
                  tickLine={false}
                  axisLine={false}
                  minTickGap={24}
                  tickMargin={14}
                  tickFormatter={formatProcessingDate}
                  height={42}
                />
                <YAxis
                  tickLine={false}
                  axisLine={false}
                  tickFormatter={(value: number) =>
                    emissionNumberFormatter.format(value)
                  }
                  width={52}
                />
                <ChartTooltip
                  cursor={false}
                  content={
                    <ChartTooltipContent
                      labelFormatter={(label) =>
                        formatProcessingDate(String(label))
                      }
                      formatter={(value) => (
                        <div className='flex min-w-[12rem] items-center justify-between gap-4'>
                          <span className='text-muted-foreground'>
                            {EMISSION_SERIES_LABEL}
                          </span>
                          <span className='font-mono font-medium tabular-nums text-foreground'>
                            {emissionNumberFormatter.format(Number(value))}{' '}
                            tCO₂
                          </span>
                        </div>
                      )}
                    />
                  }
                />
                <Line
                  dataKey={EMISSION_SERIES_KEY}
                  type='monotone'
                  stroke={`var(--color-${EMISSION_SERIES_KEY})`}
                  strokeWidth={2}
                  dot={{ r: 4 }}
                  activeDot={{ r: 6 }}
                />
              </LineChart>
            </ChartContainer>
            <p className='text-subtle mt-2 text-center text-muted-foreground'>
              算定期間
            </p>
            <div
              className='text-subtle mt-5 flex items-center justify-center gap-2 text-foreground'
              aria-label='凡例'
            >
              <span
                className='h-2.5 w-2.5 shrink-0 rounded-sm'
                style={{ backgroundColor: 'hsl(var(--primary)' }}
                aria-hidden='true'
              />
              <span>{EMISSION_SERIES_LABEL}</span>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
