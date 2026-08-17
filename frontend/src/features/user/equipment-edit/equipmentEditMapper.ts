import type {
  SiteProjectEquipment,
  SiteProjectEquipmentFile,
  SiteProjectEquipmentUpdateRequest,
  SiteProjectEquipmentValidationFile,
} from '@/api/site';
import { getEquipmentFileDisplayName } from '@/features/user/equipment/equipmentFileDisplay';

export { getEquipmentFileDisplayName } from '@/features/user/equipment/equipmentFileDisplay';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';
import { SOLAR_FORM_DEFAULT_VALUES } from '@/features/user/project-registration/solarEquipmentFormModel';
import { getSolarFormSelectedFiles } from '@/features/user/project-registration/equipmentValidationMapper';

function formString(value: string | number | null | undefined) {
  return value == null ? '' : String(value);
}

function formYesNo(value: boolean | null | undefined) {
  if (value === true) return 'yes' as const;
  if (value === false) return 'no' as const;
  return '' as const;
}

export function mapEquipmentToEditForm(
  equipment: SiteProjectEquipment,
  siteId: string,
): SolarPowerFormData {
  return {
    ...SOLAR_FORM_DEFAULT_VALUES,
    siteId,
    equipmentOpDate: equipment.equipmentOpDate ?? '',
    nominalMaxOutput: formString(equipment.nominalMaxOutput),
    pcsManufacturer: equipment.pcsManufacturer ?? '',
    pcsModel: equipment.pcsModel ?? '',
    powerCondUnitCount: formString(equipment.powerCondUnitCount),
    deviceNo: equipment.deviceNo ?? '',
    powerCondStandby: formString(equipment.powerCondStandby),
    hasBattery: formYesNo(equipment.hasBattery),
    batteryMaker: equipment.batteryMaker ?? '',
    batteryModel: equipment.batteryModel ?? '',
    batteryCapacity: formString(equipment.batteryCapacity),
    totalInvestment: formString(equipment.totalInvestment),
    hasSubsidy: formYesNo(equipment.hasSubsidy),
    subsidyName: equipment.subsidyName ?? '',
    subsidyAmount: formString(equipment.subsidyAmount),
    preProjectUnitPrice: formString(equipment.preProjectUnitPrice),
  };
}

function textValue(value: string) {
  return value.trim();
}

function numberValue(value: string) {
  if (!value.trim()) return null;

  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : null;
}

function booleanValue(value: SolarPowerFormData['hasBattery']) {
  if (value === 'yes') return true;
  if (value === 'no') return false;
  return null;
}

function assignChangedValue<
  Key extends keyof SiteProjectEquipmentUpdateRequest,
>(
  payload: SiteProjectEquipmentUpdateRequest,
  key: Key,
  currentValue: SiteProjectEquipmentUpdateRequest[Key],
  originalValue: SiteProjectEquipmentUpdateRequest[Key],
) {
  if (!Object.is(currentValue, originalValue)) {
    payload[key] = currentValue;
  }
}

export function mapEquipmentEditToUpdateRequest(
  current: SolarPowerFormData,
  original: SolarPowerFormData,
  files: SiteProjectEquipmentValidationFile[] = [],
): SiteProjectEquipmentUpdateRequest {
  const payload: SiteProjectEquipmentUpdateRequest = {};

  assignChangedValue(
    payload,
    'equipmentOpDate',
    textValue(current.equipmentOpDate),
    textValue(original.equipmentOpDate),
  );
  assignChangedValue(
    payload,
    'nominalMaxOutput',
    numberValue(current.nominalMaxOutput),
    numberValue(original.nominalMaxOutput),
  );
  assignChangedValue(
    payload,
    'pcsManufacturer',
    textValue(current.pcsManufacturer),
    textValue(original.pcsManufacturer),
  );
  assignChangedValue(
    payload,
    'pcsModel',
    textValue(current.pcsModel),
    textValue(original.pcsModel),
  );
  assignChangedValue(
    payload,
    'pcsUnitCount',
    numberValue(current.powerCondUnitCount),
    numberValue(original.powerCondUnitCount),
  );
  assignChangedValue(
    payload,
    'deviceNo',
    textValue(current.deviceNo),
    textValue(original.deviceNo),
  );
  assignChangedValue(
    payload,
    'pcsStandby',
    numberValue(current.powerCondStandby),
    numberValue(original.powerCondStandby),
  );
  assignChangedValue(
    payload,
    'hasBattery',
    booleanValue(current.hasBattery),
    booleanValue(original.hasBattery),
  );

  if (current.hasBattery === 'yes') {
    assignChangedValue(
      payload,
      'batteryMaker',
      textValue(current.batteryMaker),
      textValue(original.batteryMaker),
    );
    assignChangedValue(
      payload,
      'batteryModel',
      textValue(current.batteryModel),
      textValue(original.batteryModel),
    );
    assignChangedValue(
      payload,
      'batteryCapacity',
      numberValue(current.batteryCapacity),
      numberValue(original.batteryCapacity),
    );
  }

  assignChangedValue(
    payload,
    'totalInvestment',
    numberValue(current.totalInvestment),
    numberValue(original.totalInvestment),
  );
  assignChangedValue(
    payload,
    'hasSubsidy',
    booleanValue(current.hasSubsidy),
    booleanValue(original.hasSubsidy),
  );

  if (current.hasSubsidy === 'yes') {
    assignChangedValue(
      payload,
      'subsidyName',
      textValue(current.subsidyName),
      textValue(original.subsidyName),
    );
    assignChangedValue(
      payload,
      'subsidyAmount',
      numberValue(current.subsidyAmount),
      numberValue(original.subsidyAmount),
    );
  }

  assignChangedValue(
    payload,
    'preProjectUnitPrice',
    numberValue(current.preProjectUnitPrice),
    numberValue(original.preProjectUnitPrice),
  );

  if (files.length > 0) {
    payload.files = files;
  }

  return payload;
}

export function mergeEquipmentFilesForUpdate(
  existingFiles: SiteProjectEquipmentFile[],
  uploadedFiles: SiteProjectEquipmentValidationFile[],
): SiteProjectEquipmentValidationFile[] {
  const mergedFiles = new Map<string, SiteProjectEquipmentValidationFile>();
  existingFiles.forEach((file) => {
    if (!file.fileType || !file.s3Key || !file.fileId) return;

    mergedFiles.set(file.fileType, {
      fileType: file.fileType as SiteProjectEquipmentValidationFile['fileType'],
      s3Key: file.s3Key,
      fileName: getEquipmentFileDisplayName(file),
      fileId: file.fileId,
    });
  });
  uploadedFiles.forEach((file) => mergedFiles.set(file.fileType, file));

  return Array.from(mergedFiles.values());
}

export function hasEquipmentEditChanges(
  current: SolarPowerFormData,
  original: SolarPowerFormData,
) {
  return (
    Object.keys(mapEquipmentEditToUpdateRequest(current, original)).length > 0 ||
    getSolarFormSelectedFiles(current).length > 0
  );
}
