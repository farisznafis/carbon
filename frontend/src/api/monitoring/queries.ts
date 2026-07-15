/**
 * GET RECENT MONITORING DATA
 * ===========================
 * Fetch recent monitoring data for a project (and optional site)
 */
import { apiFetch, apiRequest, parseJsonResponse } from '../_shared/httpClient';
import { normalizeRecentMonitoringItem } from "./mappers";
import type {
  MonitoringCancelRequestListItem,
  MonitoringCancelRequestListResponse,
  MonitoringRecordListResponse,
  MonitoringRecordListResult,
  MonitoringRecentApiItem,
  MonitoringRecentItem,
  MonitoringRecentResponse,
} from "./types";

const DEFAULT_MONITORING_LIST_PAGE = 1;
const DEFAULT_MONITORING_LIST_LIMIT = 10;

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

export async function getMonitoringRecords(params: {
  siteId: string | number;
  projectId: string | number;
  page?: number;
  limit?: number;
}): Promise<MonitoringRecordListResult> {
  const page = params.page ?? DEFAULT_MONITORING_LIST_PAGE;
  const limit = params.limit ?? DEFAULT_MONITORING_LIST_LIMIT;
  const query = new URLSearchParams({
    page: String(page),
    limit: String(limit),
  });

  const response = await apiFetch(
    `/monitoring/${encodeURIComponent(params.siteId)}/${encodeURIComponent(params.projectId)}/list?${query.toString()}`,
    { method: "GET" },
  );
  const { data: json, rawBody } =
    await parseJsonResponse<MonitoringRecordListResponse>(response);

  if (!response.ok) {
    throw new Error(
      json?.message ||
        rawBody ||
        `Failed to fetch monitoring records: HTTP ${response.status}`,
    );
  }

  const responsePagination = json?.pagination;

  return {
    items: Array.isArray(json?.items) ? json.items : [],
    pagination: {
      page: responsePagination?.page ?? page,
      limit: responsePagination?.limit ?? limit,
      totalItems: responsePagination?.totalItems ?? json?.items?.length ?? 0,
      totalPages: Math.max(responsePagination?.totalPages ?? 1, 1),
      nextPageToken: responsePagination?.nextPageToken ?? null,
      hasMore: responsePagination?.hasMore ?? false,
    },
  };
}

export async function getMonitoringCancelRequests(): Promise<
  MonitoringCancelRequestListItem[]
> {
  const response = await apiRequest<MonitoringCancelRequestListResponse>(
    '/monitoring/cancel-requests',
    {
      method: 'GET',
      fallbackErrorMessage: 'Failed to fetch monitoring cancel requests',
    },
  );

  return Array.isArray(response?.items) ? response.items : [];
}
