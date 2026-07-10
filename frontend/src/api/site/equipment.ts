import { apiFetch, parseJsonResponse } from '@/api/_shared/httpClient';
import type {
  SiteProjectEquipment,
  SiteProjectEquipmentResponse,
} from './types';

export async function getSiteProjectEquipment(
  siteId: string,
  projectId: string,
): Promise<SiteProjectEquipment[]> {
  if (!siteId || !projectId) return [];

  const response = await apiFetch(
    `/site/${encodeURIComponent(siteId)}/${encodeURIComponent(projectId)}/equipment`,
    {
      method: 'GET',
    },
  );

  const { data: json, rawBody } =
    await parseJsonResponse<SiteProjectEquipmentResponse>(response);

  if (!response.ok) {
    throw new Error(
      json?.message ||
        rawBody ||
        `Failed to fetch site project equipment: HTTP ${response.status}`,
    );
  }

  return Array.isArray(json?.equipments) ? json.equipments : [];
}
