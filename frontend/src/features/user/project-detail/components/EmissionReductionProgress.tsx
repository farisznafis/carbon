import { Progress } from '@/components/ui/progress';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import {
  calculateEmissionReductionProgress,
  type EmissionReductionProgressInput,
} from './emissionReductionProgressUtils';

const percentageFormatter = new Intl.NumberFormat('ja-JP', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

const emissionNumberFormatter = new Intl.NumberFormat('ja-JP', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

type EmissionReductionProgressProps = EmissionReductionProgressInput;

export function EmissionReductionProgress({
  baselineEmission,
  projectEmission,
  targetReduction,
}: EmissionReductionProgressProps) {
  const metrics = calculateEmissionReductionProgress({
    baselineEmission,
    projectEmission,
    targetReduction,
  });

  return (
    <section
      className='mt-6 border-t border-border pt-6'
      aria-labelledby='emission-reduction-progress-title'
    >
      <h3
        id='emission-reduction-progress-title'
        className='leading-8 text-foreground'
      >
        排出削減量
      </h3>

      {!metrics ? (
        <p className='text-subtle py-10 text-center text-muted-foreground'>
          データがありません。
        </p>
      ) : (
        <div className='mt-5'>
          <p className='text-center text-3xl font-bold tabular-nums text-foreground'>
            {percentageFormatter.format(metrics.achievementRate)}%
          </p>

          <div className='mt-5'>
            <div
              className='text-subtle mb-2 flex justify-between text-muted-foreground'
              aria-hidden='true'
            >
              <span>0%</span>
              <span>25%</span>
              <span>50%</span>
              <span>75%</span>
              <span>100%</span>
            </div>

            <Tooltip>
              <TooltipTrigger asChild>
                <div
                  className='rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2'
                  tabIndex={0}
                >
                  <Progress
                    value={metrics.progressValue}
                    aria-label={`排出削減目標の達成率 ${percentageFormatter.format(metrics.achievementRate)}%`}
                  />
                </div>
              </TooltipTrigger>
              <TooltipContent className='space-y-1.5' side='top'>
                <div className='flex min-w-[15rem] justify-between gap-4'>
                  <span>排出削減量</span>
                  <strong>
                    {emissionNumberFormatter.format(metrics.reductionAmount)}{' '}
                    tCO₂
                  </strong>
                </div>
                <div className='flex justify-between gap-4'>
                  <span>ベースライン排出量</span>
                  <strong>
                    {emissionNumberFormatter.format(baselineEmission)} tCO₂
                  </strong>
                </div>
                <div className='flex justify-between gap-4'>
                  <span>プロジェクト実施後排出量</span>
                  <strong>
                    {emissionNumberFormatter.format(projectEmission)} tCO₂
                  </strong>
                </div>
              </TooltipContent>
            </Tooltip>
          </div>

          <dl className='mt-6 grid grid-cols-3 divide-x divide-border text-center'>
            <div className='px-2'>
              <dt className='text-subtle text-muted-foreground'>達成</dt>
              <dd className='mt-1 font-bold tabular-nums text-foreground'>
                {emissionNumberFormatter.format(metrics.reductionAmount)} tCO₂
              </dd>
            </div>
            <div className='px-2'>
              <dt className='text-subtle text-muted-foreground'>目標</dt>
              <dd className='mt-1 font-bold tabular-nums text-foreground'>
                {emissionNumberFormatter.format(targetReduction)} tCO₂
              </dd>
            </div>
            <div className='px-2'>
              <dt className='text-subtle text-muted-foreground'>残り</dt>
              <dd className='mt-1 font-bold tabular-nums text-foreground'>
                {emissionNumberFormatter.format(metrics.remainingReduction)}{' '}
                tCO₂
              </dd>
            </div>
          </dl>
        </div>
      )}
    </section>
  );
}
