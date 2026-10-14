import { apiFetch, parseJsonResponse } from '@/api/_shared/httpClient';
import type {
  BatchHistoryResponse,
  BatchHistoryResult,
  EmissionTrendPoint,
  ProjectEmissionBreakdown,
  ProjectEmissionHistoryPoint,
  ProjectMonitoringChartItem,
  ProjectMonitoringResponse,
  ProjectEmissionSummary,
  ProjectReductionResultResponse,
  ReductionResultApiItem,
  ReductionResultItem,
  ReductionResultPayload,
  ReductionResultResponse,
} from './types';

const DEFAULT_BATCH_HISTORY_PAGE = 1;
const DEFAULT_BATCH_HISTORY_LIMIT = 10;

export async function getBatchHistory(
  page = DEFAULT_BATCH_HISTORY_PAGE,
  limit = DEFAULT_BATCH_HISTORY_LIMIT,
): Promise<BatchHistoryResult> {
  const query = new URLSearchParams({
    page: String(page),
    limit: String(limit),
  });
  const response = await apiFetch(`/batch/history?${query.toString()}`, {
    method: 'GET',
  });
  const { data: json, rawBody } =
    await parseJsonResponse<BatchHistoryResponse>(response);

  if (!response.ok) {
    throw new Error(
      rawBody || `Failed to fetch batch history: HTTP ${response.status}`,
    );
  }

  const items = Array.isArray(json?.items) ? json.items : [];
  const pagination = json?.pagination;

  return {
    items,
    pagination: {
      page: pagination?.page ?? page,
      limit: pagination?.limit ?? limit,
      totalItems: pagination?.totalItems ?? items.length,
      totalPages: Math.max(pagination?.totalPages ?? 1, 1),
      nextPageToken: pagination?.nextPageToken ?? null,
      hasMore: pagination?.hasMore ?? false,
    },
  };
}

function toReductionResultItems(
  payload: ReductionResultPayload | undefined,
): ReductionResultApiItem[] {
  if (!payload) return [];

  return Array.isArray(payload) ? payload : [payload];
}

function isProjectReductionResultResponse(
  response: ProjectReductionResultResponse | ReductionResultApiItem,
): response is ProjectReductionResultResponse {
  return (
    'data' in response ||
    'statusCode' in response ||
    'message' in response
  );
}

function extractReductionResults(
  response:
    | ReductionResultResponse
    | ReductionResultApiItem
    | ReductionResultApiItem[],
): ReductionResultApiItem[] {
  if (Array.isArray(response)) return response;

  if (
    'processing_date' in response ||
    'empj' in response ||
    'empj_m' in response ||
    'empj_s_control' in response ||
    'empj_s_battery' in response
  ) {
    return [response];
  }

  const responseData = 'data' in response ? response.data : undefined;

  if (Array.isArray(responseData)) return responseData;

  if (responseData && 'results' in responseData) {
    return toReductionResultItems(responseData.results);
  }

  if (responseData && 'result' in responseData) {
    return toReductionResultItems(responseData.result);
  }

  if (responseData && 'items' in responseData) {
    return toReductionResultItems(responseData.items);
  }

  if (
    responseData &&
    ('processing_date' in responseData || 'created_at' in responseData)
  ) {
    return [responseData];
  }

  const rootResult = 'result' in response ? response.result : undefined;
  const rootResults = 'results' in response ? response.results : undefined;
  const rootItems = 'items' in response ? response.items : undefined;

  return toReductionResultItems(rootResult ?? rootResults ?? rootItems);
}

function mapEmissionTrendPoint(
  result: ReductionResultApiItem,
): EmissionTrendPoint | null {
  const processingDate =
    result.processing_date?.trim() || result.created_at?.trim();
  if (result.empj == null || result.empj === '') return null;

  const emission =
    typeof result.empj === 'number' ? result.empj : Number(result.empj);

  if (!processingDate || !Number.isFinite(emission)) return null;

  return {
    x: processingDate,
    y: emission,
  };
}

export function normalizeEmissionTrendResponse(
  response:
    | ReductionResultResponse
    | ReductionResultApiItem
    | ReductionResultApiItem[],
): EmissionTrendPoint[] {
  return extractReductionResults(response)
    .flatMap((result) => {
      const trendPoint = mapEmissionTrendPoint(result);
      return trendPoint ? [trendPoint] : [];
    })
    .sort((firstPoint, secondPoint) => firstPoint.x.localeCompare(secondPoint.x));
}

type ReductionResultJson =
  | ReductionResultResponse
  | ReductionResultApiItem
  | ReductionResultApiItem[];

async function fetchReductionResults(
  siteId: string,
  projectId: string,
): Promise<ReductionResultJson | null> {
  if (!siteId || !projectId) return null;

  const response = await apiFetch(
    `/batch/${encodeURIComponent(siteId)}/${encodeURIComponent(projectId)}/reduction-result`,
    { method: 'GET' },
  );
  const { data: json, rawBody } =
    await parseJsonResponse<ReductionResultJson>(response);

  if (!response.ok) {
    const message =
      json && !Array.isArray(json) && 'message' in json ? json.message : null;

    throw new Error(
      message ||
        rawBody ||
        `Failed to fetch project reduction results: HTTP ${response.status}`,
    );
  }

  return json;
}

