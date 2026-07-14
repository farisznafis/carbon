import { apiFetch, parseJsonResponse } from '@/api/_shared/httpClient';
import type {
  EmissionTrendPoint,
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

  if ('processing_date' in response || 'empj' in response) {
    return [response];
  }

  const responseData = response.data;

  if (Array.isArray(responseData)) return responseData;

  if (responseData && 'results' in responseData) {
    return toReductionResultItems(responseData.results);
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

  return toReductionResultItems(response.results ?? response.items);
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

export async function getEmissionTrend(
  siteId: string,
  projectId: string,
): Promise<EmissionTrendPoint[]> {
  if (!siteId || !projectId) return [];

  const response = await apiFetch(
    `/batch/${encodeURIComponent(siteId)}/${encodeURIComponent(projectId)}/reduction-result`,
    { method: 'GET' },
  );
  const { data: json, rawBody } = await parseJsonResponse<
    | ReductionResultResponse
    | ReductionResultApiItem
    | ReductionResultApiItem[]
  >(response);

  if (!response.ok) {
    const message =
      json && !Array.isArray(json) && 'message' in json ? json.message : null;

    throw new Error(
      message ||
        rawBody ||
        `Failed to fetch emission trend: HTTP ${response.status}`,
    );
  }

  if (!json) return [];

  return normalizeEmissionTrendResponse(json);
}
