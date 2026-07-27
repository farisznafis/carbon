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
    ...(formData.hasBattery === 'yes' ? ['batteryFile' as const] : []),
    'totalInvestmentFile',
    ...(formData.hasSubsidy === 'yes' ? ['subsidyNameFile' as const] : []),
    'preProjectUnitPriceFile',
  ];

  return requiredFields.filter(
    (fieldName) => !selectedFileFields.has(fieldName),
  );
}

export function getEquipmentValidationErrorMessage(
  error: unknown,
): string | null {
  if (typeof error === 'string') return error;
  if (Array.isArray(error)) {
    const messages = error
      .map(getEquipmentValidationErrorMessage)
      .filter((message): message is string => Boolean(message));
    return messages.length > 0 ? messages.join('、') : null;
  }
  if (!error || typeof error !== 'object') return null;

  const errorRecord = error as Record<string, unknown>;
  const directMessage = getEquipmentValidationErrorMessage(
    errorRecord.message,
  );
  if (directMessage) return directMessage;

  const constraintMessage = getEquipmentValidationErrorMessage(
    errorRecord.constraints,
  );
  if (constraintMessage) return constraintMessage;

  const messages = Object.values(errorRecord)
    .map(getEquipmentValidationErrorMessage)
    .filter((message): message is string => Boolean(message));
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

type EquipmentValidationFieldError = {
  fieldName: keyof SolarPowerFormData;
  message: string;
};

function flattenEquipmentValidationErrors(
  error: unknown,
  path = '',
): Array<{ path: string; message: string }> {
  if (typeof error === 'string') {
    return path ? [{ path, message: error }] : [];
  }

  if (Array.isArray(error)) {
    if (error.every((entry) => typeof entry === 'string')) {
      const message = getEquipmentValidationErrorMessage(error);
      return path && message ? [{ path, message }] : [];
    }

    return error.flatMap((entry, index) =>
      flattenEquipmentValidationErrors(entry, `${path}[${index}]`),
    );
  }

  if (!error || typeof error !== 'object') return [];

  const errorRecord = error as Record<string, unknown>;
  const directMessage = getEquipmentValidationErrorMessage(
    errorRecord.message,
  );
  if (path && directMessage) {
    return [{ path, message: directMessage }];
  }
  const constraintMessage = getEquipmentValidationErrorMessage(
    errorRecord.constraints,
  );
  if (path && constraintMessage) {
    return [{ path, message: constraintMessage }];
  }

  return Object.entries(errorRecord).flatMap(([key, value]) => {
    if (key === 'message' || key === 'constraints') return [];
    const nextPath = path ? `${path}.${key}` : key;
    return flattenEquipmentValidationErrors(value, nextPath);
  });
}

export function mapEquipmentValidationFieldErrors(
  errors: unknown,
  formData: SolarPowerFormData,
): EquipmentValidationFieldError[] {
  const mappedErrors = new Map<keyof SolarPowerFormData, string>();
  const selectedFiles = getSolarFormSelectedFiles(formData);

  flattenEquipmentValidationErrors(errors).forEach(({ path, message }) => {
    const normalizedPath = path
      .replaceAll('request::', '')
      .replaceAll('equipment::', '');
    const filePathMatch = normalizedPath.match(
      /(?:^|\.)files(?:\[(\d+)\])?(?:\.|$)/,
    );

    if (filePathMatch) {
      const fileIndex = filePathMatch[1]
        ? Number(filePathMatch[1])
        : null;
      const indexedFileField =
        fileIndex == null ? null : selectedFiles[fileIndex]?.fieldName;
      const targetFileFields = indexedFileField
        ? [indexedFileField]
        : getMissingEquipmentFileFields(formData);
      const fallbackFileFields =
        targetFileFields.length > 0
          ? targetFileFields
          : selectedFiles.map(({ fieldName }) => fieldName);

      fallbackFileFields.forEach((fieldName) => {
        if (!mappedErrors.has(fieldName)) {
          mappedErrors.set(fieldName, message);
        }
      });
      return;
    }

    const fieldName = getEquipmentFormFieldName(normalizedPath);
    if (fieldName && !mappedErrors.has(fieldName)) {
      mappedErrors.set(fieldName, message);
    }
  });

  return Array.from(mappedErrors, ([fieldName, message]) => ({
    fieldName,
    message,
  }));
}
