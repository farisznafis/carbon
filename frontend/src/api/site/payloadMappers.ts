import type { SiteFormData } from './types';
import type { CreateSitePayload, UpdateSitePayload } from './payloads';

export function buildAddress(formData: SiteFormData): string {
  return [
    formData.prefectureName,
    formData.cityName,
    formData.townName,
    formData.blockName,
    formData.addressDetail,
  ]
    .filter(Boolean)
    .join('');
}

export function mapCreateSitePayload(formData: SiteFormData): CreateSitePayload {
  const payload: CreateSitePayload = {
    siteName: formData.siteName,
    siteNameKana: formData.siteNameKana,
    postalCode: formData.postalCode,
    prefecture: formData.prefectureName,
    municipality: formData.cityName,
    blocknum: formData.blockName,
    buildingName: formData.addressDetail,
  };

  if (formData.userId) {
    payload.userId = Number(formData.userId);
  }

  return payload;
}

export function mapUpdateSitePayload(
  formData: SiteFormData,
  siteId: number,
  picuserId: number | null,
): UpdateSitePayload {
  return {
    siteName: formData.siteName,
    postalCode: formData.postalCode,
    prefecture: formData.prefectureName,
    municipality: formData.cityName,
    blocknum: formData.blockName,
    buildingName: formData.addressDetail,
    siteId,
    picuserId,
  };
}