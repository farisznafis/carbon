import { MonitoringSolarFormData } from "./MonitoringSolarForm";

export const emptyFormData: MonitoringSolarFormData = {
  projectId: '',
  siteId: '',
  equipmentId: null,
  equipmentHasBattery: false,
  dateRecorded: '',
  ELpv_value: null,
  ELpvr_value: null,
  changedBattery: null,
  ELPJ_battery_value: null,
  ELPJ_control_value: null,
  ELPJ_accuracy: null,
  elpv_evidence: null,
  elpvr_evidence: null,
  elpj_battery_evidence: null,
  elpj_control_evidence: null,
  description: '',
};

export const emptySolarMonitoring = { ...emptyFormData, projectType: 'solar' };
