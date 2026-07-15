import { describe, expect, it } from 'vitest';
import { calculateEmissionReductionProgress } from './emissionReductionProgressUtils';

describe('calculateEmissionReductionProgress', () => {
  it('calculates achievement and remaining reduction', () => {
    expect(
      calculateEmissionReductionProgress({
        baselineEmission: 100,
        projectEmission: 31.6,
        targetReduction: 100,
      }),
    ).toEqual({
      achievementRate: 68.4,
      progressValue: 68.4,
      reductionAmount: 68.4,
      remainingReduction: 31.6,
    });
  });

  it('caps the achievement rate and progress fill at 100 percent', () => {
    expect(
      calculateEmissionReductionProgress({
        baselineEmission: 150,
        projectEmission: 25,
        targetReduction: 100,
      }),
    ).toEqual({
      achievementRate: 100,
      progressValue: 100,
      reductionAmount: 125,
      remainingReduction: 0,
    });
  });

  it('returns null when required data is unavailable', () => {
    expect(
      calculateEmissionReductionProgress({
        baselineEmission: null,
        projectEmission: 31.6,
        targetReduction: 100,
      }),
    ).toBeNull();
  });

  it('uses API embl and empj values against the current target', () => {
    expect(
      calculateEmissionReductionProgress({
        baselineEmission: 0.2,
        projectEmission: 120.5,
        targetReduction: 100,
      }),
    ).toEqual({
      achievementRate: -120.3,
      progressValue: 0,
      reductionAmount: -120.3,
      remainingReduction: 220.3,
    });
  });
});
