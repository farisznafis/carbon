import { apiFetch, apiVoidRequest, parseJsonResponse } from '../_shared/httpClient';
import type { Site, SiteFormData } from './types';
import { buildAddress, mapCreateSitePayload, mapUpdateSitePayload } from './payloadMappers';

type CreateSiteResponse = {
  statusCode: string;
  siteId?: string | number;
  message?: string | null;
  errors?: unknown;
};

type UpdateSiteResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: unknown;
  site?: Site;
};

export async function registerSite(formData: SiteFormData): Promise<{ statusCode: string; site: Site }> {
  const payload = mapCreateSitePayload(formData);

  const response = await apiFetch('/site', {
    method: 'POST',
    body: payload,
  });

  const { data: json, rawBody } = await parseJsonResponse<CreateSiteResponse>(response);

  if (!response.ok) {
    throw new Error(json?.message || rawBody || `Failed to create site: HTTP ${response.status}`);
  }

  return {
    statusCode: json?.statusCode || 'OK',
    site: {
      id: String(json?.siteId ?? ''),
      siteName: formData.siteName,
      postalCode: formData.postalCode,
      prefectureName: formData.prefectureName,
      cityName: formData.cityName,
      blockName: [formData.townName, formData.blockName].filter(Boolean).join(''),
      address: buildAddress(formData),
      picFirstName: formData.picFirstName,
      picLastName: formData.picLastName,
      picEmail: formData.picEmail,
      picPosition: formData.picPosition,
      picPhoneNumber: formData.picPhoneNumber,
      companyId: '',
      project: [],
      picId: '',
      addressDetail: formData.addressDetail,
    },
  };
}

export async function updateSite(siteId: string, formData: SiteFormData): Promise<{ statusCode: string; site?: Site }> {
  const payload = mapUpdateSitePayload(formData);

  const response = await apiFetch(`/site/${siteId}`, {
    method: 'PATCH',
    body: payload,
  });

  const { data: json, rawBody } = await parseJsonResponse<UpdateSiteResponse>(response);

  if (!response.ok) {
    throw new Error(json?.message || rawBody || `Failed to update site: HTTP ${response.status}`);
  }

  return {
    statusCode: json?.statusCode || 'OK',
    site: json?.site,
  };
}

export const deleteSite = async (siteId: string) => {
  await apiVoidRequest(`/site/${encodeURIComponent(siteId)}`, {
    method: 'DELETE',
    fallbackErrorMessage: '拠点の削除に失敗しました。',
  });
};
