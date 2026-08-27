import type { SiteProjectEquipmentFile } from '@/api/site';

const equipmentFileTypeLabels: Record<string, string> = {
  solar_contract: '太陽光発電電力受給契約確認書',
  panel_catalog: '仕様書・カタログ',
  pcs_spec: 'PCS仕様書',
  battery_catalog: '蓄電池仕様書・カタログ',
  construction_invoice: '工事請負契約書・請求書等',
  subsidy_notice: '補助金交付決定通知書',
  electricity_bill: '電力会社請求明細書等',
};

export function formatEquipmentFileType(
  fileType: string | null | undefined,
) {
  if (!fileType) return '確認資料';
  return equipmentFileTypeLabels[fileType] ?? fileType;
}

export function getEquipmentFileDisplayName(
  file: SiteProjectEquipmentFile,
) {
  if (file.fileName?.trim()) return file.fileName.trim();

  const encodedName = file.s3Key?.split('/').filter(Boolean).at(-1);
  if (encodedName) {
    try {
      return decodeURIComponent(encodedName);
    } catch {
      return encodedName;
    }
  }

  return file.fileId?.trim() || '-';
}
