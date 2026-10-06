import { Progress } from '@/components/ui/progress';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import {
  calculateEmissionReductionProgress,
  type EmissionReductionProgressInput,
} from '@/features/user/project-detail/components/emissionReductionProgressUtils';
import { cn } from '@/lib/utils';

const oneDecimalFormatter = new Intl.NumberFormat('ja-JP', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

type ReductionMeterProps = EmissionReductionProgressInput & {
  className?: string;
};

/** Compact emission reduction meter for dashboard project rows. */
export function ReductionMeter({
  baselineEmission,
  projectEmission,
  targetReduction,
  className,
}: ReductionMeterProps) {
  const metrics = calculateEmissionReductionProgress({
    baselineEmission,
    projectEmission,
    targetReduction,
  });

  if (!metrics || targetReduction === null) {
    return (
      <div className={cn('text-small text-muted-foreground', className)}>
        排出削減量のデータはまだありません
      </div>
    );
  }

  const rate = oneDecimalFormatter.format(metrics.achievementRate);

  return (
    <div className={cn('w-full', className)}>
      <div className='flex items-baseline justify-between gap-3'>
        <span className='text-subtle font-medium text-muted-foreground'>
          排出削減 達成率
        </span>
        <span className='text-[28px] font-semibold leading-none tracking-tight tabular-nums text-foreground'>
          {rate}
          <span className='ml-0.5 text-base font-medium text-muted-foreground'>
            %
          </span>
        </span>
      </div>

      <Tooltip>
        <TooltipTrigger asChild>
          <div
            className='relative z-10 mt-3 rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2'
            tabIndex={0}
          >
            <Progress
              value={metrics.progressValue}
              className='h-2 bg-secondary'
              aria-label={`排出削減目標の達成率 ${rate}%`}
            />
          </div>
        </TooltipTrigger>
        <TooltipContent
          className='space-y-1.5'
          side='top'
        >
          <div className='flex min-w-[15rem] justify-between gap-4'>
            <span>排出削減量</span>
            <strong className='tabular-nums'>
              {oneDecimalFormatter.format(metrics.reductionAmount)} tCO₂
            </strong>
          </div>
          <div className='flex justify-between gap-4'>
            <span>ベースライン排出量</span>
            <strong className='tabular-nums'>
              {oneDecimalFormatter.format(baselineEmission ?? 0)} tCO₂
            </strong>
          </div>
          <div className='flex justify-between gap-4'>
            <span>プロジェクト実施後排出量</span>
            <strong className='tabular-nums'>
              {oneDecimalFormatter.format(projectEmission ?? 0)} tCO₂
            </strong>
          </div>
        </TooltipContent>
      </Tooltip>

      <p className='text-subtle mt-2 flex flex-wrap items-baseline justify-between gap-x-3 tabular-nums text-muted-foreground'>
        <span>
          <span className='font-semibold text-foreground'>
            {oneDecimalFormatter.format(metrics.reductionAmount)}
          </span>{' '}
          / {oneDecimalFormatter.format(targetReduction)} tCO₂
        </span>
        <span>
          残り {oneDecimalFormatter.format(metrics.remainingReduction)} tCO₂
        </span>
      </p>
    </div>
  );
}
