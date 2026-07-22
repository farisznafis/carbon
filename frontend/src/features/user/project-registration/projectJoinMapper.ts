import type { SiteProjectJoinRequest } from '@/api/site';
import type { ContractStepFormValues } from '@/features/user/project-registration/components/ContractStep';
import type { SolarEquipment } from '@/features/user/project-registration/components/types';
import type { FinalTermsValues } from '@/features/user/project-registration/components/FinalTermsStep';
import type { AgreementFormValues } from '@/features/user/project-registration/components/agreementsConfig';
import { mapSolarFormToEquipmentValidationItem } from '@/features/user/project-registration/equipmentValidationMapper';

export function mapProjectJoinRequest(
  equipment: SolarEquipment,
  agreementValues: Partial<AgreementFormValues>,
  sustainabilityValues: Partial<ContractStepFormValues>,
  finalTermsValues: Partial<FinalTermsValues>,
): SiteProjectJoinRequest {
  const isEmitter =
    agreementValues.specifiedEmitterStatus === 'isSpecifiedEmitter';
  const isSubjectToPeriodicReport =
    agreementValues.energyRationalizationCompliance === 'applicable';

  return {
    equipment: mapSolarFormToEquipmentValidationItem(
      equipment.solarData,
      equipment.id,
      equipment.solarData.siteId,
      equipment.files ?? [],
    ),
    confirmationItems: {
      isAgreeDataUse: agreementValues.useRegisteredInfo === true,
      isAgreeExtraData: agreementValues.provideAdditionalInfo === true,
      isAgreeTransfer: agreementValues.transferEnvironmentalValue === true,
      isNotRegistered: agreementValues.notRegisteredElsewhere === true,
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
    },
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
