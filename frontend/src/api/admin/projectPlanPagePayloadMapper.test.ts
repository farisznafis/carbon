import { describe, expect, it } from 'vitest';

import { mapPlanPagePayload } from './projectPayloadMapper';
import { PROJECT_PLAN_PAGES } from '@/features/admin/project-registration/forms/projectPlanPages';

const otherOperator = PROJECT_PLAN_PAGES.find(
  (page) => page.slug === 'other-operator-projects',
);

describe('mapPlanPagePayload', () => {
  it('passes fields through untouched when the names already match', () => {
    const payload = mapPlanPagePayload(
      { projChangeInfo: { reviewerName: '一般財団法人日本海事協会' } },
      'projChangeInfo',
    );

    expect(payload).toEqual({ reviewerName: '一般財団法人日本海事協会' });
  });

  it('formats dates as yyyy-MM-dd for the backend @DateFormat', () => {
    const payload = mapPlanPagePayload(
      { appConditions: { esgPledgeCaseDate: new Date('2026-08-12T09:30:00.000Z') } },
      'appConditions',
    );

    expect(payload.esgPledgeCaseDate).toBe('2026-08-12');
  });

  it('omits an invalid date rather than sending "Invalid Date"', () => {
    const payload = mapPlanPagePayload(
      { appConditions: { esgPledgeCaseDate: new Date('nonsense') } },
      'appConditions',
    );

    expect(payload).not.toHaveProperty('esgPledgeCaseDate');
  });

  it('returns an empty payload when the page has not been filled', () => {
    expect(mapPlanPagePayload({}, 'projChangeInfo')).toEqual({});
  });

  describe('renames', () => {
    it('applies the declared renames and drops the form-only aliases', () => {
      const payload = mapPlanPagePayload(
        {
          otherProjectOperator: {
            projectOperatorNameKana: 'カブシキガイシャマルマル',
            hasEnergyActReport: true,
            energyManageFactoryNumber: '0000001',
            otherCorpNumber: '9999999999999',
          },
        },
        'otherProjectOperator',
        otherOperator?.renames,
      );

      // Renamed to what the backend DTO declares.
      expect(payload.projectOperatorKana).toBe('カブシキガイシャマルマル');
      expect(payload.hasEnergyActPeriodReport).toBe(true);
      expect(payload.energyManagFactoryNumber).toBe('0000001');
      // Untouched where the names already agree.
      expect(payload.otherCorpNumber).toBe('9999999999999');
      // The form-side names must not reach the backend.
      expect(payload).not.toHaveProperty('projectOperatorNameKana');
      expect(payload).not.toHaveProperty('hasEnergyActReport');
      expect(payload).not.toHaveProperty('energyManageFactoryNumber');
    });
  });
});

describe('PROJECT_PLAN_PAGES', () => {
  it('has no duplicate steps, slugs or form keys', () => {
    const steps = PROJECT_PLAN_PAGES.map((page) => page.step);
    const slugs = PROJECT_PLAN_PAGES.map((page) => page.slug);
    const keys = PROJECT_PLAN_PAGES.map((page) => page.formKey);

    expect(new Set(steps).size).toBe(steps.length);
    expect(new Set(slugs).size).toBe(slugs.length);
    expect(new Set(keys).size).toBe(keys.length);
  });

  it('never claims step 1, which is the SQL project record', () => {
    expect(PROJECT_PLAN_PAGES.some((page) => page.step === 1)).toBe(false);
  });
});
