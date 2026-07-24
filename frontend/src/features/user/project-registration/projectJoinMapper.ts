import type {
  SiteProjectConfirmationItemsValidationRequest,
  SiteProjectJoinConfirmationItems,
  SiteProjectJoinRequest,
} from '@/api/site';
import type { ContractStepFormValues } from '@/features/user/project-registration/components/ContractStep';
import type { SolarEquipment } from '@/features/user/project-registration/components/types';
import type { FinalTermsValues } from '@/features/user/project-registration/components/FinalTermsStep';
import type { AgreementFormValues } from '@/features/user/project-registration/components/agreementsConfig';
import { mapSolarFormToEquipmentValidationItem } from '@/features/user/project-registration/equipmentValidationMapper';

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
  context: {
    projectId: string;
    processedBy: string;
    companyId: string;
  },
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

export function mapProjectJoinRequest(
  equipment: SolarEquipment,
  agreementValues: Partial<AgreementFormValues>,
  sustainabilityValues: Partial<ContractStepFormValues>,
  finalTermsValues: Partial<FinalTermsValues>,
): SiteProjectJoinRequest {
  return {
    equipment: mapSolarFormToEquipmentValidationItem(
      equipment.solarData,
      equipment.id,
      equipment.solarData.siteId,
      equipment.files ?? [],
    ),
    confirmationItems:
      mapAgreementValuesToConfirmationItems(agreementValues),
    sustainabilityCommitment: {
      companyName: sustainabilityValues.businessName?.trim() ?? '',
      repName: sustainabilityValues.representativeName?.trim() ?? '',
      repRole: sustainabilityValues.representativePosition?.trim() ?? '',
      hasConsent: sustainabilityValues.contractAgreed === true,
    },
    finalAgreement: {
      isAgreeTerms: finalTermsValues.finalTermsAgreed === true,
    },
  };
}
