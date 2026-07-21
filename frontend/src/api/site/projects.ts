import { apiFetch, parseJsonResponse } from "../_shared/httpClient";
import type { SiteProject, SiteProjectsApiResponse } from "./types";
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

export { SiteProject };
