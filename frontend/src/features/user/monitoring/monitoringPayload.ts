import type { MonitoringData } from '@/api/monitoring';

export type MonitoringPayloadContext = {
  projectType: string;
  projectId: string;
  siteId: unknown;
  hasBattery: boolean;
  evidenceUrls?: Partial<Record<keyof MonitoringData, string | null>>;
};

function fileValueToString(value: unknown): string {
  if (value instanceof File) return value.name;
  return typeof value === 'string' ? value : '';
}

function toNumberOrNull(value: unknown): number | null {
  if (value === '' || value == null) return null;

  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : null;
}

export function buildMonitoringPayload(
  formData: Record<string, unknown>,
  context: MonitoringPayloadContext,
): MonitoringData {
  const evidenceUrls = context.evidenceUrls ?? {};
  const changedBattery =
    formData.changedBattery == null
      ? null
      : Boolean(formData.changedBattery);

  return {
    projectType: context.projectType,
    projectId: context.projectId,
    siteId: context.siteId ? String(context.siteId) : null,
    equipmentId: toNumberOrNull(formData.equipmentId),
    dateRecorded: formData.dateRecorded ? String(formData.dateRecorded) : null,
    ELpv_value: toNumberOrNull(formData.ELpv_value),
    ELpvr_value: toNumberOrNull(formData.ELpvr_value),
    ...(context.hasBattery
      ? {
          changedBattery,
          ...(changedBattery
            ? {
                ELPJ_battery_value: toNumberOrNull(
                  formData.ELPJ_battery_value,
                ),
                ELPJ_battery_evidence:
                  evidenceUrls.ELPJ_battery_evidence ??
                  fileValueToString(formData.ELPJ_battery_evidence),
              }
            : {}),
        }
      : {}),
    ELPJ_control_value: toNumberOrNull(formData.ELPJ_control_value),
    ELPJ_accuracy: toNumberOrNull(formData.ELPJ_accuracy),
    ELpv_evidence:
      evidenceUrls.ELpv_evidence ?? fileValueToString(formData.ELpv_evidence),
    ELpvr_evidence:
      evidenceUrls.ELpvr_evidence ?? fileValueToString(formData.ELpvr_evidence),
    ELPJ_control_equipment_spec_files:
      evidenceUrls.ELPJ_control_equipment_spec_files ??
      fileValueToString(formData.ELPJ_control_equipment_spec_files),
  };
}
