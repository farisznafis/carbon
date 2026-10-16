import { useEffect, useState } from 'react';
import {
  getProjectEmissionSummary,
  type ProjectEmissionSummary,
} from '@/api/batch';
import { logger } from '@/lib/logger';
import { Progress } from '@/components/ui/progress';
import { Skeleton } from '@/components/ui/skeleton';
import {
  calculateEmissionReductionProgress,
  PROJECT_EMISSION_REDUCTION_TARGET,
} from '@/features/user/project-detail/components/emissionReductionProgressUtils';

const numberFormatter = new Intl.NumberFormat('ja-JP', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

type DashboardEmissionReductionProgressProps = {
  projectId: string;
};

/** Compact one-row emission reduction progress for the site dashboard project list. */
export function DashboardEmissionReductionProgress({
  projectId,
}: DashboardEmissionReductionProgressProps) {
  const [summary, setSummary] = useState<ProjectEmissionSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isCancelled = false;

    const loadEmissionSummary = async () => {
      try {
        setIsLoading(true);
        setError(null);

        const emissionSummary = await getProjectEmissionSummary(
          String(projectId),
        );

        if (!isCancelled) {
          setSummary(emissionSummary);
        }
      } catch (loadError) {
        logger.error('Error loading dashboard emission summary:', loadError);

        if (!isCancelled) {
          setSummary(null);
          setError('排出削減量データの取得に失敗しました。');
        }
      } finally {
        if (!isCancelled) {
          setIsLoading(false);
        }
      }
    };

    void loadEmissionSummary();

    return () => {
      isCancelled = true;
    };
  }, [projectId]);

  if (isLoading) {
    return (
      <div className='space-y-2' aria-busy='true'>
        <Skeleton className='h-5 w-1/2' />
        <Skeleton className='h-2 w-full' />
        <Skeleton className='h-4 w-2/3' />
      </div>
    );
  }

  if (error) {
    return (
      <p role='alert' className='text-small text-destructive'>
        {error}
      </p>
    );
  }

  const metrics = calculateEmissionReductionProgress({
    baselineEmission: summary?.baselineEmission ?? null,
    projectEmission: summary?.projectEmission ?? null,
    targetReduction: PROJECT_EMISSION_REDUCTION_TARGET,
  });

  if (!metrics) {
    return (
      <p className='text-small text-muted-foreground'>
        排出削減量のデータはまだありません。
      </p>
    );
  }

  const rate = numberFormatter.format(metrics.achievementRate);

  return (
    <div>
      <div className='flex items-baseline gap-3'>
        {/* Same label style as 方法論番号 / 方法論 in the project rows. */}
        <span className='text-subtle text-muted-foreground'>排出削減量</span>
        <span className='text-lg font-bold tabular-nums text-foreground'>
          {rate}%
        </span>
      </div>
      {/* bg-muted matches the card, so the track gets a primary tint to show the full 100% length. */}
      <Progress
        value={metrics.progressValue}
        className='mt-2 h-2 bg-primary/15'
        aria-label={`排出削減目標の達成率 ${rate}%`}
      />
      <p className='mt-1.5 text-subtle tabular-nums text-muted-foreground'>
        {numberFormatter.format(metrics.reductionAmount)} /{' '}
        {numberFormatter.format(PROJECT_EMISSION_REDUCTION_TARGET)} tCO₂ ・ 残り{' '}
        {numberFormatter.format(metrics.remainingReduction)} tCO₂
      </p>
    </div>
  );
}
