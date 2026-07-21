import type {
  SiteProjectEquipmentValidationFile,
  SiteProjectEquipmentValidationItem,
} from '@/api/site';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';

function optionalTrimmed(value: string) {
  const trimmedValue = value.trim();
  return trimmedValue || undefined;
}

function validationNumber(value: string) {
  if (!value.trim()) return null;

  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : null;
}

function optionalValidationNumber(value: string) {
  return value.trim() ? validationNumber(value) : undefined;
}

function validationBoolean(value: SolarPowerFormData['hasBattery']) {
  if (value === 'yes') return true;
  if (value === 'no') return false;
  return null;
}

export function mapSolarFormToEquipmentValidationItem(
  formData: SolarPowerFormData,
  tempId: string,
  methodologyNumber: string,
  files: SiteProjectEquipmentValidationFile[],
): SiteProjectEquipmentValidationItem {
  const hasBattery = validationBoolean(formData.hasBattery);
  const hasAdditionalEquipment = validationBoolean(
    formData.additionalEquipment,
  );
  const hasSubsidy = validationBoolean(formData.hasSubsidy);

  return {
    tempId,
    methodologyNumber,
    equipmentOpDate: formData.equipmentOpDate,
    nominalMaxOutput: validationNumber(formData.nominalMaxOutput),
    pcsManufacturer: formData.pcsManufacturer.trim(),
    pcsModel: formData.pcsModel.trim(),
    powerCondUnitCount: validationNumber(formData.powerCondUnitCount),
    deviceNo: formData.deviceNo.trim(),
    powerCondStandby: validationNumber(formData.powerCondStandby),
    hasBattery,
    ...(hasBattery === true
      ? {
          batteryMaker: formData.batteryMaker.trim(),
          batteryModel: formData.batteryModel.trim(),
          batteryCapacity: validationNumber(formData.batteryCapacity),
        }
      : {}),
    additionalEquipment: hasAdditionalEquipment,
    ictEquipmentPowerConsumption:
      hasAdditionalEquipment === false
        ? 0
        : validationNumber(formData.ictEquipmentPowerConsumption),
    ...(hasAdditionalEquipment === true
      ? {
          additionalEquipmentDate: formData.additionalEquipmentDate,
          additionalEquipmentType:
            optionalTrimmed(formData.additionalEquipmentType),
          additionalUnitCount: optionalValidationNumber(
            formData.additionalUnitCount,
          ),
          additionalDeviceUniqueNo: optionalTrimmed(
            formData.additionalDeviceUniqueNo,
          ),
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
