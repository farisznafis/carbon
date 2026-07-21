import type {
  SiteProjectEquipmentFileType,
  SiteProjectEquipmentValidationFile,
  SiteProjectEquipmentValidationItem,
} from '@/api/site';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';

const VALIDATION_FILE_FIELDS: ReadonlyArray<{
  fieldName:
    | 'operationStartDateFile'
    | 'nominalMaxOutputFile'
    | 'pcsFile'
    | 'batteryFile'
    | 'totalInvestmentFile'
    | 'subsidyNameFile'
    | 'preProjectUnitPriceFile';
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

function validationBoolean(value: SolarPowerFormData['hasBattery']) {
  if (value === 'yes') return true;
  if (value === 'no') return false;
  return null;
}

export function mapSolarFormFilesForValidation(
  formData: SolarPowerFormData,
): SiteProjectEquipmentValidationFile[] {
  return VALIDATION_FILE_FIELDS.flatMap(({ fieldName, fileType }) => {
    const file = formData[fieldName];
    if (!(file instanceof File)) return [];

    const pendingFileReference = [
      'pending-upload',
      fileType,
      file.lastModified,
      file.size,
    ].join('-');

    return [
      {
        fileType,
        s3Key: pendingFileReference,
        fileName: file.name,
        fileId: pendingFileReference,
      },
    ];
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
