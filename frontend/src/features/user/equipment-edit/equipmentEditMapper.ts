import type {
  SiteProjectEquipment,
  SiteProjectEquipmentFile,
  SiteProjectEquipmentChangeRequest,
  SiteProjectEquipmentValidationFile,
} from '@/api/site';
import { getEquipmentFileDisplayName } from '@/features/user/equipment/equipmentFileDisplay';

export { getEquipmentFileDisplayName } from '@/features/user/equipment/equipmentFileDisplay';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';
import { SOLAR_FORM_DEFAULT_VALUES } from '@/features/user/project-registration/solarEquipmentFormModel';
import { getSolarFormSelectedFiles } from '@/features/user/project-registration/equipmentValidationMapper';

const EQUIPMENT_CHANGE_COMPARISON_FIELDS: ReadonlyArray<
  keyof SolarPowerFormData
> = [
  'nominalMaxOutput',
  'pcsManufacturer',
  'pcsModel',
  'deviceNo',
  'powerCondStandby',
  'hasBattery',
  'batteryMaker',
  'batteryModel',
  'batteryCapacity',
  'totalInvestment',
  'hasSubsidy',
  'subsidyName',
  'subsidyAmount',
  'preProjectUnitPrice',
];

function formString(value: string | number | null | undefined) {
  return value == null ? '' : String(value);
}

function formYesNo(value: boolean | null | undefined) {
  if (value === true) return 'yes' as const;
  if (value === false) return 'no' as const;
  return '' as const;
}

/** Returns a browser-local date value without UTC date shifting. */
function getLocalDateInputValue(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function mapEquipmentToEditForm(
  equipment: SiteProjectEquipment,
  siteId: string,
): SolarPowerFormData {
  return {
    ...SOLAR_FORM_DEFAULT_VALUES,
    siteId,
    changeDate: getLocalDateInputValue(),
    equipmentOpDate: equipment.equipmentOpDate ?? '',
    nominalMaxOutput: formString(equipment.nominalMaxOutput),
    pcsManufacturer: equipment.pcsManufacturer ?? '',
    pcsModel: equipment.pcsModel ?? '',
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

/** Builds the complete equipment snapshot required by the change endpoint. */
export function mapEquipmentEditToChangeRequest(
  current: SolarPowerFormData,
  files: SiteProjectEquipmentValidationFile[] = [],
): SiteProjectEquipmentChangeRequest {
  const hasBattery = booleanValue(current.hasBattery);
  const hasSubsidy = booleanValue(current.hasSubsidy);

  return {
    changeDate: textValue(current.changeDate ?? ''),
    nominalMaxOutput: numberValue(current.nominalMaxOutput),
    pcsManufacturer: textValue(current.pcsManufacturer),
    pcsModel: textValue(current.pcsModel),
    deviceNo: textValue(current.deviceNo),
    pcsStandby: numberValue(current.powerCondStandby),
    hasBattery,
    batteryMaker:
      hasBattery === true ? textValue(current.batteryMaker) : null,
    batteryModel:
      hasBattery === true ? textValue(current.batteryModel) : null,
    batteryCapacity:
      hasBattery === true ? numberValue(current.batteryCapacity) : null,
    totalInvestment: numberValue(current.totalInvestment),
    hasSubsidy,
    subsidyName:
      hasSubsidy === true ? textValue(current.subsidyName) : null,
    subsidyAmount:
      hasSubsidy === true ? numberValue(current.subsidyAmount) : null,
    preProjectUnitPrice: numberValue(current.preProjectUnitPrice),
    files,
  };
}

/** Combines inherited evidence metadata with newly uploaded replacement documents. */
export function mergeEquipmentFilesForChange(
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
    EQUIPMENT_CHANGE_COMPARISON_FIELDS.some(
      (fieldName) => current[fieldName] !== original[fieldName],
    ) ||
    getSolarFormSelectedFiles(current).length > 0
  );
}
