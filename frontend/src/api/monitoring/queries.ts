/**
 * GET RECENT MONITORING DATA
 * ===========================
 * Fetch recent monitoring data for a project (and optional site)
 */
import { apiFetch, apiRequest, parseJsonResponse } from '../_shared/httpClient';
import {
  normalizeMonitoringRecordListItem,
  normalizeRecentMonitoringItem,
} from "./mappers";
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

function normalizeMonitoringRecordListResult(
  response: MonitoringRecordListResponse | null,
  page: number,
  limit: number,
): MonitoringRecordListResult {
  const items = Array.isArray(response?.items)
    ? response.items.map(normalizeMonitoringRecordListItem)
    : [];
  const responsePagination = response?.pagination;

  return {
    items,
    pagination: {
      page: responsePagination?.page ?? page,
      limit: responsePagination?.limit ?? limit,
      totalItems: responsePagination?.totalItems ?? items.length,
      totalPages: Math.max(responsePagination?.totalPages ?? 1, 1),
      nextPageToken: responsePagination?.nextPageToken ?? null,
      hasMore: responsePagination?.hasMore ?? false,
    },
  };
}

export async function getMonitoringRecent(params: {
  projectId: string;
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
  projectId: string;
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

  return normalizeMonitoringRecordListResult(json, page, limit);
}

export async function getAdminProjectMonitoringRecords(params: {
  projectId: string;
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
    `/monitoring/${encodeURIComponent(String(params.projectId))}/list?${query.toString()}`,
    { method: 'GET' },
  );
  const { data: json, rawBody } =
    await parseJsonResponse<MonitoringRecordListResponse>(response);

  if (!response.ok) {
    throw new Error(
      json?.message ||
        rawBody ||
        `Failed to fetch project monitoring records: HTTP ${response.status}`,
    );
  }

  return normalizeMonitoringRecordListResult(json, page, limit);
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
