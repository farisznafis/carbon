export type EmissionReductionProgressInput = {
  baselineEmission: number | null;
  projectEmission: number | null;
  targetReduction: number | null;
};

export type EmissionReductionProgressMetrics = {
  achievementRate: number;
  progressValue: number;
  reductionAmount: number;
  remainingReduction: number;
};

export const PROJECT_EMISSION_REDUCTION_TARGET = 100;

function roundEmission(value: number) {
  return Math.round((value + Number.EPSILON) * 1000) / 1000;
}

export function calculateEmissionReductionProgress({
  baselineEmission,
  projectEmission,
  targetReduction,
}: EmissionReductionProgressInput): EmissionReductionProgressMetrics | null {
  if (
    baselineEmission === null ||
    projectEmission === null ||
    targetReduction === null ||
    !Number.isFinite(baselineEmission) ||
    !Number.isFinite(projectEmission) ||
    !Number.isFinite(targetReduction) ||
    targetReduction <= 0
  ) {
    return null;
  }

  const reductionAmount = roundEmission(baselineEmission - projectEmission);
  const achievementRate = Math.min(
    roundEmission((reductionAmount / targetReduction) * 100),
    100,
  );

  return {
    achievementRate,
    progressValue: Math.max(achievementRate, 0),
    reductionAmount,
    remainingReduction: Math.max(
      roundEmission(targetReduction - reductionAmount),
      0,
    ),
  };
}
