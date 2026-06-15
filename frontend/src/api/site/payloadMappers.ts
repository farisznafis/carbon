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
  return {
    siteName: formData.siteName,
    siteNameKana: formData.siteNameKana,
    postalCode: formData.postalCode,
    prefName: formData.prefectureName,
    cityName: formData.cityName,
    townName: formData.townName,
    blockName: formData.blockName,
    address: buildAddress(formData),
    picFirstName: formData.picFirstName,
    picFirstNameKana: formData.picFirstNameKana,
    picLastName: formData.picLastName,
    picLastNameKana: formData.picLastNameKana,
    picEmail: formData.picEmail,
    picPhoneNumber: formData.picPhoneNumber,
    picPosition: formData.picPosition,
  };
}

export function mapUpdateSitePayload(formData: SiteFormData): UpdateSitePayload {
  return {
    postalCode: formData.postalCode,
    prefName: formData.prefectureName,
    cityName: formData.cityName,
    townName: formData.townName,
    blockName: formData.blockName,
    address: buildAddress(formData),
  };
}
