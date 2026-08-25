import { ApiError, getApiErrorMessage } from '../_shared/errors';
import { apiFetch, parseJsonResponse } from '../_shared/httpClient';
import type {
  MyProjectParticipation,
  MyProjectParticipationApiItem,
  SiteProject,
  SiteProjectsApiResponse,
} from './types';
import { mapSiteProject } from "./mappers";

export async function getSiteProjects(siteId: string): Promise<SiteProject[]> {
  if (!siteId) return [];

  const response = await apiFetch(`/site/${encodeURIComponent(siteId)}/project`, {
    method: "GET",
  });

  const { data: json, rawBody } = await parseJsonResponse<SiteProjectsApiResponse>(response);

  if (!response.ok) {
    throw new Error(json?.message || rawBody || `Failed to fetch site projects: HTTP ${response.status}`);
  }

  if (!json || !Array.isArray(json.projects)) {
    return [];
  }

  return json.projects.map(mapSiteProject);
}

// GET /site/project/{projectId}/my-participations
// Returns the current user's participation records for one project.
export async function getMyProjectParticipations(
  projectId: string,
): Promise<MyProjectParticipation[]> {
  if (!projectId) return [];

  const response = await apiFetch(
    `/site/project/${encodeURIComponent(projectId)}/my-participations`,
    { method: 'GET' },
  );
  const { data, rawBody } = await parseJsonResponse<
    MyProjectParticipationApiItem[]
  >(response);

  if (!response.ok) {
    throw new ApiError(
      getApiErrorMessage(
        response,
        null,
        rawBody,
        '参加情報の取得に失敗しました',
      ),
      { status: response.status, rawBody },
    );
  }

  if (!Array.isArray(data)) return [];

  return data.map((participation) => ({
    participationId: participation.participationId,
    siteId: String(participation.siteId ?? ''),
    siteName: participation.siteName ?? '',
    status: participation.status ?? '',
    pdfStatus: participation.pdfStatus ?? '',
  }));
}

export { SiteProject };
