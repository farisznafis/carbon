import {
  participationReviewChecklistFields,
  type ParticipationConfirmationItems,
  type ParticipationReviewChecklist,
  type ParticipationReviewDetail,
  type ParticipationReviewEquipment,
  type ParticipationReviewFile,
  type ParticipationReviewState,
  type ParticipationSustainabilityCommitment,
} from './participationReviewTypes';

type UnknownRecord = Record<string, unknown>;

function asRecord(value: unknown): UnknownRecord {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
    ? (value as UnknownRecord)
    : {};
}

function asText(value: unknown): string {
  return typeof value === 'string' ? value : '';
}

function asNumber(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function asBoolean(value: unknown): boolean | null {
  return typeof value === 'boolean' ? value : null;
}

function mapChecklist(value: UnknownRecord): ParticipationReviewChecklist {
  return participationReviewChecklistFields.reduce<ParticipationReviewChecklist>(
    (checklist, fieldName) => {
      checklist[fieldName] = value[fieldName] === true;
      return checklist;
    },
    {} as ParticipationReviewChecklist,
  );
}

function mapFile(value: unknown): ParticipationReviewFile {
  const file = asRecord(value);
  return {
    id: asNumber(file.id),
    fileType: asText(file.fileType),
    fileId: asText(file.fileId),
    s3Key: asText(file.s3Key),
  };
}

function mapEquipment(value: unknown): ParticipationReviewEquipment {
  const equipment = asRecord(value);
  return {
    id: asNumber(equipment.id),
    siteId: asText(equipment.siteId),
    equipmentOpDate: asText(equipment.equipmentOpDate),
    nominalMaxOutput: asNumber(equipment.nominalMaxOutput),
    pcsManufacturer: asText(equipment.pcsManufacturer),
    pcsModel: asText(equipment.pcsModel),
    deviceNo: asText(equipment.deviceNo),
    pcsStandby: asNumber(equipment.pcsStandby),
    hasBattery: asBoolean(equipment.hasBattery),
    batteryMaker: asText(equipment.batteryMaker),
    batteryModel: asText(equipment.batteryModel),
    batteryCapacity: asNumber(equipment.batteryCapacity),
    totalInvestment: asNumber(equipment.totalInvestment),
    hasSubsidy: asBoolean(equipment.hasSubsidy),
    subsidyName: asText(equipment.subsidyName),
    subsidyAmount: asNumber(equipment.subsidyAmount),
    preProjectUnitPrice: asNumber(equipment.preProjectUnitPrice),
    evidenceDocuments: Array.isArray(equipment.evidenceDocuments)
      ? equipment.evidenceDocuments.map(asText).filter(Boolean)
      : [],
    files: Array.isArray(equipment.files)
      ? equipment.files.map(mapFile)
      : [],
  };
}

function mapConfirmationItems(value: unknown): ParticipationConfirmationItems {
  const confirmationItems = asRecord(value);
  return {
    isAgreeDataUse: asBoolean(confirmationItems.isAgreeDataUse),
    isAgreeExtraData: asBoolean(confirmationItems.isAgreeExtraData),
    isAgreeTransfer: asBoolean(confirmationItems.isAgreeTransfer),
    isNotRegistered: asBoolean(confirmationItems.isNotRegistered),
    isKeidanrenParticipation: asBoolean(
      confirmationItems.isKeidanrenParticipation,
    ),
    isEmitter: asBoolean(confirmationItems.isEmitter),
    emitterCode: asText(confirmationItems.emitterCode),
    isAdjustedEmissionsAdded: asBoolean(
      confirmationItems.isAdjustedEmissionsAdded,
    ),
    isSubjectToPeriodicReport: asBoolean(
      confirmationItems.isSubjectToPeriodicReport,
    ),
    isPeriodicReportTypeOne: asBoolean(
      confirmationItems.isPeriodicReportTypeOne,
    ),
    energyBusinessCode: asText(confirmationItems.energyBusinessCode),
    factoryCode: asText(confirmationItems.factoryCode),
  };
}

function mapSustainabilityCommitment(
  value: unknown,
): ParticipationSustainabilityCommitment {
  const commitment = asRecord(value);
  return {
    companyName: asText(commitment.companyName),
    repName: asText(commitment.repName),
    repRole: asText(commitment.repRole),
    hasConsent: asBoolean(commitment.hasConsent),
    isAgreeTerms: asBoolean(commitment.isAgreeTerms),
  };
}

function mapReview(value: unknown): ParticipationReviewState {
  const review = asRecord(value);
  return {
    ...mapChecklist(review),
    primaryApplReviewerName: asText(review.primaryApplReviewerName),
    secondaryApplReviewerName: asText(review.secondaryApplReviewerName),
  };
}

export function mapParticipationReviewDetail(
  value: unknown,
): ParticipationReviewDetail {
  const review = asRecord(value);
  return {
    participationId: asNumber(review.participationId) ?? 0,
    siteId: asNumber(review.siteId) ?? 0,
    projectId: asNumber(review.projectId) ?? 0,
    projectName: asText(review.projectName),
    siteName: asText(review.siteName),
    companyName: asText(review.companyName),
    submittedAt: asText(review.submittedAt),
    equipment: mapEquipment(review.equipment),
    confirmationItems: mapConfirmationItems(review.confirmationItems),
    sustainabilityCommitment: mapSustainabilityCommitment(
      review.sustainabilityCommitment,
    ),
    review: mapReview(review.review),
    pdfStatus: asText(review.pdfStatus),
  };
}

export function mapParticipationReviewList(
  value: unknown,
): ParticipationReviewDetail[] {
  if (!Array.isArray(value)) {
    throw new Error('入会届一覧のレスポンス形式が不正です。');
  }

  return value.map(mapParticipationReviewDetail);
}

