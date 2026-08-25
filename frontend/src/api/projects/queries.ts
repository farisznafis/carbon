/**
 * Projects API Module
 * ====================
 * Handles project-related read operations.
 */

import { ApiError, getApiErrorMessage } from '../_shared/errors';
import { apiFetch, parseJsonResponse } from '../_shared/httpClient';
import type { ApiErrorPayload } from '../_shared/types';
import { mapProjectListItem } from './mappers';
import type {
  Project,
  ProjectDetail,
  ProjectDetailResponse,
  ProjectListResponse,
  ProjectMembershipPdfDownload,
} from './types';

// GET /project/{projectId}/participations/{participationId}/membership-pdf
/** Requests the temporary membership PDF URL shared by master and admin views. */
export async function downloadProjectMembershipPdf(
  projectId: string,
  participationId: number,
): Promise<ProjectMembershipPdfDownload> {
  const response = await apiFetch(
    `/project/${encodeURIComponent(projectId)}/participations/${encodeURIComponent(String(participationId))}/membership-pdf`,
    { method: 'GET' },
  );
  const { data, rawBody } = await parseJsonResponse<
    ProjectMembershipPdfDownload & ApiErrorPayload
  >(response);

  if (!response.ok) {
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

  if (!data?.pdfUrl) {
    throw new Error('PDFのダウンロードURLが見つかりませんでした');
  }

  return { pdfUrl: data.pdfUrl, expiresIn: data.expiresIn ?? 0 };
}

export async function getProjectDetails(
  projectId: string,
): Promise<ProjectDetail> {
  const response = await apiFetch(`/project/${encodeURIComponent(projectId)}`, {
    method: 'GET',
  });
  const { data: json, rawBody } = await parseJsonResponse<ProjectDetailResponse>(response);

  if (!response.ok) {
    throw new Error(json?.message || rawBody || 'Failed to load project details');
  }

  if (!json?.project) {
    throw new Error('Project not found');
  }

  return json.project;
}

export async function getProjects(): Promise<Project[]> {
  const response = await apiFetch('/project', {
    method: 'GET',
  });

  const { data: json, rawBody } = await parseJsonResponse<ProjectListResponse>(response);

  if (!response.ok) {
    throw new Error(json?.message || rawBody || 'Failed to load projects');
  }

  if (!json || !Array.isArray(json.projects)) {
    throw new Error('Projects not found');
  }

  return json.projects.map(mapProjectListItem);
}
