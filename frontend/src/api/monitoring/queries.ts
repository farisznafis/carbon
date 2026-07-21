/**
 * GET RECENT MONITORING DATA
 * ===========================
 * Fetch recent monitoring data for a project (and optional site)
 */
import { apiFetch, parseJsonResponse } from "../_shared/httpClient";
import { normalizeRecentMonitoringItem } from "./mappers";
import type {
  MonitoringRecentApiItem,
  MonitoringRecentItem,
  MonitoringRecentResponse,
} from "./types";

export async function getMonitoringRecent(params: {
  projectId: string | number;
  siteId?: string | number;
  limit?: number;
}): Promise<MonitoringRecentItem[]> {
  const qs = new URLSearchParams();
  qs.set("projectId", String(params.projectId));

  if (params.siteId != null && String(params.siteId).trim() !== "") {
    qs.set("siteId", String(params.siteId));
  }

  if (params.limit != null) {
    qs.set("limit", String(params.limit));
  }

  const response = await apiFetch(`/monitoring?${qs.toString()}`, {
    method: "GET",
  });

  const { data: json, rawBody } = await parseJsonResponse<MonitoringRecentResponse>(response);

  if (!response.ok) {
    throw new Error(
      json?.message || rawBody || `Failed to fetch monitoring data: HTTP ${response.status}`,
    );
  }

  const payload = json?.data ?? json;
  const items: MonitoringRecentApiItem[] = Array.isArray(payload)
    ? payload
    : payload && typeof payload === "object"
      ? [payload as MonitoringRecentApiItem]
      : [];

  return items.map(normalizeRecentMonitoringItem);
}
