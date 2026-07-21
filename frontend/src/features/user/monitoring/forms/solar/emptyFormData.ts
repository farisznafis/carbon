import { MonitoringSolarFormData } from "./MonitoringSolarForm";

export const emptyFormData: MonitoringSolarFormData = {
  projectId: '',
  siteId: '',
  dateRecorded: '',
  ELpv_value: null,
  ELpvr_value: null,
  changedBattery: false,
  ELPJ_battery_value: null,
  ELPJ_control_value: null,
  ELPJ_accuracy: null,
  ELpv_evidence: null,
  ELpvr_evidence: null,
  ELPJ_battery_evidence: null,
  ELPJ_control_equipment_spec_files: null,
  description: '',
};

export const emptySolarMonitoring = { ...emptyFormData, projectType: 'solar' };