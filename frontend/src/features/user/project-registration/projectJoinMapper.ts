import type {
  SiteProjectConfirmationItemsValidationRequest,
  SiteProjectFinalAgreementValidationRequest,
  SiteProjectJoinConfirmationItems,
  SiteProjectJoinSustainabilityCommitmentRequest,
  SiteProjectSolarRegistrationEquipment,
  SiteProjectSolarRegistrationRequest,
} from '@/api/site';
import type { ContractStepFormValues } from '@/features/user/project-registration/components/ContractStep';
import type { SolarEquipment } from '@/features/user/project-registration/components/types';
import type { FinalTermsValues } from '@/features/user/project-registration/components/FinalTermsStep';
import type { AgreementFormValues } from '@/features/user/project-registration/components/agreementsConfig';
import { mapSolarFormToEquipmentValidationItem } from '@/features/user/project-registration/equipmentValidationMapper';

type ProjectJoinRequestContext = {
  projectId: string;
  processedBy: string;
  companyId: string;
};

type SolarRegistrationContext = ProjectJoinRequestContext & {
  siteId: string;
  methodologyNumber: string;
};

const mapRadioSelection = (
  value: string | undefined,
  affirmativeValue: string,
  negativeValue: string,
): boolean | null => {
  if (value === affirmativeValue) return true;
  if (value === negativeValue) return false;
  return null;
};

export function mapAgreementValuesToConfirmationItems(
  agreementValues: Partial<AgreementFormValues>,
): SiteProjectJoinConfirmationItems {
  const isEmitter =
    agreementValues.specifiedEmitterStatus === 'isSpecifiedEmitter';
  const isSubjectToPeriodicReport =
    agreementValues.energyRationalizationCompliance === 'applicable';
  const mapRequiredAgreement = (
    value: boolean | null | undefined,
  ): true | null => {
    return value === true ? true : null;
  };

  return {
    isAgreeDataUse: mapRequiredAgreement(agreementValues.useRegisteredInfo),
    isAgreeExtraData: mapRequiredAgreement(agreementValues.provideAdditionalInfo),
    isAgreeTransfer: mapRequiredAgreement(agreementValues.transferEnvironmentalValue),
    isNotRegistered: mapRequiredAgreement(agreementValues.notRegisteredElsewhere),
    isKeidanrenParticipation:
      agreementValues.keidanrenParticipation === 'participating',
    isEmitter,
    ...(isEmitter
      ? {
          emitterCode: agreementValues.specifiedEmitterCode?.trim(),
          isAdjustedEmissionsAdded:
            agreementValues.emissionAdditionAgreement === true,
        }
      : {}),
    isSubjectToPeriodicReport,
    ...(isSubjectToPeriodicReport
      ? {
          isPeriodicReportTypeOne: agreementValues.energyLawType === 'type1',
          energyBusinessCode:
            agreementValues.energyLawBusinessNumber?.trim(),
          factoryCode: agreementValues.energyLawFactoryNumber?.trim(),
        }
      : {}),
  };
}

export function mapConfirmationItemsValidationRequest(
  agreementValues: Partial<AgreementFormValues>,
  context: ProjectJoinRequestContext,
): SiteProjectConfirmationItemsValidationRequest {
  const confirmationItems =
    mapAgreementValuesToConfirmationItems(agreementValues);
  const isKeidanrenParticipation = mapRadioSelection(
    agreementValues.keidanrenParticipation,
    'participating',
    'notParticipating',
  );
  const isEmitter = mapRadioSelection(
    agreementValues.specifiedEmitterStatus,
    'isSpecifiedEmitter',
    'notSpecifiedEmitter',
  );
  const isSubjectToPeriodicReport = mapRadioSelection(
    agreementValues.energyRationalizationCompliance,
    'applicable',
    'notApplicable',
  );
  const isPeriodicReportTypeOne =
    isSubjectToPeriodicReport === true
      ? mapRadioSelection(agreementValues.energyLawType, 'type1', 'type2')
      : isSubjectToPeriodicReport === null
        ? null
        : false;

  return {
    id: context.projectId,
    processedBy: context.processedBy,
    companyId: context.companyId,
    ...confirmationItems,
    isAgreeDataUse: agreementValues.useRegisteredInfo ?? null,
    isAgreeExtraData: agreementValues.provideAdditionalInfo ?? null,
    isAgreeTransfer: agreementValues.transferEnvironmentalValue ?? null,
    isNotRegistered: agreementValues.notRegisteredElsewhere ?? null,
    isKeidanrenParticipation,
    isEmitter,
    emitterCode: confirmationItems.emitterCode ?? '',
    isAdjustedEmissionsAdded:
      isEmitter === false
        ? false
        : agreementValues.emissionAdditionAgreement ?? null,
    isSubjectToPeriodicReport,
    isPeriodicReportTypeOne,
    energyBusinessCode: confirmationItems.energyBusinessCode ?? '',
    factoryCode: confirmationItems.factoryCode ?? '',
  };
}

