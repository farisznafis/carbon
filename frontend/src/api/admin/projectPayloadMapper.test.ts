import { describe, expect, it } from 'vitest';
import {
  mapAdminProjectPayload,
  mapAdminProjectUpdatePayload,
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

  it('maps project totals and operator ID without removed display fields', () => {
    const payload = mapAdminProjectPayload(
      {
        projectBase: {
          membersTotal: 0,
          operatorCompanyId: 'operator-1',
          postProjectEmission: 0,
        },
      },
      'admin-1',
    );

    expect(payload.project[0]).toMatchObject({
      membersTotal: 0,
      operatorCompanyId: 'operator-1',
      postProjectEmission: 0,
    });
    expect(payload.operatorCompanyId).toBe('operator-1');
    expect(payload.project[0]).not.toHaveProperty('companyName');
    expect(payload.project[0]).not.toHaveProperty('emissionReduction');
  });
});
