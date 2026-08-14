import { ApiError, getApiErrorMessage } from '../_shared/errors';
import { apiFetch, parseJsonResponse } from '../_shared/httpClient';
import type { ApiErrorPayload } from '../_shared/types';
import type {
  MembershipPdfDownload,
  SiteProject,
  SiteProjectsApiResponse,
} from './types';
import { mapSiteProject } from "./mappers";

const DEFAULT_MEMBERSHIP_PDF_BASENAME = 'membership';

function decodeFileName(value: string): string {
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}

function getContentDispositionFileName(headerValue: string | null): string | null {
  if (!headerValue) return null;

  const encodedMatch = headerValue.match(/filename\*=UTF-8''([^;]+)/i);
  if (encodedMatch?.[1]) {
    return decodeFileName(encodedMatch[1].trim().replace(/^"|"$/g, ''));
  }

  const plainMatch = headerValue.match(/filename="?([^";]+)"?/i);
  return plainMatch?.[1]?.trim() || null;
}

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

export async function downloadProjectMembershipPdf(
  projectId: string,
  participationId: number,
): Promise<MembershipPdfDownload> {
  const response = await apiFetch(
    `/project/${encodeURIComponent(projectId)}/participations/${encodeURIComponent(String(participationId))}/membership-pdf`,
    { method: 'GET' },
  );

  if (!response.ok) {
    const { data, rawBody } = await parseJsonResponse<ApiErrorPayload>(response);
    throw new ApiError(
      getApiErrorMessage(
        response,
        data,
        rawBody,
        'PDFの取得に失敗しました',
      ),
      {
        status: response.status,
        payload: data,
        rawBody,
      },
    );
  }

  return {
    blob: await response.blob(),
    fileName:
      getContentDispositionFileName(response.headers.get('Content-Disposition')) ??
      `${DEFAULT_MEMBERSHIP_PDF_BASENAME}-${projectId}.pdf`,
  };
}

export { SiteProject };
