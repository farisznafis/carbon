import { describe, expect, it } from 'vitest';
import type { SiteProjectEquipment } from '@/api/site';
import {
  hasEquipmentEditChanges,
  mapEquipmentEditToUpdateRequest,
  mapEquipmentToEditForm,
  mergeEquipmentFilesForUpdate,
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
    expect(
      hasEquipmentEditChanges(
        {
          ...original,
          pcsFile: new File(['updated'], 'updated-pcs.pdf', {
            type: 'application/pdf',
          }),
        },
        original,
      ),
    ).toBe(true);
  });

  it('merges uploaded file replacements with existing file metadata', () => {
    const files = mergeEquipmentFilesForUpdate(
      [
        {
          fileType: 'solar_contract',
          s3Key: 'equipment/existing-contract.pdf',
          fileId: 'existing-contract-id',
        },
        {
          fileType: 'pcs_spec',
          s3Key: 'equipment/existing-pcs.pdf',
          fileId: 'existing-pcs-id',
        },
      ],
      [
        {
          fileType: 'solar_contract',
          s3Key: 'equipment/new-contract.pdf',
          fileName: 'new-contract.pdf',
          fileId: 'new-contract-id',
        },
      ],
    );

    expect(files).toEqual([
      {
        fileType: 'solar_contract',
        s3Key: 'equipment/new-contract.pdf',
        fileName: 'new-contract.pdf',
        fileId: 'new-contract-id',
      },
      {
        fileType: 'pcs_spec',
        s3Key: 'equipment/existing-pcs.pdf',
        fileName: 'existing-pcs.pdf',
        fileId: 'existing-pcs-id',
      },
    ]);

    const original = mapEquipmentToEditForm(equipment, 'SITE-1');
    expect(mapEquipmentEditToUpdateRequest(original, original, files)).toEqual({
      files,
    });
  });

  it('keeps existing file metadata even when no replacement is selected', () => {
    expect(
      mergeEquipmentFilesForUpdate(
        [
          {
            fileType: 'pcs_spec',
            s3Key: 'equipment/existing-pcs.pdf',
            fileId: 'existing-pcs-id',
          },
        ],
        [],
      ),
    ).toEqual([
      {
        fileType: 'pcs_spec',
        s3Key: 'equipment/existing-pcs.pdf',
        fileName: 'existing-pcs.pdf',
        fileId: 'existing-pcs-id',
      },
    ]);
  });
});
