import { describe, expect, it } from 'vitest';
import type { SolarEquipment } from '@/features/user/project-registration/components/types';
import { mapProjectJoinRequest } from './projectJoinMapper';

const equipment: SolarEquipment = {
  id: 'equipment-1',
  formType: 'solar',
  solarData: {
    siteId: 'SITE-1',
    equipmentOpDate: '2026-07-22',
    nominalMaxOutput: '10',
    pcsManufacturer: 'Maker',
    pcsModel: 'Model',
    powerCondUnitCount: '1',
    deviceNo: 'DEVICE-1',
    powerCondStandby: '1',
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
    totalInvestment: '1000',
    hasSubsidy: 'no',
    subsidyName: '',
    subsidyAmount: '',
    preProjectUnitPrice: '20',
  },
  files: [
    {
      fileType: 'solar_contract',
      s3Key: 'equipment/contract.pdf',
      fileName: 'contract.pdf',
      fileId: 'file-1',
    },
  ],
};

describe('mapProjectJoinRequest', () => {
  it('maps all project registration steps to the join contract', () => {
    const result = mapProjectJoinRequest(
      equipment,
      {
        useRegisteredInfo: true,
        provideAdditionalInfo: true,
        transferEnvironmentalValue: true,
        notRegisteredElsewhere: true,
        keidanrenParticipation: 'participating',
        specifiedEmitterStatus: 'isSpecifiedEmitter',
        specifiedEmitterCode: '123456789',
        emissionAdditionAgreement: true,
        energyRationalizationCompliance: 'applicable',
        energyLawType: 'type1',
        energyLawBusinessNumber: '1234567',
        energyLawFactoryNumber: '7654321',
      },
      {
        businessName: 'Company',
        representativeName: 'Representative',
        representativePosition: 'Director',
        contractAgreed: true,
      },
      { finalTermsAgreed: true },
    );

    expect(result).toMatchObject({
      equipment: {
        siteId: 'SITE-1',
        files: equipment.files,
      },
      confirmationItems: {
        isAgreeDataUse: true,
        isKeidanrenParticipation: true,
        isEmitter: true,
        emitterCode: '123456789',
        isAdjustedEmissionsAdded: true,
        isSubjectToPeriodicReport: true,
        isPeriodicReportTypeOne: true,
        energyBusinessCode: '1234567',
        factoryCode: '7654321',
      },
      sustainabilityCommitment: {
        companyName: 'Company',
        repName: 'Representative',
        repRole: 'Director',
        hasConsent: true,
      },
      finalAgreement: { isAgreeTerms: true },
    });
  });
});
