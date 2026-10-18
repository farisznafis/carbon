import type { AdminProjectDetail } from '@/api/admin/projects';
import { toStartedAtDate } from '@/api/admin/projectPayloadMapper';

const textFallback = (value: unknown) => (value == null ? '' : String(value));

const numberFallback = (value: unknown) => {
  if (value == null || value === '') return 0;

  const numberValue = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
};

const firstPresent = (...values: unknown[]) =>
  values.find((value) => value != null && value !== '');

/**
 * The SQL project record, as step 1 of the wizard holds it.
 *
 * Shared because more than one screen shows step 1: the wizard reads it when
 * resuming a project, and the plan application page shows it read-only. A
 * second copy would drift, and step 1 is what every plan page is keyed by.
 */
export const mapDetailToProjectBase = (
  detail: AdminProjectDetail,
): Record<string, unknown> => ({
  projectName: textFallback(detail.projectName),
  projectNumber: textFallback(detail.projectNumber ?? detail.id),
  projectStatus: textFallback(detail.projectStatus),
  startedAt: toStartedAtDate(detail.startedAt),
  usePurpose: textFallback(detail.usePurpose),
  emissionReduction: numberFallback(detail.emissionReduction),
  baselineEmissions: numberFallback(
    firstPresent(detail.baselineEmissions, detail.baselineEmission),
  ),
  postProjectEmission: numberFallback(
    firstPresent(detail.postProjectEmission, detail.postProjectEmissions),
  ),
  repFirstName: textFallback(detail.repFirstName),
  repFirstNameKana: textFallback(detail.repFirstNameKana),
  repLastName: textFallback(detail.repLastName),
  repLastNameKana: textFallback(detail.repLastNameKana),
  repEmail: textFallback(detail.repEmail),
  repPhoneNumber: textFallback(detail.repPhoneNumber),
  repPosition: textFallback(detail.repPosition),
  picFirstName: textFallback(detail.picFirstName),
  picFirstNameKana: textFallback(detail.picFirstNameKana),
  picLastName: textFallback(detail.picLastName),
  picLastNameKana: textFallback(detail.picLastNameKana),
  picEmail: textFallback(detail.picEmail),
  picPhoneNumber: textFallback(detail.picPhoneNumber),
  picPosition: textFallback(detail.picPosition),
  picDataManagement: textFallback(detail.dataManagementPic),
  companyName: textFallback(detail.companyName),
  regionalCategory: textFallback(detail.regionalCategory),
  newMembersTotal: numberFallback(detail.newMembersTotal),
  withdrawnMembers: numberFallback(detail.withdrawnMembers),
  cumulativeMembers: numberFallback(detail.cumulativeMembers),
  membersTotal: numberFallback(detail.membersTotal),
  methodologyVersion: textFallback(detail.methodologyVersion),
  methodCategory: textFallback(detail.methodologyCategory),
  methodNumber: textFallback(detail.methodologyNumber),
  methodName: textFallback(detail.methodologyName),
  methodMainEmission: textFallback(detail.methodologyMainEmission),
  methodCombiFeasibility: Boolean(detail.methodCombiFeasibility),
  methodDetails: textFallback(detail.methodDetails),
  operatorCompanyId: textFallback(detail.operatorCompanyId),
});