export async function getEmissionTrend(
  siteId: string,
  projectId: string,
): Promise<EmissionTrendPoint[]> {
  const json = await fetchReductionResults(siteId, projectId);
  return json ? normalizeEmissionTrendResponse(json) : [];
}

function toFiniteEmission(value: number | string | null | undefined) {
  if (value == null || value === '') return null;

  const emission = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(emission) ? emission : null;
}

/** Loads reduction results with their EmPJ breakdown, oldest first. */
export async function getReductionResults(
  siteId: string,
  projectId: string,
): Promise<ReductionResultItem[]> {
  const json = await fetchReductionResults(siteId, projectId);
  if (!json) return [];

  return extractReductionResults(json)
    .flatMap((result) => {
      const processingDate =
        result.processing_date?.trim() || result.created_at?.trim();
      return processingDate
        ? [{
            processingDate,
            empj: toFiniteEmission(result.empj),
            empjM: toFiniteEmission(result.empj_m),
            empjSControl: toFiniteEmission(result.empj_s_control),
            empjSBattery: toFiniteEmission(result.empj_s_battery),
          }]
        : [];
    })
    .sort((left, right) => left.processingDate.localeCompare(right.processingDate));
}

/**
 * Maps the yearly reduction chart into a chronological history. The backend
 * emits one entry per processing record, so a year that appears twice keeps
 * its latest entry.
 */
function toProjectEmissionHistory(
  chartData: readonly ProjectMonitoringChartItem[],
): ProjectEmissionHistoryPoint[] {
  const pointsByPeriod = new Map<string, ProjectEmissionHistoryPoint>();

  chartData.forEach((item) => {
    const period = item.year?.trim();
    if (!period) return;

    pointsByPeriod.set(period, {
      period,
      baselineEmission: toFiniteEmission(item.baseline),
      projectEmission: toFiniteEmission(item.actualEmission),
      reduction: toFiniteEmission(item.reduction),
    });
  });

  return [...pointsByPeriod.values()].sort((left, right) =>
    left.period.localeCompare(right.period),
  );
}

/** Loads a project's yearly emission history from GET /project/{projectId}/monitoring. */
export async function getProjectEmissionHistory(
  projectId: string,
): Promise<ProjectEmissionHistoryPoint[]> {
  if (!projectId) return [];

  const response = await apiFetch(
    `/project/${encodeURIComponent(projectId)}/monitoring`,
    { method: 'GET' },
  );
  const { data: json, rawBody } =
    await parseJsonResponse<ProjectMonitoringResponse>(response);

  if (!response.ok) {
    throw new Error(
      json?.message ||
        rawBody ||
        `Failed to fetch project monitoring: HTTP ${response.status}`,
    );
  }

  return toProjectEmissionHistory(json?.detail?.chart?.chartData ?? []);
}

function sumEmissionField(
  results: ReductionResultApiItem[],
  field: 'empj_m' | 'empj_s_control' | 'empj_s_battery',
) {
  let total = 0;
  let hasValidValue = false;

  for (const result of results) {
    const emission = toFiniteEmission(result[field]);
    if (emission === null) continue;

    total += emission;
    hasValidValue = true;
  }

  return hasValidValue ? total : null;
}

export function normalizeProjectEmissionBreakdownResponse(
  response:
    | ReductionResultResponse
    | ReductionResultApiItem
    | ReductionResultApiItem[],
): ProjectEmissionBreakdown | null {
  const results = extractReductionResults(response);
  if (results.length === 0) return null;

  const breakdown: ProjectEmissionBreakdown = {
    mainEmission: sumEmissionField(results, 'empj_m'),
    controlDeviceEmission: sumEmissionField(results, 'empj_s_control'),
    batteryEmission: sumEmissionField(results, 'empj_s_battery'),
  };

  return Object.values(breakdown).some((emission) => emission !== null)
    ? breakdown
    : null;
}

export function normalizeProjectEmissionSummaryResponse(
  response: ProjectReductionResultResponse | ReductionResultApiItem,
): ProjectEmissionSummary | null {
  const result = isProjectReductionResultResponse(response)
    ? response.data
    : response;
  if (!result) return null;

  const baselineEmission = toFiniteEmission(result.embl);
  const projectEmission = toFiniteEmission(result.empj);
  const breakdown = normalizeProjectEmissionBreakdownResponse(result);
  const hasSummary =
    baselineEmission !== null ||
    projectEmission !== null ||
    breakdown !== null;

  return hasSummary
    ? { baselineEmission, projectEmission, breakdown }
    : null;
}

export async function getProjectEmissionSummary(
  projectId: string,
): Promise<ProjectEmissionSummary | null> {
  if (!projectId) return null;

  const response = await apiFetch(
    `/batch/${encodeURIComponent(projectId)}/reduction-result-total`,
    { method: 'GET' },
  );
  const { data: json, rawBody } =
    await parseJsonResponse<ProjectReductionResultResponse>(response);

  if (!response.ok) {
    throw new Error(
      json?.message ||
        rawBody ||
        `Failed to fetch project reduction result total: HTTP ${response.status}`,
    );
  }

  return json ? normalizeProjectEmissionSummaryResponse(json) : null;
}
