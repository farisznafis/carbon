import { ConfirmationItem } from '../confirmationItem';
import { MonitoringSolarFormData } from './MonitoringSolarForm';

const missingValue = '未入力';
const missingFile = '未添付';

/** Displays the selected evidence filename or the existing evidence URL. */
function formatOptionalFile(value: unknown): string {
  if (!value) return missingFile;
  return value instanceof File ? value.name : String(value);
}

/** Lists monitoring fields and battery details according to equipment eligibility. */
export function getMonitoringSolarConfirmation(
  data: MonitoringSolarFormData,
  original?: MonitoringSolarFormData,
): ConfirmationItem[] {
  /** Marks changes only when comparing against an existing monitoring record. */
  const isChanged = (key: keyof MonitoringSolarFormData) =>
    original !== undefined && original[key] !== data[key];

  return [
    {
      label: '設備ID',
      value: data.equipmentId == null ? missingValue : String(data.equipmentId),
      isChanged: isChanged('equipmentId'),
    },
    { label: '記録日', value: data.dateRecorded || missingValue, isChanged: isChanged('dateRecorded') },
    { label: '発電量 (ELPV)', value: String(data.ELpv_value ?? missingValue), isChanged: isChanged('ELpv_value') },
    { label: '自家消費量 (ELPVR)', value: String(data.ELpvr_value ?? missingValue), isChanged: isChanged('ELpvr_value') },
    ...(data.equipmentHasBattery === true
      ? [{
          label: '蓄電池放電量 (ELPJ)',
          value: String(data.ELPJ_battery_value ?? missingValue),
          isChanged: isChanged('ELPJ_battery_value'),
        }]
      : []),
    { label: '制御機器稼働時間 (ELPJ)', value: String(data.ELPJ_control_value ?? missingValue), isChanged: isChanged('ELPJ_control_value') },
    { label: '測定精度', value: String(data.ELPJ_accuracy ?? missingValue), isChanged: isChanged('ELPJ_accuracy') },
    { label: '発電量 (ELPV) エビデンス', value: formatOptionalFile(data.elpv_evidence), isChanged: isChanged('elpv_evidence') },
    { label: '自家消費量 (ELPVR) エビデンス', value: formatOptionalFile(data.elpvr_evidence), isChanged: isChanged('elpvr_evidence') },
    ...(data.equipmentHasBattery === true
      ? [{ label: '蓄電池エビデンス', value: formatOptionalFile(data.elpj_battery_evidence), isChanged: isChanged('elpj_battery_evidence') }]
      : []),
    { label: '制御機器仕様 (ELPJ) エビデンス', value: formatOptionalFile(data.elpj_control_evidence), isChanged: isChanged('elpj_control_evidence') },
  ];
}
