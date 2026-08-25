import type { SiteProjectSustainabilityCommitmentDefaultResponse } from '@/api/site';
import type { ContractStepFormValues } from '@/features/user/project-registration/components/ContractStep';

/** Maps backend Contract Step defaults while intentionally leaving consent unchecked. */
export function mapSustainabilityCommitmentDefault(
  response: SiteProjectSustainabilityCommitmentDefaultResponse,
): ContractStepFormValues {
  return {
    businessName: response.companyName?.trim() ?? '',
    representativeName: response.repName?.trim() ?? '',
    representativePosition: response.repRole?.trim() ?? '',
    contractAgreed: false,
  };
}

/** Detects legacy drafts that contain user-entered Contract Step values but no source flag. */
export function hasLegacyContractDraftValues(
  values: Partial<ContractStepFormValues>,
): boolean {
  return Boolean(
    values.businessName?.trim() ||
      values.representativeName?.trim() ||
      values.representativePosition?.trim() ||
      values.contractAgreed,
  );
}
