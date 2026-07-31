import type {
  ParticipationReviewChecklistField,
  ParticipationReviewDetail,
} from '@/api/admin';

export const participationReviewChecklistLabels: Record<
  ParticipationReviewChecklistField,
  string
> = {
  isSolarContractDoc: '太陽光発電設備の契約書を確認',
  isPcsSpecDoc: 'PCS仕様書を確認',
  isBatterySpecDoc: '蓄電池仕様書を確認',
  isOperationStartDoc: '運転開始日確認資料を確認',
  isGenerationDataDoc: '発電量データを確認',
  isReversePowerDoc: '逆潮流確認資料を確認',
  isInvestmentProofDoc: '設備投資額確認資料を確認',
  isApplicationCompleteCheck: '申請内容の記載を確認',
  isFormsAttachCheck: '必要書類の添付を確認',
  isEmissionsReductionActivityListCheck: '排出削減活動リストを確認',
  isOriginalSpecCheck: '原本・仕様を確認',
  isSubsidyCheck: '補助金に関する内容を確認',
};

export function getParticipationReviewStage(
  review: ParticipationReviewDetail,
): '一次確認待ち' | '二次確認待ち' | '確認済み' {
  if (!review.review.primaryApplReviewerName.trim()) return '一次確認待ち';
  if (!review.review.secondaryApplReviewerName.trim()) return '二次確認待ち';
  return '確認済み';
}

export function displayParticipationValue(
  value: string | number | null | undefined,
): string {
  if (value === null || value === undefined || value === '') return '―';
  return String(value);
}

export function displayParticipationBoolean(value: boolean | null): string {
  if (value === true) return 'はい';
  if (value === false) return 'いいえ';
  return '―';
}

export const FILE_TYPE_LABEL: Record<string, string> = {
  solar_contract: '太陽光発電電力受給契約確認書',
  panel_catalog: '仕様書・カタログ',
  pcs_spec: 'PCS仕様書',
  battery_catalog: '仕様書・カタログ',

  construction_invoice: '工事請負契約書・請求書等',
  subsidy_notice: '補助金交付決定通知書',
  electricity_bill: '電力会社請求明細書等',
};

export function formatFileType(fileType: string | null | undefined): string {
  if (!fileType) return '―';
  return FILE_TYPE_LABEL[fileType] ?? fileType;
}