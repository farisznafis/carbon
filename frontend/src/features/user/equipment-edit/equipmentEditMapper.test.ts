import { describe, expect, it } from 'vitest';
import type { SiteProjectEquipment } from '@/api/site';
import {
  hasEquipmentEditChanges,
  mapEquipmentEditToChangeRequest,
  mapEquipmentToEditForm,
  mergeEquipmentFilesForChange,
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
        changeDate: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
      }),
    );
  });

  it('creates the complete change payload and preserves zero and false', () => {
    const original = mapEquipmentToEditForm(equipment, 'SITE-1');
    const current = {
      ...original,
      changeDate: '2026-08-20',
      powerCondStandby: '0',
      hasBattery: 'no' as const,
      subsidyAmount: '0',
    };

    expect(mapEquipmentEditToChangeRequest(current)).toEqual({
      changeDate: '2026-08-20',
      nominalMaxOutput: 25.5,
      pcsManufacturer: 'PCS Maker',
      pcsModel: 'PCS-1',
      pcsUnitCount: 2,
      deviceNo: 'DEVICE-1',
      pcsStandby: 0,
      hasBattery: false,
      batteryMaker: null,
      batteryModel: null,
      batteryCapacity: null,
      totalInvestment: 100000,
      hasSubsidy: true,
      subsidyName: 'GX補助金',
      subsidyAmount: 0,
      preProjectUnitPrice: 20,
      files: [],
    });
  });

  it('omits hidden conditional values when their feature is disabled', () => {
    const original = mapEquipmentToEditForm(equipment, 'SITE-1');
    const current = {
      ...original,
      changeDate: '2026-08-20',
      hasBattery: 'no' as const,
      batteryMaker: 'Changed but hidden',
      hasSubsidy: 'no' as const,
      subsidyName: 'Changed but hidden',
    };

    expect(mapEquipmentEditToChangeRequest(current)).toEqual(
      expect.objectContaining({
        hasBattery: false,
        batteryMaker: null,
        batteryModel: null,
        batteryCapacity: null,
        hasSubsidy: false,
        subsidyName: null,
        subsidyAmount: null,
      }),
    );
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
    const files = mergeEquipmentFilesForChange(
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
    expect(mapEquipmentEditToChangeRequest(original, files)).toEqual(
      expect.objectContaining({ files }),
    );
  });

  it('keeps existing file metadata even when no replacement is selected', () => {
    expect(
      mergeEquipmentFilesForChange(
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
