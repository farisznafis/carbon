import type {
  SiteProjectEquipmentFileType,
  SiteProjectEquipmentCreateRequest,
  SiteProjectEquipmentValidationFile,
  SiteProjectEquipmentValidationItem,
} from '@/api/site';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';

type SolarFileFieldName =
  | 'operationStartDateFile'
  | 'nominalMaxOutputFile'
  | 'pcsFile'
  | 'batteryFile'
  | 'totalInvestmentFile'
  | 'subsidyNameFile'
  | 'preProjectUnitPriceFile';

const VALIDATION_FILE_FIELDS: ReadonlyArray<{
  fieldName: SolarFileFieldName;
  fileType: SiteProjectEquipmentFileType;
}> = [
  { fieldName: 'operationStartDateFile', fileType: 'solar_contract' },
  { fieldName: 'nominalMaxOutputFile', fileType: 'panel_catalog' },
  { fieldName: 'pcsFile', fileType: 'pcs_spec' },
  { fieldName: 'batteryFile', fileType: 'battery_catalog' },
  { fieldName: 'totalInvestmentFile', fileType: 'construction_invoice' },
  { fieldName: 'subsidyNameFile', fileType: 'subsidy_notice' },
  { fieldName: 'preProjectUnitPriceFile', fileType: 'electricity_bill' },
];

function validationNumber(value: string) {
  if (!value.trim()) return null;

  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : null;
}

function validationString(value: string) {
  return value.trim() || null;
}

function validationBoolean(value: SolarPowerFormData['hasBattery']) {
  if (value === 'yes') return true;
  if (value === 'no') return false;
  return null;
}

export function mapSolarFormFilesForValidation(
  formData: SolarPowerFormData,
): SiteProjectEquipmentValidationFile[] {
  return getSolarFormSelectedFiles(formData).map(({ fileType, file }) => {
    const pendingFileReference = [
      'pending-upload',
      fileType,
      file.lastModified,
      file.size,
    ].join('-');

    return {
      fileType,
      s3Key: pendingFileReference,
      fileName: file.name,
      fileId: pendingFileReference,
    };
  });
}

export function getSolarFormSelectedFiles(
  formData: SolarPowerFormData,
): Array<{
  fieldName: SolarFileFieldName;
  fileType: SiteProjectEquipmentFileType;
  file: File;
}> {
  return VALIDATION_FILE_FIELDS.flatMap(({ fieldName, fileType }) => {
    if (fieldName === 'batteryFile' && formData.hasBattery !== 'yes') return [];
    if (fieldName === 'subsidyNameFile' && formData.hasSubsidy !== 'yes') {
      return [];
    }

    const file = formData[fieldName];
    return file instanceof File ? [{ fieldName, fileType, file }] : [];
  });
}

export function mapSolarFormToEquipmentValidationItem(
  formData: SolarPowerFormData,
  tempId: string,
  siteId: string,
  files: SiteProjectEquipmentValidationFile[],
): SiteProjectEquipmentValidationItem {
  const hasBattery = validationBoolean(formData.hasBattery);
  const hasSubsidy = validationBoolean(formData.hasSubsidy);

  return {
    tempId,
    siteId: siteId.trim() || null,
    equipmentOpDate: validationString(formData.equipmentOpDate),
    nominalMaxOutput: validationNumber(formData.nominalMaxOutput),
    pcsManufacturer: validationString(formData.pcsManufacturer),
    pcsModel: validationString(formData.pcsModel),
    deviceNo: validationString(formData.deviceNo),
    pcsStandby: validationNumber(formData.powerCondStandby),
    hasBattery,
    ...(hasBattery === true
      ? {
          batteryMaker: validationString(formData.batteryMaker),
          batteryModel: validationString(formData.batteryModel),
          batteryCapacity: validationNumber(formData.batteryCapacity),
        }
      : {}),
    totalInvestment: validationNumber(formData.totalInvestment),
    hasSubsidy,
    ...(hasSubsidy === true
      ? {
          subsidyName: validationString(formData.subsidyName),
          subsidyAmount: validationNumber(formData.subsidyAmount),
        }
      : {}),
    preProjectUnitPrice: validationNumber(formData.preProjectUnitPrice),
    files,
  };
}

/** Maps confirmed solar form values to the create-equipment contract. */
export function mapSolarFormToEquipmentCreateRequest(
  formData: SolarPowerFormData,
  tempId: string,
  siteId: string,
  files: SiteProjectEquipmentValidationFile[],
): SiteProjectEquipmentCreateRequest {
  const payload = mapSolarFormToEquipmentValidationItem(
    formData,
    tempId,
    siteId,
    files,
  );

  return {
    ...payload,
    siteId,
    batteryMaker: payload.batteryMaker ?? null,
    batteryModel: payload.batteryModel ?? null,
    batteryCapacity: payload.batteryCapacity ?? null,
    subsidyName: payload.subsidyName ?? null,
    subsidyAmount: payload.subsidyAmount ?? null,
  };
}
