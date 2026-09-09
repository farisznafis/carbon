import { describe, expect, it } from 'vitest';
import type { SiteProjectEquipmentValidationFile } from '@/api/site';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';
import {
  mapSolarFormFilesForValidation,
  mapSolarFormToEquipmentCreateRequest,
  mapSolarFormToEquipmentValidationItem,
} from './equipmentValidationMapper';

const baseFormData: SolarPowerFormData = {
  siteId: 'SITE4f83ac050b20d755',
  equipmentOpDate: '2026-07-17',
  nominalMaxOutput: '10.5',
  pcsManufacturer: 'PCS Maker',
  pcsModel: 'PCS-01',
  powerCondUnitCount: '2',
  deviceNo: 'DEVICE-01',
  powerCondStandby: '3.5',
  hasBattery: 'no',
  batteryMaker: '',
  batteryModel: '',
  batteryCapacity: '',
  totalInvestment: '1000000',
  hasSubsidy: 'no',
  subsidyName: '',
  subsidyAmount: '',
  preProjectUnitPrice: '42.5',
};

const uploadedFile: SiteProjectEquipmentValidationFile = {
  fileType: 'solar_contract',
  s3Key: 'equipment/temp/contract.pdf',
  fileName: 'contract.pdf',
  fileId: 'file-1',
};

describe('mapSolarFormToEquipmentValidationItem', () => {
  it('maps canonical API field names and numeric values', () => {
    expect(
      mapSolarFormToEquipmentValidationItem(
        baseFormData,
        'temp-1',
        'SITE4f83ac050b20d755',
        [uploadedFile],
      ),
    ).toEqual({
      tempId: 'temp-1',
      siteId: 'SITE4f83ac050b20d755',
      equipmentOpDate: '2026-07-17',
      nominalMaxOutput: 10.5,
      pcsManufacturer: 'PCS Maker',
      pcsModel: 'PCS-01',
      pcsUnitCount: 2,
      deviceNo: 'DEVICE-01',
      pcsStandby: 3.5,
      hasBattery: false,
      totalInvestment: 1000000,
      hasSubsidy: false,
      preProjectUnitPrice: 42.5,
      files: [uploadedFile],
    });
  });

  it('includes conditional battery and subsidy fields', () => {
    const result = mapSolarFormToEquipmentValidationItem(
      {
        ...baseFormData,
        hasBattery: 'yes',
        batteryMaker: 'Battery Maker',
        batteryModel: 'BAT-01',
        batteryCapacity: '8.2',
        hasSubsidy: 'yes',
        subsidyName: '補助金A',
        subsidyAmount: '250000',
      },
      'temp-2',
      'SITE4f83ac050b20d755',
      [],
    );

    expect(result).toMatchObject({
      hasBattery: true,
      batteryMaker: 'Battery Maker',
      batteryModel: 'BAT-01',
      batteryCapacity: 8.2,
      hasSubsidy: true,
      subsidyName: '補助金A',
      subsidyAmount: 250000,
    });
  });

  it('preserves empty required values for backend validation', () => {
    const result = mapSolarFormToEquipmentValidationItem(
      {
        ...baseFormData,
        equipmentOpDate: '',
        nominalMaxOutput: '',
        pcsManufacturer: '',
        pcsModel: '',
        powerCondUnitCount: '',
        deviceNo: '',
        powerCondStandby: '',
        hasBattery: '',
        totalInvestment: '',
        hasSubsidy: '',
        preProjectUnitPrice: '',
      },
      'temp-3',
      '',
      [],
    );

    expect(result).toMatchObject({
      equipmentOpDate: null,
      nominalMaxOutput: null,
      pcsManufacturer: null,
      pcsModel: null,
      pcsUnitCount: null,
      deviceNo: null,
      pcsStandby: null,
      hasBattery: null,
      siteId: null,
      totalInvestment: null,
      hasSubsidy: null,
      preProjectUnitPrice: null,
    });
  });

  it('maps empty conditional text values to null', () => {
    const result = mapSolarFormToEquipmentValidationItem(
      {
        ...baseFormData,
        hasBattery: 'yes',
        batteryMaker: '',
        batteryModel: '',
        batteryCapacity: '',
        hasSubsidy: 'yes',
        subsidyName: '',
        subsidyAmount: '',
      },
      'temp-4',
      'SITE4f83ac050b20d755',
      [],
    );

    expect(result).toMatchObject({
      batteryMaker: null,
      batteryModel: null,
      batteryCapacity: null,
      subsidyName: null,
      subsidyAmount: null,
    });
  });
});

describe('mapSolarFormToEquipmentCreateRequest', () => {
  it('omits the retired PCS unit count from the create request', () => {
    const result = mapSolarFormToEquipmentCreateRequest(
      baseFormData,
      'temp-create',
      'SITE4f83ac050b20d755',
      [uploadedFile],
    );

    expect(result).toEqual(
      expect.objectContaining({
        tempId: 'temp-create',
        siteId: 'SITE4f83ac050b20d755',
        equipmentOpDate: '2026-07-17',
        batteryMaker: null,
        batteryModel: null,
        batteryCapacity: null,
        subsidyName: null,
        subsidyAmount: null,
        files: [uploadedFile],
      }),
    );
    expect(result).not.toHaveProperty('pcsUnitCount');
  });
});

describe('mapSolarFormFilesForValidation', () => {
  it('maps every selected form file to validation metadata', () => {
    const result = mapSolarFormFilesForValidation({
      ...baseFormData,
      operationStartDateFile: new File(['contract'], 'contract.pdf'),
      nominalMaxOutputFile: new File(['panel'], 'panel.pdf'),
      pcsFile: new File(['pcs'], 'pcs.pdf'),
    });

    expect(result).toHaveLength(3);
    expect(result).toEqual([
      expect.objectContaining({
        fileType: 'solar_contract',
        fileName: 'contract.pdf',
      }),
      expect.objectContaining({
        fileType: 'panel_catalog',
        fileName: 'panel.pdf',
      }),
      expect.objectContaining({
        fileType: 'pcs_spec',
        fileName: 'pcs.pdf',
      }),
    ]);
    result.forEach((file) => {
      expect(file.s3Key).not.toBe('');
      expect(file.fileId).not.toBe('');
    });
  });
});
