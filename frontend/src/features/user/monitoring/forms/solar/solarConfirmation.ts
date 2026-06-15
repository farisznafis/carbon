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
    { label: 'ELpv value', value: String(data.ELpv_value ?? '未入力'), isChanged: isChanged('ELpv_value') },
    { label: 'ELpvr value', value: String(data.ELpvr_value ?? '未入力'), isChanged: isChanged('ELpvr_value') },
    { label: '蓄電池交換', value: data.changedBattery ? '有' : '無', isChanged: isChanged('changedBattery') },
    { label: 'ELPJ battery value', value: String(data.ELPJ_battery_value ?? '未入力'), isChanged: isChanged('ELPJ_battery_value') },
    { label: 'ELPJ control value', value: String(data.ELPJ_control_value ?? '未入力'), isChanged: isChanged('ELPJ_control_value') },
    { label: 'ELPJ accuracy', value: String(data.ELPJ_accuracy ?? '未入力'), isChanged: isChanged('ELPJ_accuracy') },
    { label: 'ELpv 証拠', value: formatOptionalFile(data.ELpv_evidence), isChanged: isChanged('ELpv_evidence') },
    { label: 'ELpvr 証拠', value: formatOptionalFile(data.ELpvr_evidence), isChanged: isChanged('ELpvr_evidence') },
    { label: 'ELPJ battery 証拠', value: formatOptionalFile(data.ELPJ_battery_evidence), isChanged: isChanged('ELPJ_battery_evidence') },
    { label: 'ELPJ control 仕様', value: formatOptionalFile(data.ELPJ_control_equipment_spec_files), isChanged: isChanged('ELPJ_control_equipment_spec_files') },
  ];
}
