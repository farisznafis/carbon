import { apiFetch, parseJsonResponse } from '@/api/_shared/httpClient';
import type {
  EmissionTrendPoint,
  ProjectEmissionBreakdown,
  ProjectEmissionSummary,
  ProjectReductionResultResponse,
  ReductionResultApiItem,
  ReductionResultPayload,
  ReductionResultResponse,
} from './types';

function toReductionResultItems(
  payload: ReductionResultPayload | undefined,
): ReductionResultApiItem[] {
  if (!payload) return [];

  return Array.isArray(payload) ? payload : [payload];
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

  const responseData = response.data;

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

  return toReductionResultItems(
    response.result ?? response.results ?? response.items,
  );
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
  const result = 'data' in response ? response.data : response;
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
