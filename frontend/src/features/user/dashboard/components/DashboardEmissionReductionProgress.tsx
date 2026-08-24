import { useEffect, useState } from 'react';
import {
  getProjectEmissionSummary,
  type ProjectEmissionSummary,
} from '@/api/batch';
import { logger } from '@/lib/logger';
import { EmissionReductionProgress } from '@/features/user/project-detail/components/EmissionReductionProgress';
import { PROJECT_EMISSION_REDUCTION_TARGET } from '@/features/user/project-detail/components/emissionReductionProgressUtils';
import { SkeletonChart } from '@/components/feedback/loading';

type DashboardEmissionReductionProgressProps = {
  projectId: string;
};

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

  return (
    <div className='min-h-[280px] rounded-2xl p-5 xl:order-1 border-border border'>
      {isLoading ? (
        <SkeletonChart className='min-h-[240px] border-0 p-0 shadow-none' />
      ) : error ? (
        <div
          className='flex min-h-[240px] items-center justify-center text-center text-destructive'
          role='alert'
        >
          {error}
        </div>
      ) : (
        <EmissionReductionProgress
          baselineEmission={summary?.baselineEmission ?? null}
          projectEmission={summary?.projectEmission ?? null}
          targetReduction={PROJECT_EMISSION_REDUCTION_TARGET}
          className='m-0 border-0 p-0'
        />
      )}
    </div>
  );
}
