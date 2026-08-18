import type {
  MonitoringRecentApiItem,
  MonitoringRecentItem,
  MonitoringRecordListApiItem,
  MonitoringRecordListItem,
} from "./types";

function toNumberOrNull(value: unknown): number | null {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const parsed = typeof value === "number" ? value : Number(value);
  return Number.isNaN(parsed) ? null : parsed;
}

function toNumberOrZero(value: unknown): number {
  const parsed = toNumberOrNull(value);
  return parsed ?? 0;
}

function toStringOrEmpty(value: unknown): string {
  return value == null ? '' : String(value);
}

function toStringOrNull(value: unknown): string | null {
  if (value == null || value === '') return null;
  return String(value);
}

function toBoolean(value: unknown): boolean {
  if (typeof value === 'boolean') return value;
  if (typeof value === 'number') return value !== 0;
  if (typeof value === 'string') {
    return value.toLowerCase() === 'true' || value === '1';
  }
  return false;
}

export function normalizeRecentMonitoringItem(
  item: MonitoringRecentApiItem,
): MonitoringRecentItem {
  const dateRecorded =
    item.dateRecorded ??
    item.date_recorder ??
    item.timeUpdated ??
    "";

  const legacyBatteryValue = item.ELPJBatteryValue;

  return {
    projectId: item.projectId == null ? '' : String(item.projectId),
    deviceId: toNumberOrNull(item.deviceId),
    parameter: item.parameter ?? "",
    value: toNumberOrNull(item.value),
    unit: item.unit ?? "",
    timestamp: item.timestamp ?? dateRecorded,

    dateRecorded,
    changedBattery: Boolean(item.changedBattery),
    projectType: item.projectType ?? "",

    siteId: item.siteId == null ? null : String(item.siteId),
    equipmentId: toNumberOrNull(item.equipmentId),
    userId: toNumberOrNull(item.userId),

    ELpv_value: toNumberOrZero(item.ELpv_value),
    ELpvr_value: toNumberOrZero(item.ELpvr_value),
    ELPJ_battery_value:
      toNumberOrNull(item.ELPJ_battery_value) ??
      toNumberOrNull(legacyBatteryValue),
    ELPJ_control_value: toNumberOrZero(item.ELPJ_control_value),
    ELPJ_accuracy: toNumberOrZero(item.ELPJ_accuracy),

    elpv_evidence: item.elpv_evidence ?? null,
    elpvr_evidence: item.elpvr_evidence ?? null,
    elpj_battery_evidence: item.elpj_battery_evidence ?? null,
    elpj_control_evidence
      item.elpj_control_evidence ?? null,
  };
}

export function normalizeMonitoringRecordListItem(
  item: MonitoringRecordListApiItem,
): MonitoringRecordListItem {
  return {
    id: toStringOrEmpty(item.id ?? item.monitoringId ?? item.recordId),
    projectId: toStringOrEmpty(item.projectId ?? item.projectID),
    siteId: toStringOrEmpty(item.siteId ?? item.siteID),
    equipmentId: toNumberOrNull(item.equipmentId ?? item.equipmentID),
    participatingUserId:
      toNumberOrNull(
        item.participatingUserId ?? item.participatingUserID,
      ) ?? 0,
    timeUpdated: toStringOrEmpty(item.timeUpdated ?? item.updatedAt),
    dateRecorded: toStringOrEmpty(item.dateRecorded),
    elpjControlValue: toNumberOrNull(item.elpjControlValue),
    elpjControlEquipmentSpecFiles: toStringOrNull(
      item.elpjControlEquipmentSpecFiles,
    ),
    elpjBatteryValue: toNumberOrNull(item.elpjBatteryValue),
    elpjAccuracy: toNumberOrNull(item.elpjAccuracy),
    elpjBatteryEvidence: toStringOrNull(item.elpjBatteryEvidence),
    elPv: toNumberOrNull(item.elPv),
    elPvEvidence: toStringOrNull(item.elPvEvidence),
    elPvr: toNumberOrNull(item.elPvr),
    elPvrEvidence: toStringOrNull(item.elPvrEvidence),
    elPvEvidenceUrl: toStringOrNull(item.elPvEvidenceUrl),
    elPvrEvidenceUrl: toStringOrNull(item.elPvrEvidenceUrl),
    elpjBatterySpecificationUrl: toStringOrNull(
      item.elpjBatterySpecificationUrl,
    ),
    elpjControlEvidenceUrl: toStringOrNull(item.elpjControlEvidenceUrl),
    changedBattery: toBoolean(item.changedBattery),
    isActive: toBoolean(item.isActive),
    processedAt: toStringOrNull(item.processedAt),
    methodologyType: toStringOrNull(item.methodologyType),
  };
}
