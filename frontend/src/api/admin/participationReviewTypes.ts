export const participationReviewChecklistFields = [
  'isSolarContractDoc',
  'isPcsSpecDoc',
  'isBatterySpecDoc',
  'isOperationStartDoc',
  'isGenerationDataDoc',
  'isReversePowerDoc',
  'isInvestmentProofDoc',
  'isApplicationCompleteCheck',
  'isFormsAttachCheck',
  'isEmissionsReductionActivityListCheck',
  'isOriginalSpecCheck',
  'isSubsidyCheck',
] as const;

export const participationReviewStatuses = [
  'OPEN',
  'APPROVED',
  'REJECTED',
] as const;

export type ParticipationReviewStatus =
  (typeof participationReviewStatuses)[number];

export type ParticipationReviewChecklistField =
  (typeof participationReviewChecklistFields)[number];

export type ParticipationReviewChecklist = Record<
  ParticipationReviewChecklistField,
  boolean
>;

export type ParticipationReviewFile = {
  id: number | null;
  fileType: string;
  fileId: string;
  s3Key: string;
};

export type ParticipationReviewEquipment = {
  id: number | null;
  siteId: string;
  equipmentOpDate: string;
  nominalMaxOutput: number | null;
  pcsManufacturer: string;
  pcsModel: string;
  deviceNo: string;
  pcsStandby: number | null;
  hasBattery: boolean | null;
  batteryMaker: string;
  batteryModel: string;
  batteryCapacity: number | null;
  totalInvestment: number | null;
  hasSubsidy: boolean | null;
  subsidyName: string;
  subsidyAmount: number | null;
  preProjectUnitPrice: number | null;
  evidenceDocuments: string[];
  files: ParticipationReviewFile[];
};

export type ParticipationConfirmationItems = {
  isAgreeDataUse: boolean | null;
  isAgreeExtraData: boolean | null;
  isAgreeTransfer: boolean | null;
  isNotRegistered: boolean | null;
  isKeidanrenParticipation: boolean | null;
  isEmitter: boolean | null;
  emitterCode: string;
  isAdjustedEmissionsAdded: boolean | null;
  isSubjectToPeriodicReport: boolean | null;
  isPeriodicReportTypeOne: boolean | null;
  energyBusinessCode: string;
  factoryCode: string;
};

export type ParticipationSustainabilityCommitment = {
  companyName: string;
  repName: string;
  repRole: string;
  hasConsent: boolean | null;
  isAgreeTerms: boolean | null;
};

export type ParticipationReviewState = ParticipationReviewChecklist & {
  primaryApplReviewerName: string;
  secondaryApplReviewerName: string;
};

export type ParticipationReviewDetail = {
  participationId: number;
  siteId: number;
  projectId: number;
  projectName: string;
  siteName: string;
  companyName: string;
  submittedAt: string;
  equipment: ParticipationReviewEquipment;
  confirmationItems: ParticipationConfirmationItems;
  sustainabilityCommitment: ParticipationSustainabilityCommitment;
  review: ParticipationReviewState;
  pdfStatus: string;
};

export type ApproveParticipationReviewPayload = ParticipationReviewChecklist & {
  reviewerName: string;
  reviewerInkan: string;
};

export type RejectParticipationReviewPayload =
  ApproveParticipationReviewPayload & {
    rejectReason: string;
  };

