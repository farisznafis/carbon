import { apiRequest } from '@/api/_shared/httpClient';

/**
 * 個別活動実績報告 (EN-R-002): one row per equipment in a project, and the
 * full detail of one row. Any field may be null when it was never entered;
 * the admin then reads it off the evidence files.
 */

/** UNDETERMINED per the 別紙10-1 mapping; PENDING is what the current spec still lists. */
export type EligibilityStatus = 'ELIGIBLE' | 'NOT_ELIGIBLE' | 'UNDETERMINED' | 'PENDING';
export type ReportReadiness = 'READY' | 'MISSING_EVIDENCE' | 'NO_BATCH_RESULT';

export type IndividualActivityRow = {
  equipmentId: number;
  companyId: string | null;
  companyName: string | null;
  siteId: string | null;
  siteAddress: string | null;
  hasBattery: boolean | null;
  eligibilityStatus: EligibilityStatus | null;
  reportReadiness: ReportReadiness | null;
  generationKwh: number | null;
  saleKwh: number | null;
};

export type EvidenceFile = { fileType: string | null; url: string | null };

type Nullable<T> = { [K in keyof T]: T[K] | null };

export type MonitoringPoint = Nullable<{
  readingDate: string;
  utilityMeterReadingDate: string;
  generationKwh: number;
  saleKwh: number;
}>;

export type IndividualActivityDetail = {
  equipmentId: number;
  member: Nullable<{
    companyId: string;
    companyName: string;
    siteId: string;
    address: string;
    membershipApplicationDate: string;
    eligibilityStatus: EligibilityStatus;
    businessOperatorFlags: Nullable<{
      carbonNeutralActionPlan: boolean;
      energySavingActSpecifiedOperator: boolean;
      warmingActSpecifiedEmitter: boolean;
      emitterCode: string;
    }>;
  }> | null;
  equipment: Nullable<{
    pcs: Nullable<{
      manufacturer: string;
      model: string;
      unitCount: number;
      serialNumber: string;
      standbyPowerKw: number;
      meterClass: string;
      maxErrorRatePercent: number;
    }>;
    nominalOutputKw: number;
    battery: Nullable<{
      manufacturer: string;
      model: string;
      capacityKwh: number;
      chargeEfficiencyPercent: number;
      dischargeEfficiencyPercent: number;
    }>;
    ictConsumptionKw: number;
    operationStartDate: string;
    effectiveFrom: string;
    effectiveTo: string;
    evidenceFiles: EvidenceFile[];
  }> | null;
  additionality: Nullable<{
    totalInvestment: number;
    subsidyAmount: number;
    subsidyName: string;
    subsidyGrantingBody: string;
    preProjectUnitPrice: number;
  }> | null;
  registrationRequirements: Nullable<{
    operatingInJapan: boolean;
    withinTwoYearsOfApplication: boolean;
    hasAdditionality: boolean;
    noDuplicateRegistration: boolean;
  }> | null;
  monitoring: Nullable<{
    samplingStatus: string;
    meteringPosition: string;
    generationMethod: string;
    saleMethod: string;
    windowStart: MonitoringPoint;
    windowEnd: MonitoringPoint;
    evidenceFiles: EvidenceFile[];
  }> | null;
};

/** GET /admin/monitoring-data/projects/{projectId}/individual-activity/rows — the rows only. */
export async function getIndividualActivityRows(projectId: string): Promise<IndividualActivityRow[]> {
  const body = await apiRequest<IndividualActivityRow[] | { data?: IndividualActivityRow[] }>(
    `/admin/monitoring-data/projects/${encodeURIComponent(projectId)}/individual-activity/rows`,
    { method: 'GET', forbiddenHandling: 'caller', fallbackErrorMessage: '個別活動データを取得できませんでした。' },
  );
  // The spec returns a bare array; tolerate the usual envelope too.
  return Array.isArray(body) ? body : body?.data ?? [];
}

/** GET /admin/monitoring-data/individual-activity/rows/{equipmentId}?projectPublicId= — one row in full. */
export async function getIndividualActivityDetail(
  projectId: string,
  equipmentId: number,
): Promise<IndividualActivityDetail> {
  const body = await apiRequest<IndividualActivityDetail & { data?: IndividualActivityDetail }>(
    `/admin/monitoring-data/individual-activity/rows/${equipmentId}?projectPublicId=${encodeURIComponent(projectId)}`,
    { method: 'GET', forbiddenHandling: 'caller', fallbackErrorMessage: '設備の詳細を取得できませんでした。' },
  );
  return body?.data ?? body;
}
