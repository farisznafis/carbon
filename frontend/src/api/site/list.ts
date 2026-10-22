import { apiFetch, parseJsonResponse } from "@/api/_shared/httpClient";
import type { Site, SitesResponse } from "./types";
import { mapSiteListItem } from "./mappers";

function resolveCompanyId(explicitCompanyId?: string): string {
  if (explicitCompanyId && explicitCompanyId !== "null" && explicitCompanyId !== "undefined") {
    return explicitCompanyId;
  }

  const storedUser = localStorage.getItem("user");
  if (!storedUser) {
    return "";
  }

  try {
    const parsedUser = JSON.parse(storedUser);
    const companyId = parsedUser?.companyId;
    return companyId && companyId !== "null" && companyId !== "undefined"
      ? String(companyId)
      : "";
  } catch {
    return "";
  }
}



export const getSites = async (companyId?: string): Promise<{ sites: Site[] }> => {
  const resolvedCompanyId = resolveCompanyId(companyId);

  if (!resolvedCompanyId) {
    throw new Error("companyId is required to fetch sites");
  }

  const response = await apiFetch(
    `/site?companyId=${encodeURIComponent(resolvedCompanyId)}`,
    {
      method: "GET",
    },
  );

  const { data: json, rawBody } = await parseJsonResponse<SitesResponse>(response);

  if (!response.ok) {
    // 拠点が1件もない会社では API が 404 を返すため、エラーではなく空の一覧として扱う
    if (response.status === 404) {
      return { sites: [] };
    }
    throw new Error(json?.message || rawBody || `Failed to fetch sites: HTTP ${response.status}`);
  }

  if (!json || !Array.isArray(json.siteList)) {
    throw new Error(json?.message || "Invalid sites response");
  }

  return {
    sites: json.siteList.map(mapSiteListItem),
  };
};
