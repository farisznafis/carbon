import { describe, expect, it } from 'vitest';
import type { SolarEquipment } from '@/features/user/project-registration/components/types';
import {
  mapConfirmationItemsValidationRequest,
  mapFinalAgreementRequest,
  mapProjectJoinRequest,
  mapSustainabilityCommitmentRequest,
} from './projectJoinMapper';

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
  it('maps every registration step to the nested project join request', () => {
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
      {
        projectId: 'PROJ0002',
        processedBy: 'user-1',
        companyId: 'PC000000001',
      },
    );

    expect(result).toMatchObject({
      equipment: {
        tempId: 'equipment-1',
        siteId: 'SITE-1',
        pcsUnitCount: 1,
        pcsStandby: 1,
        files: equipment.files,
      },
      confirmationItems: {
        id: 'PROJ0002',
        processedBy: 'user-1',
        companyId: 'PC000000001',
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
      finalAgreement: {
        isAgreeTerms: true,
      },
    });
  });
});

describe('mapConfirmationItemsValidationRequest', () => {
  it('adds request context and sends explicit defaults for hidden fields', () => {
    const result = mapConfirmationItemsValidationRequest(
      {
        useRegisteredInfo: true,
        provideAdditionalInfo: true,
        transferEnvironmentalValue: true,
        notRegisteredElsewhere: true,
        keidanrenParticipation: 'notParticipating',
        specifiedEmitterStatus: 'notSpecifiedEmitter',
        energyRationalizationCompliance: 'notApplicable',
      },
      {
        projectId: 'PROJ0002',
        processedBy: 'user-1',
        companyId: 'PC000000001',
      },
    );

    expect(result).toEqual({
      id: 'PROJ0002',
      processedBy: 'user-1',
      companyId: 'PC000000001',
      isAgreeDataUse: true,
      isAgreeExtraData: true,
      isAgreeTransfer: true,
      isNotRegistered: true,
      isKeidanrenParticipation: false,
      isEmitter: false,
      emitterCode: '',
      isAdjustedEmissionsAdded: false,
      isSubjectToPeriodicReport: false,
      isPeriodicReportTypeOne: false,
      energyBusinessCode: '',
      factoryCode: '',
    });
  });

  it('preserves unanswered checkboxes and radio fields as null', () => {
    const result = mapConfirmationItemsValidationRequest(
      {
        useRegisteredInfo: null,
        provideAdditionalInfo: null,
        transferEnvironmentalValue: null,
        notRegisteredElsewhere: null,
        keidanrenParticipation: '',
        specifiedEmitterStatus: '',
        energyRationalizationCompliance: '',
      },
      {
        projectId: 'PROJ0002',
        processedBy: 'user-1',
        companyId: 'PC000000001',
      },
    );

    expect(result).toMatchObject({
      isAgreeDataUse: null,
      isAgreeExtraData: null,
      isAgreeTransfer: null,
      isNotRegistered: null,
      isKeidanrenParticipation: null,
      isEmitter: null,
      isAdjustedEmissionsAdded: null,
      isSubjectToPeriodicReport: null,
      isPeriodicReportTypeOne: null,
    });
  });
});

describe('mapSustainabilityCommitmentRequest', () => {
  it('maps and trims the visible agreement fields', () => {
    expect(
      mapSustainabilityCommitmentRequest({
        businessName: ' Company ',
        representativeName: ' Representative ',
        representativePosition: ' Director ',
        contractAgreed: true,
      }),
    ).toEqual({
      companyName: 'Company',
      repName: 'Representative',
      repRole: 'Director',
      hasConsent: true,
    });
  });

  it('sends empty values and false when the form is unanswered', () => {
    expect(mapSustainabilityCommitmentRequest({})).toEqual({
      companyName: '',
      repName: '',
      repRole: '',
      hasConsent: false,
    });
  });
});

describe('mapFinalAgreementRequest', () => {
  it('maps the final agreement without inventing consent', () => {
    expect(mapFinalAgreementRequest({ finalTermsAgreed: true })).toEqual({
      isAgreeTerms: true,
    });
    expect(mapFinalAgreementRequest({})).toEqual({
      isAgreeTerms: false,
    });
  });
});
