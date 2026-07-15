import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from 'recharts';
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart';
import type { ProjectEmissionBreakdown } from '@/api/batch';
import { cn } from '@/lib/utils';
import { EmissionReductionProgress } from './EmissionReductionProgress';
import type { EmissionReductionProgressInput } from './emissionReductionProgressUtils';

const EMISSION_VALUE_KEY = 'value';

const emissionBreakdownChartConfig = {
  [EMISSION_VALUE_KEY]: {
    label: '排出量',
    // color: 'hsl(var(--chart-2))',
    color: 'hsl(var(--primary))',
  },
} satisfies ChartConfig;

const emissionNumberFormatter = new Intl.NumberFormat('ja-JP', {
  maximumFractionDigits: 3,
});

type EmissionBreakdownPoint = {
  label: string;
  value: number;
};

function createEmissionBreakdownData(
  breakdown: ProjectEmissionBreakdown | null,
): EmissionBreakdownPoint[] {
  if (!breakdown) return [];

  const breakdownEntries = [
    {
      label: '電力制御装置由来排出量',
      value: breakdown.controlDeviceEmission,
    },
    {
      label: '蓄電池由来排出量',
      value: breakdown.batteryEmission,
    },
    {
      label: '主要排出量',
      value: breakdown.mainEmission,
    },
  ];

  return breakdownEntries.flatMap((entry) =>
    entry.value === null ? [] : [{ ...entry, value: entry.value }],
  );
}

type EmissionBreakdownChartProps = {
  data: ProjectEmissionBreakdown | null;
  isLoading: boolean;
  error: string | null;
  reductionProgress: EmissionReductionProgressInput;
  className?: string;
};

export function EmissionBreakdownChart({
  data,
  isLoading,
  error,
  reductionProgress,
  className,
}: EmissionBreakdownChartProps) {
  const chartData = createEmissionBreakdownData(data);

  return (
    <section
      className={cn(
        'flex h-full min-h-[360px] flex-col rounded-2xl border border-border bg-card p-5 shadow-app-sm',
        className,
      )}
      aria-labelledby='emission-breakdown-title'
    >
      <h3 id='emission-breakdown-title' className='leading-8 text-foreground'>
        プロジェクト実施後排出量内訳
      </h3>

      <div className='mt-4 flex min-h-[280px] flex-1 items-center justify-center'>
        {isLoading ? (
          <p className='text-subtle text-muted-foreground'>読み込み中...</p>
        ) : error ? (
          <p className='text-subtle text-destructive' role='alert'>
            {error}
          </p>
        ) : chartData.length === 0 ? (
          <p className='text-subtle text-muted-foreground'>
            表示できるデータがありません。
          </p>
        ) : (
          <div className='w-full'>
            <ChartContainer
              config={emissionBreakdownChartConfig}
              className='h-[245px] w-full aspect-auto'
            >
              <BarChart
                accessibilityLayer
                data={chartData}
                layout='vertical'
                margin={{ top: 8, right: 20, bottom: 8, left: 4 }}
              >
                <CartesianGrid horizontal={false} />
                <XAxis
                  type='number'
                  dataKey={EMISSION_VALUE_KEY}
                  tickLine={false}
                  axisLine={false}
                  tickMargin={10}
                  tickFormatter={(value: number) =>
                    emissionNumberFormatter.format(value)
                  }
                />
                <YAxis
                  type='category'
                  dataKey='label'
                  tickLine={false}
                  axisLine={false}
                  tickMargin={8}
                  width={150}
                  interval={0}
                />
                <ChartTooltip
                  cursor={false}
                  content={
                    <ChartTooltipContent
                      labelFormatter={(label) => String(label)}
                      formatter={(value) => (
                        <span className='font-mono font-medium tabular-nums text-foreground'>
                          {emissionNumberFormatter.format(Number(value))} tCO₂
                        </span>
                      )}
                    />
                  }
                />
                <Bar
                  dataKey={EMISSION_VALUE_KEY}
                  fill={`var(--color-${EMISSION_VALUE_KEY})`}
                  radius={[0, 4, 4, 0]}
                />
              </BarChart>
            </ChartContainer>
            <p className='text-subtle mt-2 text-center text-muted-foreground'>
              排出量（tCO₂）
            </p>
          </div>
        )}
      </div>

      {!isLoading && !error ? (
        <EmissionReductionProgress {...reductionProgress} />
      ) : null}
    </section>
  );
}
