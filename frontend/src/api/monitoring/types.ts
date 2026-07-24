import type { ApiPagination } from '../_shared/types';

export type MonitoringData = {
  projectId: string;
  siteId?: string | null;
  projectType: string;
  dateRecorded: string | null;
  ELpv_value: number | null;
  ELpvr_value: number | null;
  changedBattery: boolean;
  ELPJ_battery_value: number | null;
  ELPJ_control_value: number | null;
  ELPJ_accuracy: number | null;
  ELpv_evidence?: string | null;
  ELpvr_evidence?: string | null;
  ELPJ_battery_evidence?: string | null;
  ELPJ_control_equipment_spec_files?: string | null;
};

export interface SubmitDataResponse {
  success: boolean;
  message: string;
  dataId: string;
}

export type MonitoringEvidenceKind =
  | "elpv"
  | "elpvr"
  | "elpjBattery"
  | "elpjControl";

export type MonitoringRecentApiItem = {
  projectId?: string | null;
  siteId?: string | number | null;
  userId?: string | number | null;
  projectType?: string | null;

  dateRecorded?: string | null;
  date_recorder?: string | null;
  timeUpdated?: string | null;

  changedBattery?: boolean | null;

  ELpv_value?: number | string | null;
  ELpvr_value?: number | string | null;
  ELPJ_battery_value?: number | string | null;
  ELPJ_control_value?: number | string | null;
  ELPJ_accuracy?: number | string | null;

  ELpv_evidence?: string | null;
  ELpvr_evidence?: string | null;
  ELPJ_battery_evidence?: string | null;
  ELPJ_control_equipment_spec_files?: string | null;

  deviceId?: string | number | null;
  parameter?: string | null;
  value?: number | string | null;
  unit?: string | null;
  timestamp?: string | null;
  ELPJBatteryValue?: number | string | null;
};

export type MonitoringRecentItem = MonitoringData & {
  deviceId?: number | null;
  parameter?: string;
  value?: number | null;
  unit?: string;
  timestamp?: string;
  dateRecorded: string;
  changedBattery: boolean;
  projectType: string;
  siteId?: string | null;
  userId?: number | null;
  ELpv_value?: number | null;
  ELpvr_value?: number | null;
  ELPJ_battery_value?: number | null;
  ELPJ_control_value?: number | null;
  ELPJ_accuracy?: number | null;
  ELpv_evidence?: string | null;
  ELpvr_evidence?: string | null;
  ELPJ_battery_evidence?: string | null;
  ELPJ_control_equipment_spec_files?: string | null;
};

export type MonitoringRecentResponse = {
  statusCode?: string;
  message?: string | null;
  data?: unknown;
};

export type MonitoringRecordListItem = {
  id: string;
  projectID: string;
  siteID: string | number;
  participatingUserID: number;
  timeUpdated: string;
  dateRecorded: string;
  elpjControlValue: number | null;
  elpjControlEquipmentSpecFiles: string | null;
  elpjBatteryValue: number | null;
  elpjAccuracy: number | null;
  elpjBatteryEvidence: string | null;
  elPv: number | null;
  elPvEvidence: string | null;
  elPvr: number | null;
  elPvrEvidence: string | null;
  elPvEvidenceUrl: string | null;
  elPvrEvidenceUrl: string | null;
  elpjBatterySpecificationUrl: string | null;
  elpjControlEvidenceUrl: string | null;
  changedBattery: boolean;
  isActive: boolean;
  processedAt: string | null;
  methodologyType: string | null;
};

export type MonitoringRecordListResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: Record<string, string> | null;
  timestamp?: string;
  items?: MonitoringRecordListItem[] | null;
  pagination?: Partial<ApiPagination> | null;
};

export type MonitoringRecordListResult = {
  items: MonitoringRecordListItem[];
  pagination: ApiPagination;
};

export type MonitoringCancelRequestPayload = {
  siteId: string;
  recordPK: string;
  recordSK: string;
  reasonText: string;
};

export type MonitoringCancelRequestResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: Record<string, string> | null;
  timestamp?: string;
  cancelRequestId?: number;
  recordPk?: string;
  recordSk?: string;
  status?: string;
  requestedBy?: string;
  requestedAt?: string;
  siteId?: string;
  reason?: string;
};

export type MonitoringCancelRequestListItem = {
  id: number;
  requestedAt: string;
  reason: string;
  recordPk: string;
  status: string;
  requestedBy: string;
  siteName: string;
  decidedAt: string | null;
  decidedBy: string | null;
};

export type MonitoringCancelRequestListResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: Record<string, string> | null;
  timestamp?: string;
  items?: MonitoringCancelRequestListItem[] | null;
};

export type MonitoringCancelDecision = 'APPROVE' | 'REJECTED';

export type MonitoringCancelDecisionPayload = {
  decision: MonitoringCancelDecision;
  managerRemarks?: string;
};

export type MonitoringCancelDecisionResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: Record<string, string> | null;
  timestamp?: string;
  cancelRequestId?: number;
  status?: string;
  decidedBy?: string;
  decidedAt?: string;
  originalRecordPk?: string;
  originalRecordSk?: string;
  offsetRecordPk?: string;
  offsetRecordSk?: string;
  managerRemarks?: string;
};
