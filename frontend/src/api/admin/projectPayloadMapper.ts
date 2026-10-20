import type {
  AdminProjectPayloadFormValues,
  CreateAdminProjectPayload,
  UpdateAdminProjectPayload,
} from './types';

function toOptionalString(value: string | number | undefined): string | undefined {
  return value === undefined || value === null || value === '' ? undefined : String(value);
}

function toStringValue(value: unknown): string | undefined {
  if (value === undefined || value === null) return undefined;
  const stringValue = String(value).trim();
  return stringValue === '' ? undefined : stringValue;
}

const JST_OFFSET_MS = 9 * 60 * 60 * 1000;

/**
 * プロジェクト開始日 as the Instant the backend stores.
 *
 * The operator picks a calendar date; 会則 prints the stored instant in JST.
 * So the date is read as JST midnight whatever the browser's timezone —
 * otherwise the terms could show the day before the one that was picked.
 */
export function toStartedAtInstant(value: unknown): string | undefined {
  const date = toStringValue(value);
  if (!date || !/^\d{4}-\d{2}-\d{2}$/.test(date)) return undefined;

  const instant = new Date(`${date}T00:00:00+09:00`);
  return Number.isNaN(instant.getTime()) ? undefined : instant.toISOString();
}

/** The reverse of `toStartedAtInstant`: the JST date, for the date input. */
export function toStartedAtDate(value: unknown): string {
  const text = toStringValue(value);
  if (!text) return '';

  const instant = new Date(text);
  return Number.isNaN(instant.getTime())
    ? ''
    : new Date(instant.getTime() + JST_OFFSET_MS).toISOString().slice(0, 10);
}

export function mapAdminProjectPayload(
  values: AdminProjectPayloadFormValues,
  createdBy: string,
): CreateAdminProjectPayload {
  const projectBase = values.projectBase ?? {};

  return {
    project: [
      {
        projectName: projectBase.projectName,
        projectNumber: projectBase.projectNumber,
        projectStatus: projectBase.projectStatus,
        startedAt: toStartedAtInstant(projectBase.startedAt),
        usePurpose: projectBase.usePurpose,
        baselineEmissions: projectBase.baselineEmissions,
        postProjectEmission: projectBase.postProjectEmission,
        repFirstName: projectBase.repFirstName,
        repFirstNameKana: projectBase.repFirstNameKana,
        repLastName: projectBase.repLastName,
        repLastNameKana: projectBase.repLastNameKana,
        repEmail: projectBase.repEmail,
        repPhoneNumber: projectBase.repPhoneNumber,
        repPosition: projectBase.repPosition,
        picFirstName: projectBase.picFirstName,
        picFirstNameKana: projectBase.picFirstNameKana,
        picLastName: projectBase.picLastName,
        picLastNameKana: projectBase.picLastNameKana,
        picEmail: projectBase.picEmail,
        picPhoneNumber: projectBase.picPhoneNumber,
        picPosition: projectBase.picPosition,
        dataManagementPic: projectBase.picDataManagement,
        regionalCategory: projectBase.regionalCategory,
        methodologyVersion: projectBase.methodologyVersion,
        methodCategory: projectBase.methodCategory,
        methodNumber: projectBase.methodNumber,
        methodName: projectBase.methodName,
        methodMainEmission: projectBase.methodMainEmission,
        methodCombiFeasibility: projectBase.methodCombiFeasibility,
        methodDetails: projectBase.methodDetails,
        operatorCompanyId: toOptionalString(projectBase.operatorCompanyId),
      },
    ],
    operatorCompanyId: toOptionalString(projectBase.operatorCompanyId),
    createdBy,
  };
}

const toNumberValue = (value: unknown): number => {
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : 0;
};

export function mapAdminProjectUpdatePayload(
  values: AdminProjectPayloadFormValues,
  processedBy: string,
): UpdateAdminProjectPayload {
  const projectBase = values.projectBase ?? {};

  return {
    processedBy,
    projectName: toStringValue(projectBase.projectName) ?? '',
    baselineEmissions: toNumberValue(projectBase.baselineEmissions),
    postProjectEmission: toNumberValue(projectBase.postProjectEmission),
    repFirstName: toStringValue(projectBase.repFirstName) ?? '',
    repFirstNameKana: toStringValue(projectBase.repFirstNameKana) ?? '',
    repLastName: toStringValue(projectBase.repLastName) ?? '',
    repLastNameKana: toStringValue(projectBase.repLastNameKana) ?? '',
    repEmail: toStringValue(projectBase.repEmail) ?? '',
    repPhoneNumber: toStringValue(projectBase.repPhoneNumber) ?? '',
    repPosition: toStringValue(projectBase.repPosition) ?? '',
    picFirstName: toStringValue(projectBase.picFirstName) ?? '',
    picFirstNameKana: toStringValue(projectBase.picFirstNameKana) ?? '',
    picLastName: toStringValue(projectBase.picLastName) ?? '',
    picLastNameKana: toStringValue(projectBase.picLastNameKana) ?? '',
    picEmail: toStringValue(projectBase.picEmail) ?? '',
    picPhoneNumber: toStringValue(projectBase.picPhoneNumber) ?? '',
    picPosition: toStringValue(projectBase.picPosition) ?? '',
    dataManagementPic: toStringValue(projectBase.picDataManagement) ?? '',
    regionalCategory: toStringValue(projectBase.regionalCategory) ?? '',
    methodologyVersion: toStringValue(projectBase.methodologyVersion) ?? '',
    methodCategory: toStringValue(projectBase.methodCategory) ?? '',
    methodNumber: toStringValue(projectBase.methodNumber) ?? '',
    methodName: toStringValue(projectBase.methodName) ?? '',
    methodMainEmission:
      toStringValue(projectBase.methodMainEmission) ?? '',
    methodCombiFeasibility: Boolean(projectBase.methodCombiFeasibility),
    methodDetails: toStringValue(projectBase.methodDetails) ?? '',
    startedAt: toStartedAtInstant(projectBase.startedAt),
    usePurpose: toStringValue(projectBase.usePurpose),
  };
}

/** Backend @DateFormat expects yyyy-MM-dd; the form holds a Date for date pickers. */