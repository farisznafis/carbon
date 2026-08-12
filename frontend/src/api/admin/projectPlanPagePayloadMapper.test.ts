import { describe, expect, it } from 'vitest';

import {
  mapApplicableConditionsPayload,
  mapOtherOperatorProjectsPayload,
} from './projectPayloadMapper';

describe('mapOtherOperatorProjectsPayload', () => {
  it('renames the fields whose form names differ from the backend DTO', () => {
    const payload = mapOtherOperatorProjectsPayload({
      otherProjectOperator: {
        projectOperatorNameKana: 'カブシキガイシャマルマル',
        hasEnergyActReport: true,
        energyManageFactoryNumber: '0000001',
      },
    });

    expect(payload.projectOperatorKana).toBe('カブシキガイシャマルマル');
    expect(payload.hasEnergyActPeriodReport).toBe(true);
    expect(payload.energyManagFactoryNumber).toBe('0000001');
  });

  it('drops the form-only aliases so they are never sent', () => {
    const payload = mapOtherOperatorProjectsPayload({
      otherProjectOperator: {
        projectOperatorNameKana: 'カナ',
        hasEnergyActReport: false,
        energyManageFactoryNumber: '1',
      },
    });

    expect(payload).not.toHaveProperty('projectOperatorNameKana');
    expect(payload).not.toHaveProperty('hasEnergyActReport');
    expect(payload).not.toHaveProperty('energyManageFactoryNumber');
  });

  it('passes through fields that already match', () => {
    const payload = mapOtherOperatorProjectsPayload({
      otherProjectOperator: {
        otherCorpNumber: '9999999999999',
        city: '●●市',
        address: '●●1-1-1',
      },
    });

    expect(payload).toMatchObject({
      otherCorpNumber: '9999999999999',
      city: '●●市',
      address: '●●1-1-1',
    });
  });

  it('returns an empty payload when the page has not been filled', () => {
    expect(mapOtherOperatorProjectsPayload({})).toEqual({});
  });
});

describe('mapApplicableConditionsPayload', () => {
  it('formats the pledge date as yyyy-MM-dd for the backend @DateFormat', () => {
    const payload = mapApplicableConditionsPayload({
      appConditions: {
        esgPledgeCaseDate: new Date('2026-08-12T09:30:00.000Z'),
      },
    });

    expect(payload.esgPledgeCaseDate).toBe('2026-08-12');
  });

  it('leaves an already-formatted date string alone', () => {
    const payload = mapApplicableConditionsPayload({
      appConditions: { esgPledgeCaseDate: '2026-08-12' },
    });

    expect(payload.esgPledgeCaseDate).toBe('2026-08-12');
  });

  it('omits an invalid date rather than sending "Invalid Date"', () => {
    const payload = mapApplicableConditionsPayload({
      appConditions: { esgPledgeCaseDate: new Date('nonsense') },
    });

    expect(payload).not.toHaveProperty('esgPledgeCaseDate');
  });

  it('reads from the appConditions key, which differs from the slug', () => {
    const payload = mapApplicableConditionsPayload({
      appConditions: { businessName: '株式会社テスト' },
    });

    expect(payload.businessName).toBe('株式会社テスト');
  });
});
