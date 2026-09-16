import { cn } from '@/lib/utils';
import { calculateEmissionReductionProgress } from './emissionReductionProgressUtils';

type EmissionReductionProgressCardProps = {
  baselineEmission: number | null;
  projectEmission: number | null;
  targetReduction: number;
  className?: string;
};

function formatNumber(value: number | null, maximumFractionDigits = 1) {
  if (value === null || !Number.isFinite(value)) return '-';
  return value.toLocaleString('ja-JP', { maximumFractionDigits });
}

/** Renders the shared emission-reduction calculation in the project detail card. */
export function EmissionReductionProgressCard({
  baselineEmission,
  projectEmission,
  targetReduction,
  className,
}: EmissionReductionProgressCardProps) {
  const metrics = calculateEmissionReductionProgress({
    baselineEmission,
    projectEmission,
    targetReduction,
  });

  return (
    <section
      className={cn(
        'flex min-h-[320px] flex-col rounded-2xl border border-border bg-card p-5 shadow-app-sm',
        className,
      )}
    >
      <div className='mb-6'>
        <h3 className='text-xl font-bold text-foreground'>
          排出削減量
        </h3>
      </div>

      <div className='flex flex-1 flex-col justify-center'>
        <div className='text-center'>
          <p className='text-5xl font-bold leading-none text-foreground'>
            {metrics === null
              ? '-'
              : metrics.achievementRate.toLocaleString('ja-JP', {
                  maximumFractionDigits: 1,
                })}
            {metrics !== null ? (
              <span className='ml-1 text-2xl'>%</span>
            ) : null}
          </p>
        </div>

        <div className='mt-8'>
          <div className='mb-2 flex justify-between text-subtle text-foreground'>
            <span>0%</span>
            <span>25%</span>
            <span>50%</span>
            <span>75%</span>
            <span>100%</span>
          </div>

          <div className='h-4 overflow-hidden rounded-full bg-secondary'>
            <div
              className='h-full rounded-full bg-primary transition-all'
              style={{
                width:
                  metrics === null
                    ? '0%'
                    : `${metrics.progressValue}%`,
              }}
            />
          </div>
        </div>

        <div className='mt-8 grid grid-cols-3 gap-4 border-t border-border pt-6'>
          <div className='text-center'>
            <p className='text-small text-foreground'>達成</p>
            <p className='mt-2 text-large font-bold text-foreground'>
              {formatNumber(metrics?.reductionAmount ?? null)} tCO₂
            </p>
          </div>

          <div className='text-center'>
            <p className='text-small text-foreground'>目標</p>
            <p className='mt-2 text-large font-bold text-foreground'>
              {formatNumber(targetReduction)} tCO₂
            </p>
          </div>

          <div className='text-center'>
            <p className='text-small text-foreground'>残り</p>
            <p className='mt-2 text-large font-bold text-foreground'>
              {formatNumber(metrics?.remainingReduction ?? null)} tCO₂
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
