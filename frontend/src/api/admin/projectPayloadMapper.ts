import type {
  AdminProjectPlanApplicationPayloadFormValues,
  AdminProjectPayloadFormValues,
  CreateAdminProjectPlanApplicationPayload,
  CreateAdminProjectPayload,
} from './types';

function toOptionalString(value: string | number | undefined): string | undefined {
  return value === undefined || value === null || value === '' ? undefined : String(value);
}

function toStringValue(value: unknown): string | undefined {
  if (value === undefined || value === null) return undefined;
  const stringValue = String(value).trim();
  return stringValue === '' ? undefined : stringValue;
}

function firstStringValue(...values: unknown[]): string | undefined {
  for (const value of values) {
    const stringValue = toStringValue(value);
    if (stringValue !== undefined) return stringValue;
  }

  return undefined;
}

function joinName(lastName: unknown, firstName: unknown): string | undefined {
  const parts = [toStringValue(lastName), toStringValue(firstName)].filter(Boolean);
  return parts.length > 0 ? parts.join(' ') : undefined;
}

function normalizeApplicationCategory(value: unknown): unknown {
  if (value === 'プロジェクト登録') return '登録';
  if (value === 'プロジェクト計画更新' || value === '計画更新') return '計画変更';
  if (value === '認証権限保有主体更新') return '認証権限保有主体変更';

  return value;
}

function toBooleanValue(value: unknown): unknown {
  if (typeof value === 'boolean') return value;
  if (typeof value !== 'string') return value;

  if (['true', '有', 'はい', '対象である', '特定排出者である'].includes(value)) {
    return true;
  }

  if (['false', '無', 'いいえ', '対象でない', '特定排出者でない'].includes(value)) {
    return false;
  }

  if (
    value ===
    'カーボンニュートラル行動計画に参加する削減活動の実施者による削減活動を含む'
  ) {
    return true;
  }

  if (
    value ===
    'カーボンニュートラル行動計画に参加する削減活動の実施者による削減活動を含まない'
  ) {
    return false;
  }

  return value;
}

function normalizeProjectApplicationBooleans(
  projectApplication: Record<string, unknown>,
) {
  const booleanFields = [
    'isUsingCostSupport',
    'isUsingExamCostSupport',
    'isDesignatedEmitter',
    'hasEnergyActPeriodReport',
    'isChangeRepresentativePosition',
    'isChangeRepresentativeName',
    'isChangePicPosition',
    'isChangePicName',
    'isChangePicEmail',
    'isChangePicPhone',
    'isChangeOtherContactBusinessName',
    'isChangeOtherContactPicPosition',
    'isChangeOtherContactPicName',
    'isChangeOtherContactPicEmail',
    'isChangeOtherContactPicPhone',
    'isParticipatingCnAct',
  ];

  return booleanFields.reduce<Record<string, unknown>>(
    (normalized, fieldName) => {
      if (fieldName in normalized) {
        normalized[fieldName] = toBooleanValue(normalized[fieldName]);
      }
      return normalized;
    },
    { ...projectApplication },
  );
}

function cleanPayload(payload: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(payload)
      .map(([key, value]) => {
        if (typeof value === 'string') {
          return [key, value.trim()] as const;
        }
        return [key, value] as const;
      })
      .filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );
}

const NOT_APPLICABLE_ENERGY_ACT_NUMBER = '0';

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
        newMembersTotal: projectBase.newMembersTotal,
        withdrawnMembers: projectBase.withdrawnMembers,
        cumulativeMembers: projectBase.cumulativeMembers,
        membersTotal: projectBase.membersTotal,
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

export function mapAdminProjectPlanApplicationPayload(
  values: AdminProjectPlanApplicationPayloadFormValues,
): CreateAdminProjectPlanApplicationPayload {
  const projectBase = values.projectBase ?? {};
  const projectApplication = normalizeProjectApplicationBooleans(
    values.projectApplication ?? {},
  );
  const shouldSendEnergyActNumbers =
    projectApplication.hasEnergyActPeriodReport === true;
  const energyActOperationNumber = shouldSendEnergyActNumbers
    ? firstStringValue(
        projectApplication.energyActOperationNumber,
        projectApplication.energyActOpeNumber,
      ) ?? NOT_APPLICABLE_ENERGY_ACT_NUMBER
    : NOT_APPLICABLE_ENERGY_ACT_NUMBER;
  const energyManageFactoryNumber = shouldSendEnergyActNumbers
    ? firstStringValue(
        projectApplication.energyManageFactoryNumber,
        projectApplication.energyManagFactoryNumber,
      ) ?? NOT_APPLICABLE_ENERGY_ACT_NUMBER
    : NOT_APPLICABLE_ENERGY_ACT_NUMBER;

  return cleanPayload({
    ...projectApplication,
    applicationCategory: normalizeApplicationCategory(projectApplication.applicationCategory),
    isUsingExamCostSupport: projectApplication.isUsingExamCostSupport ?? projectApplication.isUsingCostSupport,
    projectOperatorKana: projectApplication.projectOperatorKana,
    projectOperatorNameJa: projectApplication.projectOperatorNameJa ?? projectApplication.projectOperatorJa,
    projectOperatorNameEn: projectApplication.projectOperatorNameEn ?? projectApplication.projectOperatorEn,
    corpNumber: projectApplication.corpNumber,
    corpPosition: projectApplication.corpPosition,
    corpName: projectApplication.corpName,
    isDesignatedEmitter: projectApplication.isDesignatedEmitter,
    emitterCode: projectApplication.emitterCode,
    hasEnergyActPeriodReport: projectApplication.hasEnergyActPeriodReport,
    energyActOperationNumber,
    energyActOpeNumber: undefined,
    energyManageFactoryNumber,
    energyManagFactoryNumber: undefined,
    representativePosition: firstStringValue(
      projectApplication.representativePosition,
      projectBase.repPosition,
    ),
    representativeName: firstStringValue(
      projectApplication.representativeName,
      joinName(projectBase.repLastName, projectBase.repFirstName),
    ),
    picPosition: firstStringValue(projectApplication.picPosition, projectBase.picPosition),
    picName: firstStringValue(
      projectApplication.picName,
      joinName(projectBase.picLastName, projectBase.picFirstName),
    ),
    picEmail: firstStringValue(projectApplication.picEmail, projectBase.picEmail),
    picPhoneNumber: firstStringValue(projectApplication.picPhoneNumber, projectApplication.picPhone, projectBase.picPhoneNumber),
    projectNumber: firstStringValue(projectApplication.projectNumber, projectBase.projectNumber),
    projectName: firstStringValue(projectApplication.projectName, projectBase.projectName),
    methodologyCategory: firstStringValue(projectApplication.methodologyCategory, projectBase.methodCategory),
    methodologyNumber: firstStringValue(projectApplication.methodologyNumber, projectBase.methodNumber),
    methodology: firstStringValue(projectApplication.methodology, projectBase.methodName),
    locationEn: firstStringValue(
      projectApplication.locationEn,
      projectApplication.SaaS,
      projectApplication.location,
    ),
    regionalCategory: firstStringValue(
      projectApplication.regionalCategory,
      projectApplication.regionCategory,
      projectBase.regionalCategory,
    ),
    groupName: projectApplication.groupName,
    groupNameFurigana: projectApplication.groupNameFurigana,
    isParticipatingCnAct: projectApplication.isParticipatingCnAct,
    validationCheckReviewerName: projectApplication.validationCheckReviewerName,
    reviewerName: projectApplication.reviewerName,
    projectOutline: projectApplication.projectOutline,
  });
}
