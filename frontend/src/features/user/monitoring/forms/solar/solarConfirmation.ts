import { ConfirmationItem } from '../confirmationItem';
import { MonitoringSolarFormData } from './MonitoringSolarForm';

function formatOptionalFile(value: unknown): string {
  if (!value) return '未添付';
  return value instanceof File ? value.name : String(value);
}

export function getMonitoringSolarConfirmation(
  data: MonitoringSolarFormData,
  original?: MonitoringSolarFormData,
): ConfirmationItem[] {
  const isChanged = (key: keyof MonitoringSolarFormData) =>
    original?.[key] !== data[key];

  return [
    { label: '記録日', value: data.dateRecorded || '未入力', isChanged: isChanged('dateRecorded') },
    { label: '発電量 (ELPV)', value: String(data.ELpv_value ?? '未入力'), isChanged: isChanged('ELpv_value') },
    { label: '自家消費量 (ELPVR)', value: String(data.ELpvr_value ?? '未入力'), isChanged: isChanged('ELpvr_value') },
    { label: '蓄電池交換の有無', value: data.changedBattery ? '有' : '無', isChanged: isChanged('changedBattery') },
    { label: '蓄電池放電量 (ELPJ)', value: String(data.ELPJ_battery_value ?? '未入力'), isChanged: isChanged('ELPJ_battery_value') },
    { label: '制御機器稼働時間 (ELPJ)', value: String(data.ELPJ_control_value ?? '未入力'), isChanged: isChanged('ELPJ_control_value') },
    { label: '測定精度', value: String(data.ELPJ_accuracy ?? '未入力'), isChanged: isChanged('ELPJ_accuracy') },
    { label: '発電量 (ELPV) エビデンス', value: formatOptionalFile(data.ELpv_evidence), isChanged: isChanged('ELpv_evidence') },
    { label: '自家消費量 (ELPVR) エビデンス', value: formatOptionalFile(data.ELpvr_evidence), isChanged: isChanged('ELpvr_evidence') },
    { label: '蓄電池エビデンス', value: formatOptionalFile(data.ELPJ_battery_evidence), isChanged: isChanged('ELPJ_battery_evidence') },
    { label: '制御機器仕様 (ELPJ) エビデンス', value: formatOptionalFile(data.ELPJ_control_equipment_spec_files), isChanged: isChanged('ELPJ_control_equipment_spec_files') },
  ];
}
