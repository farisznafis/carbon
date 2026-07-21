import type {
  SiteProjectEquipmentValidationFile,
  SiteProjectEquipmentValidationItem,
} from '@/api/site';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';

function validationNumber(value: string) {
  if (!value.trim()) return null;

  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : null;
}

function validationBoolean(value: SolarPowerFormData['hasBattery']) {
  if (value === 'yes') return true;
  if (value === 'no') return false;
  return null;
}

export function mapSolarFormToEquipmentValidationItem(
  formData: SolarPowerFormData,
  tempId: string,
  siteId: string,
  files: SiteProjectEquipmentValidationFile[],
): SiteProjectEquipmentValidationItem {
  const hasBattery = validationBoolean(formData.hasBattery);
  const hasSubsidy = validationBoolean(formData.hasSubsidy);
  const numericSiteId = siteId.trim() ? Number(siteId) : Number.NaN;

  return {
    tempId,
    siteId: Number.isInteger(numericSiteId) ? numericSiteId : null,
    equipmentOpDate: formData.equipmentOpDate,
    nominalMaxOutput: validationNumber(formData.nominalMaxOutput),
    pcsManufacturer: formData.pcsManufacturer.trim(),
    pcsModel: formData.pcsModel.trim(),
    pcsUnitCount: validationNumber(formData.powerCondUnitCount),
    deviceNo: formData.deviceNo.trim(),
    pcsStandby: validationNumber(formData.powerCondStandby),
    hasBattery,
    ...(hasBattery === true
      ? {
          batteryMaker: formData.batteryMaker.trim(),
          batteryModel: formData.batteryModel.trim(),
          batteryCapacity: validationNumber(formData.batteryCapacity),
        }
      : {}),
    totalInvestment: validationNumber(formData.totalInvestment),
    hasSubsidy,
    ...(hasSubsidy === true
      ? {
          subsidyName: formData.subsidyName.trim(),
          subsidyAmount: validationNumber(formData.subsidyAmount),
        }
      : {}),
    preProjectUnitPrice: validationNumber(formData.preProjectUnitPrice),
    files,
  };
}
