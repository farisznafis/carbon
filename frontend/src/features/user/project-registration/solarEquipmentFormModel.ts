import { getSolarFormSelectedFiles } from '@/features/user/project-registration/equipmentValidationMapper';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';

export const SOLAR_FORM_DEFAULT_VALUES: SolarPowerFormData = {
  siteId: '',
  equipmentOpDate: '',
  operationStartDateFile: undefined,
  nominalMaxOutput: '',
  nominalMaxOutputFile: undefined,
  pcsManufacturer: '',
  pcsModel: '',
  powerCondUnitCount: '',
  deviceNo: '',
  powerCondStandby: '',
  pcsFile: undefined,
  hasBattery: '',
  batteryMaker: '',
  batteryModel: '',
  batteryCapacity: '',
  batteryFile: undefined,
  additionalEquipment: '',
  ictEquipmentPowerConsumption: '',
  additionalEquipmentDate: '',
  additionalEquipmentType: '',
  additionalUnitCount: '',
  additionalDeviceUniqueNo: '',
  totalInvestment: '',
  totalInvestmentFile: undefined,
  hasSubsidy: '',
  subsidyName: '',
  subsidyNameFile: undefined,
  subsidyAmount: '',
  preProjectUnitPrice: '',
  preProjectUnitPriceFile: undefined,
};

export const REQUIRED_EQUIPMENT_FILE_MESSAGE =
  '確認資料をアップロードしてください。';

const API_FIELD_ALIASES: Record<string, keyof SolarPowerFormData> = {
  pcsUnitCount: 'powerCondUnitCount',
  pcsStandby: 'powerCondStandby',
};

const FORM_FIELD_NAMES = new Set<keyof SolarPowerFormData>(
  Object.keys(SOLAR_FORM_DEFAULT_VALUES) as Array<keyof SolarPowerFormData>,
);

export function getMissingEquipmentFileFields(
  formData: SolarPowerFormData,
): Array<keyof SolarPowerFormData> {
  const selectedFileFields = new Set<keyof SolarPowerFormData>(
    getSolarFormSelectedFiles(formData).map(({ fieldName }) => fieldName),
  );
  const requiredFields: Array<keyof SolarPowerFormData> = [
    'operationStartDateFile',
    'nominalMaxOutputFile',
    'pcsFile',
    'totalInvestmentFile',
    'preProjectUnitPriceFile',
    ...(formData.hasBattery === 'yes' ? ['batteryFile' as const] : []),
    ...(formData.hasSubsidy === 'yes' ? ['subsidyNameFile' as const] : []),
  ];

  return requiredFields.filter(
    (fieldName) => !selectedFileFields.has(fieldName),
  );
}

export function getEquipmentValidationErrorMessage(
  error: unknown,
): string | null {
  if (typeof error === 'string') return error;
  if (!Array.isArray(error)) return null;

  const messages = error.filter(
    (message): message is string => typeof message === 'string',
  );
  return messages.length > 0 ? messages.join('、') : null;
}

export function getEquipmentFormFieldName(
  path: string,
): keyof SolarPowerFormData | null {
  const fieldName = path.replace(/\[\d+\]/g, '').split('.').at(-1);
  if (!fieldName) return null;

  return (
    API_FIELD_ALIASES[fieldName] ??
    (FORM_FIELD_NAMES.has(fieldName as keyof SolarPowerFormData)
      ? (fieldName as keyof SolarPowerFormData)
      : null)
  );
}
