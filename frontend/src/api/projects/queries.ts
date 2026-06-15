/**
 * Projects API Module
 * ====================
 * Handles project-related read operations.
 */

import { apiFetch, parseJsonResponse } from '../_shared/httpClient';
import { mapProjectListItem } from './mappers';
import type { Project, ProjectDetail, ProjectDetailResponse, ProjectListResponse } from './types';

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