// Step 3 Map
export function mapSustainabilityCommitmentRequest(
  values: Partial<ContractStepFormValues>,
): SiteProjectJoinSustainabilityCommitmentRequest {
  return {
    companyName: values.businessName?.trim() ?? '',
    repName: values.representativeName?.trim() ?? '',
    repRole: values.representativePosition?.trim() ?? '',
    hasConsent: values.contractAgreed === true,
  };
}

export function mapFinalAgreementRequest(
  values: Partial<FinalTermsValues>,
): SiteProjectFinalAgreementValidationRequest {
  return {
    isAgreeTerms: values.finalTermsAgreed === true,
  };
}

function mapSolarRegistrationEquipment(
  equipment: SolarEquipment,
  methodologyNumber: string,
): SiteProjectSolarRegistrationEquipment {
  const validatedEquipment = mapSolarFormToEquipmentValidationItem(
    equipment.solarData,
    equipment.id,
    equipment.solarData.siteId,
    equipment.files ?? [],
  );
  const additionalEquipment = mapRadioSelection(
    equipment.solarData.additionalEquipment,
    'yes',
    'no',
  );

  return {
    tempId: validatedEquipment.tempId,
    methodologyNumber,
    isEligible: true,
    equipmentOpDate: validatedEquipment.equipmentOpDate,
    nominalMaxOutput: validatedEquipment.nominalMaxOutput,
    pcsManufacturer: validatedEquipment.pcsManufacturer,
    pcsModel: validatedEquipment.pcsModel,
    powerCondUnitCount: validatedEquipment.pcsUnitCount,
    deviceNo: validatedEquipment.deviceNo,
    powerCondStandby: validatedEquipment.pcsStandby,
    hasBattery: validatedEquipment.hasBattery,
    ...(validatedEquipment.hasBattery === true
      ? {
          batteryMaker: validatedEquipment.batteryMaker,
          batteryModel: validatedEquipment.batteryModel,
          batteryCapacity: validatedEquipment.batteryCapacity,
        }
      : {}),
    ictEquipmentPowerConsumption:
      additionalEquipment === true
        ? toNullableNumber(equipment.solarData.ictEquipmentPowerConsumption)
        : null,
    additionalEquipment,
    ...(additionalEquipment === true
      ? {
          additionalEquipmentDate:
            equipment.solarData.additionalEquipmentDate.trim(),
          additionalEquipmentType:
            equipment.solarData.additionalEquipmentType.trim(),
          additionalUnitCount: toNullableNumber(
            equipment.solarData.additionalUnitCount,
          ),
          additionalDeviceUniqueNo:
            equipment.solarData.additionalDeviceUniqueNo.trim(),
        }
      : {}),
    totalInvestment: validatedEquipment.totalInvestment,
    hasSubsidy: validatedEquipment.hasSubsidy,
    ...(validatedEquipment.hasSubsidy === true
      ? {
          subsidyName: validatedEquipment.subsidyName,
          subsidyAmount: validatedEquipment.subsidyAmount,
        }
      : {}),
    preProjectUnitPrice: validatedEquipment.preProjectUnitPrice,
    files: equipment.files ?? [],
  };
}

function toNullableNumber(value: string): number | null {
  if (!value.trim()) return null;

  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : null;
}

export function mapProjectSolarRegistrationRequest(
  equipments: SolarEquipment[],
  agreementValues: Partial<AgreementFormValues>,
  sustainabilityValues: Partial<ContractStepFormValues>,
  finalTermsValues: Partial<FinalTermsValues>,
  context: SolarRegistrationContext,
): SiteProjectSolarRegistrationRequest {
  const {
    id: _id,
    processedBy: _processedBy,
    companyId: _companyId,
    ...confirmationItems
  } = mapConfirmationItemsValidationRequest(agreementValues, context);

  return {
    siteId: context.siteId,
    projectId: context.projectId,
    equipments: equipments.map((equipment) =>
      mapSolarRegistrationEquipment(equipment, context.methodologyNumber),
    ),
    ...confirmationItems,
    energyReport:
      confirmationItems.isSubjectToPeriodicReport === true
        ? agreementValues.energyLawType?.trim() ?? ''
        : '',
    ...mapSustainabilityCommitmentRequest(sustainabilityValues),
    ...mapFinalAgreementRequest(finalTermsValues),
  };
}
