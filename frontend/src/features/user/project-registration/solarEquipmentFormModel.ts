import { getSolarFormSelectedFiles } from '@/features/user/project-registration/equipmentValidationMapper';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';

export const SOLAR_FORM_DEFAULT_VALUES: SolarPowerFormData = {
  siteId: '',
  changeDate: '',
  equipmentOpDate: '',
  operationStartDateFile: null,
  nominalMaxOutput: '',
  nominalMaxOutputFile: null,
  pcsManufacturer: '',
  pcsModel: '',
  powerCondUnitCount: '',
  deviceNo: '',
  powerCondStandby: '',
  pcsFile: null,
  hasBattery: '',
  batteryMaker: '',
  batteryModel: '',
  batteryCapacity: '',
  batteryFile: null,
  totalInvestment: '',
  totalInvestmentFile: null,
  hasSubsidy: '',
  subsidyName: '',
  subsidyNameFile: null,
  subsidyAmount: '',
  preProjectUnitPrice: '',
  preProjectUnitPriceFile: null,
};

export const REQUIRED_EQUIPMENT_FILE_MESSAGE =
  '確認資料をアップロードしてください。';

const API_FIELD_ALIASES: Record<string, keyof SolarPowerFormData> = {
  拠点: 'siteId',
  変更日: 'changeDate',
  changedate: 'changeDate',
  '稼働開始日（電力受給開始日）': 'equipmentOpDate',
  pcsunitcount: 'powerCondUnitCount',
  pcsstandby: 'powerCondStandby',
  solar_contract: 'operationStartDateFile',
  panel_catalog: 'nominalMaxOutputFile',
  pcs_spec: 'pcsFile',
  battery_catalog: 'batteryFile',
  construction_invoice: 'totalInvestmentFile',
  subsidy_notice: 'subsidyNameFile',
  electricity_bill: 'preProjectUnitPriceFile',
};

const FILE_ERROR_HINTS: ReadonlyArray<{
  hints: string[];
  fieldName: keyof SolarPowerFormData;
}> = [
  {
    hints: ['solar_contract', '太陽光発電電力受給契約確認書'],
    fieldName: 'operationStartDateFile',
  },
  {
    hints: ['panel_catalog', 'パネル仕様書'],
    fieldName: 'nominalMaxOutputFile',
  },
  {
    hints: ['battery_catalog', '蓄電池仕様書'],
    fieldName: 'batteryFile',
  },
  {
    hints: ['construction_invoice', '工事請負契約書'],
    fieldName: 'totalInvestmentFile',
  },
  {
    hints: ['subsidy_notice', '補助金交付決定通知書'],
    fieldName: 'subsidyNameFile',
  },
  {
    hints: ['electricity_bill', '電力会社からの請求明細書'],
    fieldName: 'preProjectUnitPriceFile',
  },
  {
    hints: ['pcs_spec', '確認資料：仕様書・カタログ'],
    fieldName: 'pcsFile',
  },
];

function getFileFieldFromError(path: string, message: string) {
  const searchableError = `${path} ${message}`.toLowerCase();
  return (
    FILE_ERROR_HINTS.find(({ hints }) =>
      hints.some((hint) => searchableError.includes(hint.toLowerCase())),
    )?.fieldName ?? null
  );
}

const REQUIRED_FIELD_MESSAGES: Partial<
  Record<keyof SolarPowerFormData, string>
> = {
  equipmentOpDate: '稼働開始日を入力してください。',
  nominalMaxOutput: '公称最大出力を入力してください。',
  pcsManufacturer: 'メーカーを入力してください。',
  pcsModel: '型式を入力してください。',
  powerCondUnitCount: '導入台数を入力してください。',
  deviceNo: '機器固有番号を入力してください。',
  powerCondStandby: '夜間待機電力を入力してください。',
  hasBattery: '蓄電池設置の有無を選択してください。',
  batteryMaker: '蓄電池メーカーを入力してください。',
  batteryModel: '蓄電池型式を入力してください。',
  batteryCapacity: '蓄電池容量を入力してください。',
  totalInvestment: '設備の総投資額を入力してください。',
  hasSubsidy: '補助金受給の有無を選択してください。',
  subsidyName: '補助金名称を入力してください。',
  subsidyAmount: '補助金額を入力してください。',
  preProjectUnitPrice: '電力購入単価を入力してください。',
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
  const fieldName = path
    .replace(/\[\d+\]/g, '')
    .split('.')
    .at(-1)
    ?.split('::')
    .at(-1)
    ?.trim();
  if (!fieldName) return null;

  return (
    API_FIELD_ALIASES[fieldName.toLowerCase()] ??
    (FORM_FIELD_NAMES.has(fieldName as keyof SolarPowerFormData)
      ? (fieldName as keyof SolarPowerFormData)
      : null)
  );
}

export type EquipmentValidationFieldError = {
  fieldName: keyof SolarPowerFormData;
  message: string;
};

export function getMissingEquipmentFileFieldErrors(
  formData: SolarPowerFormData,
): EquipmentValidationFieldError[] {
  return getMissingEquipmentFileFields(formData).map((fieldName) => ({
    fieldName,
    message: REQUIRED_EQUIPMENT_FILE_MESSAGE,
  }));
}

export function getMissingEquipmentRequiredFieldErrors(
  formData: SolarPowerFormData,
): EquipmentValidationFieldError[] {
  const requiredFields: Array<keyof SolarPowerFormData> = [
    'equipmentOpDate',
    'nominalMaxOutput',
    'pcsManufacturer',
    'pcsModel',
    'powerCondUnitCount',
    'deviceNo',
    'powerCondStandby',
    'hasBattery',
    ...(formData.hasBattery === 'yes'
      ? (['batteryMaker', 'batteryModel', 'batteryCapacity'] as const)
      : []),
    'totalInvestment',
    'hasSubsidy',
    ...(formData.hasSubsidy === 'yes'
      ? (['subsidyName', 'subsidyAmount'] as const)
      : []),
    'preProjectUnitPrice',
  ];
  const scalarErrors = requiredFields.flatMap((fieldName) => {
    const value = formData[fieldName];
    if (typeof value === 'string' && value.trim()) return [];

    return [
      {
        fieldName,
        message:
          REQUIRED_FIELD_MESSAGES[fieldName] ?? '必須項目を入力してください。',
      },
    ];
  });

  return [...scalarErrors, ...getMissingEquipmentFileFieldErrors(formData)];
}

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
    const hintedFileField = getFileFieldFromError(normalizedPath, message);
    if (hintedFileField) {
      if (!mappedErrors.has(hintedFileField)) {
        mappedErrors.set(hintedFileField, message);
      }
      return;
    }
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
