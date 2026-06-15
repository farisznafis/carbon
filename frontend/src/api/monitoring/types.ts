export type MonitoringData = {
  projectId: number;
  siteId?: number | null;
  userId?: number | null;
  projectType: string;
  dateRecorded: string;
  ELpv_value: number;
  ELpvr_value: number;
  changedBattery: boolean;
  ELPJ_battery_value: number | null;
  ELPJ_control_value: number;
  ELPJ_accuracy: number;
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
  projectId?: string | number | null;
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
  siteId?: number | null;
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
