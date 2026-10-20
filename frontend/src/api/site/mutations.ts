import { apiFetch, apiVoidRequest, parseJsonResponse } from '../_shared/httpClient';
import type { Site, SiteFormData } from './types';
import { buildAddress, mapCreateSitePayload, mapUpdateSitePayload } from './payloadMappers';
import type { UserList } from '../users';

type CreateSiteResponse = {
  statusCode: string;
  siteId?: string;
  message?: string | null;
  errors?: unknown;
};

type UpdateSiteResponse = {
  statusCode?: string;
  message?: string | null;
  errors?: unknown;
  site?: Site;
};

export async function validateCreateSite(formData: SiteFormData): Promise<void> {
  await apiVoidRequest('/site/validate-create', {
    method: 'POST',
    body: mapCreateSitePayload(formData),
    fallbackErrorMessage: '拠点登録内容の検証に失敗しました',
  });
}

export async function validateUpdateSite(
  siteId: string,
  formData: SiteFormData,
  picuserId: number | null = null,
): Promise<void> {
  await apiVoidRequest(`/site/validate-update/${encodeURIComponent(siteId)}`, {
    method: 'POST',
    body: mapUpdateSitePayload(formData, picuserId),
    fallbackErrorMessage: '拠点更新内容の検証に失敗しました',
  });
}

export async function registerSite(
  formData: SiteFormData,
  assignee?: UserList | null,
): Promise<{ statusCode: string; site: Site }> {
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
      siteId: json?.siteId ?? '',
      siteName: formData.siteName,
      postalCode: formData.postalCode,
      prefectureName: formData.prefectureName,
      cityName: formData.cityName,
      blockName: [formData.townName, formData.blockName].filter(Boolean).join(''),
      address: buildAddress(formData),
      buildingName: formData.addressDetail,
      picFirstName: assignee?.firstName ?? '',
      picLastName: assignee?.lastName ?? (formData.userId ? '' : 'マスター'),
      picEmail: assignee?.email ?? '',
      picPosition: assignee?.position ?? '',
      picPhoneNumber: assignee?.phoneNumber ?? '',
      companyId: '',
      project: [],
      picId: formData.userId ?? '',
      addressDetail: formData.addressDetail,
    },
  };
}

export async function updateSite(
  siteId: string,
  formData: SiteFormData,
  picuserId: number | null = null,
): Promise<{ statusCode: string; site?: Site }> {
  const payload = mapUpdateSitePayload(formData, picuserId);

  const response = await apiFetch(`/site/${encodeURIComponent(siteId)}`, {
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
