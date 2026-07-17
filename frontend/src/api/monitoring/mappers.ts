import type { MonitoringRecentApiItem, MonitoringRecentItem } from "./types";

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
    projectId: toNumberOrZero(item.projectId),
    deviceId: toNumberOrNull(item.deviceId),
    parameter: item.parameter ?? "",
    value: toNumberOrNull(item.value),
    unit: item.unit ?? "",
    timestamp: item.timestamp ?? dateRecorded,

    dateRecorded,
    changedBattery: Boolean(item.changedBattery),
    projectType: item.projectType ?? "",

    siteId: item.siteId == null ? null : String(item.siteId),
    userId: toNumberOrNull(item.userId),

    ELpv_value: toNumberOrZero(item.ELpv_value),
    ELpvr_value: toNumberOrZero(item.ELpvr_value),
    ELPJ_battery_value:
      toNumberOrNull(item.ELPJ_battery_value) ??
      toNumberOrNull(legacyBatteryValue),
    ELPJ_control_value: toNumberOrZero(item.ELPJ_control_value),
    ELPJ_accuracy: toNumberOrZero(item.ELPJ_accuracy),

    ELpv_evidence: item.ELpv_evidence ?? null,
    ELpvr_evidence: item.ELpvr_evidence ?? null,
    ELPJ_battery_evidence: item.ELPJ_battery_evidence ?? null,
    ELPJ_control_equipment_spec_files:
      item.ELPJ_control_equipment_spec_files ?? null,
  };
}
