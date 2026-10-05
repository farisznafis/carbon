import type { MonitoringData } from '@/api/monitoring';

export type MonitoringPayloadContext = {
  projectType: string;
  projectId: string;
  siteId: unknown;
  hasBattery: boolean;
  evidenceUrls?: Partial<Record<keyof MonitoringData, string | null>>;
};

/** Preserves an existing evidence URL or the selected filename before upload. */
function fileValueToString(value: unknown): string {
  if (value instanceof File) return value.name;
  return typeof value === 'string' ? value : '';
}

/** Converts entered numbers, including zero, while retaining empty values as null. */
function toNumberOrNull(value: unknown): number | null {
  if (value === '' || value == null) return null;

  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : null;
}

/** Builds the backend DTO and includes battery data only for battery equipment. */
export function buildMonitoringPayload(
  formData: Record<string, unknown>,
  context: MonitoringPayloadContext,
): MonitoringData {
  const evidenceUrls = context.evidenceUrls ?? {};
  return {
    projectType: context.projectType,
    projectId: context.projectId,
    siteId: context.siteId ? String(context.siteId) : null,
    equipmentId: toNumberOrNull(formData.equipmentId),
    dateRecorded: formData.dateRecorded ? String(formData.dateRecorded) : null,
    ELpv_value: toNumberOrNull(formData.ELpv_value),
    ELpvr_value: toNumberOrNull(formData.ELpvr_value),
    ...(context.hasBattery === true
      ? {
          changedBattery: true,
          ELPJ_battery_value: toNumberOrNull(formData.ELPJ_battery_value),
          elpj_battery_evidence:
            evidenceUrls.elpj_battery_evidence ??
            fileValueToString(formData.elpj_battery_evidence),
        }
      : {}),
    ELPJ_control_value: toNumberOrNull(formData.ELPJ_control_value),
    ELPJ_accuracy: toNumberOrNull(formData.ELPJ_accuracy),
    elpv_evidence:
      evidenceUrls.elpv_evidence ?? fileValueToString(formData.elpv_evidence),
    elpvr_evidence:
      evidenceUrls.elpvr_evidence ?? fileValueToString(formData.elpvr_evidence),
    elpj_control_evidence:
      evidenceUrls.elpj_control_evidence ??
      fileValueToString(formData.elpj_control_evidence),
  };
}
