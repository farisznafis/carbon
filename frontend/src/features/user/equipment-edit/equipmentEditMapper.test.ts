import { describe, expect, it } from 'vitest';
import type { SiteProjectEquipment } from '@/api/site';
import {
  hasEquipmentEditChanges,
  mapEquipmentEditToUpdateRequest,
  mapEquipmentToEditForm,
} from './equipmentEditMapper';

const equipment: SiteProjectEquipment = {
  equipmentId: 10,
  equipmentOpDate: '2026-08-01',
  nominalMaxOutput: 25.5,
  pcsManufacturer: 'PCS Maker',
  pcsModel: 'PCS-1',
  powerCondUnitCount: 2,
  deviceNo: 'DEVICE-1',
  powerCondStandby: 1,
  hasBattery: true,
  batteryMaker: 'Battery Maker',
  batteryModel: 'BAT-1',
  batteryCapacity: 12,
  totalInvestment: 100000,
  hasSubsidy: true,
  subsidyName: 'GX補助金',
  subsidyAmount: 5000,
  preProjectUnitPrice: 20,
};

describe('equipmentEditMapper', () => {
  it('maps normalized equipment values into the shared solar form', () => {
    expect(mapEquipmentToEditForm(equipment, 'SITE-1')).toEqual(
      expect.objectContaining({
        siteId: 'SITE-1',
        nominalMaxOutput: '25.5',
        powerCondUnitCount: '2',
        powerCondStandby: '1',
        hasBattery: 'yes',
        hasSubsidy: 'yes',
      }),
    );
  });

  it('creates a partial canonical payload and preserves zero and false', () => {
    const original = mapEquipmentToEditForm(equipment, 'SITE-1');
    const current = {
      ...original,
      powerCondStandby: '0',
      hasBattery: 'no' as const,
      subsidyAmount: '0',
    };

    expect(mapEquipmentEditToUpdateRequest(current, original)).toEqual({
      pcsStandby: 0,
      hasBattery: false,
      subsidyAmount: 0,
    });
  });

  it('omits hidden conditional values when their feature is disabled', () => {
    const original = mapEquipmentToEditForm(equipment, 'SITE-1');
    const current = {
      ...original,
      hasBattery: 'no' as const,
      batteryMaker: 'Changed but hidden',
      hasSubsidy: 'no' as const,
      subsidyName: 'Changed but hidden',
    };

    expect(mapEquipmentEditToUpdateRequest(current, original)).toEqual({
      hasBattery: false,
      hasSubsidy: false,
    });
  });

  it('detects no-op and changed form values', () => {
    const original = mapEquipmentToEditForm(equipment, 'SITE-1');

    expect(hasEquipmentEditChanges(original, original)).toBe(false);
    expect(
      hasEquipmentEditChanges(
        { ...original, pcsManufacturer: 'New Maker' },
        original,
      ),
    ).toBe(true);
  });
});
