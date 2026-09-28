import { describe, expect, it } from 'vitest';
import {
  mapAdminProjectPayload,
  mapAdminProjectPlanApplicationPayload,
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

describe('mapAdminProjectPlanApplicationPayload', () => {
  it('sends the 他の連絡先 fields under the names the DTO declares', () => {
    // These were spelled otherContactPicName / ...PicPhone on the form and
    // otherContactsPicName / ...PicPhoneNumber on the DTO. Jackson ignores
    // unknown properties and all of them are optional, so the page saved
    // cleanly and the operator's answers were dropped in silence.
    const payload = mapAdminProjectPlanApplicationPayload({
      projectApplication: {
        otherContactsPicName: '鈴木 一郎',
        isChangeOtherContactsPicName: true,
        otherContactsPicEmail: 'ichiro.suzuki@example.com',
        isChangeOtherContactsPicEmail: true,
        otherContactsPicPhoneNumber: '0312345678',
        isChangeOtherContactsPicPhoneNumber: true,
        isChangePicPhoneNumber: true,
      },
    });

    expect(payload.otherContactsPicName).toBe('鈴木 一郎');
    expect(payload.otherContactsPicEmail).toBe('ichiro.suzuki@example.com');
    expect(payload.otherContactsPicPhoneNumber).toBe('0312345678');
    expect(payload.isChangeOtherContactsPicName).toBe(true);
    expect(payload.isChangeOtherContactsPicEmail).toBe(true);
    expect(payload.isChangeOtherContactsPicPhoneNumber).toBe(true);
    expect(payload.isChangePicPhoneNumber).toBe(true);

    // The old spellings must not reappear alongside them.
    expect(payload).not.toHaveProperty('otherContactPicName');
    expect(payload).not.toHaveProperty('otherContactPicEmail');
    expect(payload).not.toHaveProperty('otherContactPicPhone');
  });

  it('sends what the operator entered over a DTO-named autofill copy', () => {
    const payload = mapAdminProjectPlanApplicationPayload({
      projectApplication: {
        picPhone: '0312345678',
        picPhoneNumber: '03-5432-1234',
        regionCategory: '関東',
        regionalCategory: '全国',
      },
    });

    expect(payload.picPhoneNumber).toBe('0312345678');
    expect(payload.regionalCategory).toBe('関東');
  });
});
