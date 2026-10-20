import { describe, expect, it } from 'vitest';
import {
  mapAdminProjectPayload,
  mapAdminProjectUpdatePayload,
  toStartedAtDate,
  toStartedAtInstant,
} from './projectPayloadMapper';

describe('mapAdminProjectUpdatePayload', () => {
  it('maps registration values to the update contract without stale aliases', () => {
    expect(
      mapAdminProjectUpdatePayload(
        {
          projectBase: {
            projectName: 'Project A',
            baselineEmissions: 120,
            postProjectEmission: 80,
            picDataManagement: '管理 太郎',
            methodCombiFeasibility: true,
          },
        },
        'admin-1',
      ),
    ).toMatchObject({
      processedBy: 'admin-1',
      projectName: 'Project A',
      baselineEmissions: 120,
      postProjectEmission: 80,
      dataManagementPic: '管理 太郎',
      methodCombiFeasibility: true,
    });
  });

  it('maps emissions and operator ID without removed fields', () => {
    const payload = mapAdminProjectPayload(
      {
        projectBase: {
          operatorCompanyId: 'operator-1',
          postProjectEmission: 0,
        },
      },
      'admin-1',
    );

    expect(payload.project[0]).toMatchObject({
      operatorCompanyId: 'operator-1',
      postProjectEmission: 0,
    });
    // 会員数 is filled in by hand on the Excel forms and is not sent.
    for (const removed of ['newMembersTotal', 'withdrawnMembers', 'cumulativeMembers', 'membersTotal']) {
      expect(payload.project[0]).not.toHaveProperty(removed);
    }
    expect(payload.operatorCompanyId).toBe('operator-1');
    expect(payload.project[0]).not.toHaveProperty('companyName');
    expect(payload.project[0]).not.toHaveProperty('emissionReduction');
  });

  it('sends 開始日 as the JST-midnight instant and 利用目的 as typed', () => {
    // Both are required by the create DTO. They were missing from the form
    // entirely, so every create was rejected with 開始日時は必須です。
    const payload = mapAdminProjectPayload(
      {
        projectBase: {
          startedAt: '2026-09-03',
          usePurpose: '地域脱炭素施策',
        },
      },
      'admin-1',
    );

    expect(payload.project[0].startedAt).toBe('2026-09-02T15:00:00.000Z');
    expect(payload.project[0].usePurpose).toBe('地域脱炭素施策');
  });

  it('leaves both out of an update when step 1 does not hold them', () => {
    const payload = mapAdminProjectUpdatePayload(
      { projectBase: { projectName: 'Project A', startedAt: '' } },
      'admin-1',
    );

    // The backend keeps the stored value for a field that is not sent; an
    // empty string would instead fail to parse as an Instant.
    expect(payload.startedAt).toBeUndefined();
    expect(payload.usePurpose).toBeUndefined();
  });
});

describe('toStartedAtDate', () => {
  it('reads a stored instant back as the JST date that was picked', () => {
    expect(toStartedAtDate(toStartedAtInstant('2026-09-03'))).toBe('2026-09-03');
    // 01:05 UTC is already the 3rd in JST, and so is 16:00 UTC on the 2nd.
    expect(toStartedAtDate('2026-09-03T01:05:12.040Z')).toBe('2026-09-03');
    expect(toStartedAtDate('2026-09-02T16:00:00Z')).toBe('2026-09-03');
  });

  it('gives the date input an empty value when nothing usable is stored', () => {
    expect(toStartedAtDate(null)).toBe('');
    expect(toStartedAtDate('invalid-date')).toBe('');
  });
});
