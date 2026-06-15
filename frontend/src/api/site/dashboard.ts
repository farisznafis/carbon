import type { SiteDashboardData, SiteDashboardApiResponse } from "./types";
import { apiFetch, parseJsonResponse } from "@/api/_shared/httpClient";
import { mapSiteDashboard } from "./mappers";

export const getSiteData = async (siteId: string): Promise<SiteDashboardData> => {
  if (!siteId) {
    return { site: null, metrics: null };
  }

  const response = await apiFetch(`/site/${encodeURIComponent(siteId)}`, {
    method: "GET",
  });

  const { data: json, rawBody } = await parseJsonResponse<SiteDashboardApiResponse>(response);

  if (!response.ok) {
    if (response.status === 404) {
      return { site: null, metrics: null };
    }
    throw new Error(json?.message || rawBody || `Failed to fetch site data: HTTP ${response.status}`);
  }

  const apiSite = json?.site;
  if (!apiSite) {
    return { site: null, metrics: null };
  }

  return {
    site: mapSiteDashboard(apiSite),
    metrics: apiSite.data ?? null,
  };
};
