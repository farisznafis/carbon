import { describe, expect, it } from 'vitest';
import type { SiteProjectEquipmentValidationFile } from '@/api/site';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';
import { mapSolarFormToEquipmentValidationItem } from './equipmentValidationMapper';

const baseFormData: SolarPowerFormData = {
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
  additionalEquipment: 'no',
  ictEquipmentPowerConsumption: '',
  additionalEquipmentDate: '',
  additionalEquipmentType: '',
  additionalUnitCount: '',
  additionalDeviceUniqueNo: '',
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
        '1',
        [uploadedFile],
      ),
    ).toEqual({
      tempId: 'temp-1',
      siteId: 1,
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
        additionalEquipment: 'yes',
        ictEquipmentPowerConsumption: '15',
        additionalEquipmentDate: '2026-07-18',
        additionalEquipmentType: 'ICT controller',
        additionalUnitCount: '3',
        additionalDeviceUniqueNo: 'ICT-01',
        hasSubsidy: 'yes',
        subsidyName: '補助金A',
        subsidyAmount: '250000',
      },
      'temp-2',
      '1',
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
        nominalMaxOutput: '',
        powerCondUnitCount: '',
        powerCondStandby: '',
        hasBattery: '',
        additionalEquipment: '',
        totalInvestment: '',
        hasSubsidy: '',
        preProjectUnitPrice: '',
      },
      'temp-3',
      '',
      [],
    );

    expect(result).toMatchObject({
      nominalMaxOutput: null,
      pcsUnitCount: null,
      pcsStandby: null,
      hasBattery: null,
      siteId: null,
      totalInvestment: null,
      hasSubsidy: null,
      preProjectUnitPrice: null,
    });
  });
});
