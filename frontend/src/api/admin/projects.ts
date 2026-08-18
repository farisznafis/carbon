import { apiFetch, parseJsonResponse } from '@/api/_shared/httpClient';
import { ApiError } from '@/api/_shared/errors';
import type {
  AdminMutationResponse,
  AdminProjectDetail,
  AdminProjectDetailResponse,
  CreateAdminProjectResponse,
  CreateAdminProjectPayload,
  ProjectPlanAutofillResponse,
  ProjectPlanApplicationResponse,
  ProjectPlanBatchProgressResponse,
  ProjectPlanFormResponse,
  ProjectPlanProgressSummary,
  ProjectPlanProgressResponse,
  ProjectStatus,
  UpdateProjectStatusPayload,
  UpdateProjectStatusResponse,
  UpdateAdminProjectPayload,
} from './types';

export type {
  AdminProjectDetail,
  AdminProjectDetailResponse,
  CreateAdminProjectResponse,
  CreateAdminProjectPayload,
  ProjectPlanAutofillResponse,
  ProjectPlanApplicationResponse,
  ProjectPlanFormProgress,
  ProjectPlanBatchProgressResponse,
  ProjectPlanFormResponse,
  ProjectPlanProgressSummary,
  ProjectPlanFormStatus,
  ProjectPlanProgressResponse,
  ProjectStatus,
  UpdateProjectStatusPayload,
  UpdateProjectStatusResponse,
  UpdateAdminProjectPayload,
} from './types';

export async function getAdminProjectDetail(
  projectId: string,
): Promise<AdminProjectDetail> {
  const response = await apiFetch(`/project/${encodeURIComponent(projectId)}`, {
    method: 'GET',
  });
  const { data: json, rawBody } = await parseJsonResponse<AdminProjectDetailResponse>(response);

  if (!response.ok) {
    const message =
      json?.message ||
      rawBody ||
      `Failed to load admin project detail: HTTP ${response.status}`;
    throw new ApiError(message, {
      status: response.status,
      payload: json,
      rawBody,
    });
  }

  if (!json?.project) {
    throw new Error('Project not found');
  }

  return json.project;
}

export async function createAdminProject(
  payload: CreateAdminProjectPayload,
): Promise<CreateAdminProjectResponse['project']> {
  const response = await apiFetch('/project', {
    method: 'POST',
    body: payload,
  });
  const { data: json, rawBody } = await parseJsonResponse<CreateAdminProjectResponse>(response);

  if (!response.ok) {
    const message = json?.message || rawBody || `Failed to create project: HTTP ${response.status}`;
    throw new ApiError(message, {
      status: response.status,
      payload: json,
      rawBody,
    });
  }

  return json?.project;
}

export async function updateAdminProject(
  projectId: string,
  payload: UpdateAdminProjectPayload,
): Promise<void> {
  const response = await apiFetch(`/project/${encodeURIComponent(projectId)}`, {
    method: 'PATCH',
    body: payload,
  });
  const { data: json, rawBody } = await parseJsonResponse<AdminMutationResponse>(response);

  if (!response.ok) {
    const message = json?.message || rawBody || `Failed to update project: HTTP ${response.status}`;
    throw new ApiError(message, {
      status: response.status,
      payload: json,
      rawBody,
    });
  }
}

export async function updateAdminProjectStatus(
  projectId: string,
  payload: UpdateProjectStatusPayload,
): Promise<UpdateProjectStatusResponse> {
  const response = await apiFetch(
    `/project/${encodeURIComponent(projectId)}/status`,
    {
      method: 'PATCH',
      body: payload,
    },
  );
  const { data: json, rawBody } = await parseJsonResponse<
    UpdateProjectStatusResponse & AdminMutationResponse
  >(response);

  if (!response.ok) {
    const message = json?.message || rawBody || `Failed to update project status: HTTP ${response.status}`;
    throw new ApiError(message, {
      status: response.status,
      payload: json,
      rawBody,
    });
  }

  if (!json) {
    throw new Error('Project status update response is empty');
  }

  return json;
}

export async function getAdminProjectPlanApplication(
  projectId: string,
): Promise<ProjectPlanApplicationResponse> {
  const response = await apiFetch(
    `/projectplan/${encodeURIComponent(projectId)}/application`,
    {
      method: 'GET',
    },
  );
  const { data: json, rawBody } = await parseJsonResponse<ProjectPlanApplicationResponse>(response);

  if (!response.ok) {
    const message =
      json?.message ||
      rawBody ||
      `Failed to load project plan application: HTTP ${response.status}`;
    throw new ApiError(message, {
      status: response.status,
      payload: json,
      rawBody,
    });
  }

  return json ?? {};
}

export async function getProjectPlanAutofillByProject(
  projectId: string,
): Promise<ProjectPlanAutofillResponse> {
  const response = await apiFetch(
    `/projectplan/autofill/${encodeURIComponent(projectId)}`,
    {
      method: 'GET',
    },
  );
  const { data: json, rawBody } =
    await parseJsonResponse<ProjectPlanAutofillResponse>(response);

  if (!response.ok) {
    const message =
      json?.message ||
      rawBody ||
      `Failed to load project autofill data: HTTP ${response.status}`;
    throw new ApiError(message, {
      status: response.status,
      payload: json,
      rawBody,
    });
  }

  return json ?? {};
}

export async function getProjectPlanAutofillDefaults(): Promise<ProjectPlanAutofillResponse> {
  const response = await apiFetch('/projectplan/autofill', {
    method: 'GET',
  });
  const { data: json, rawBody } =
    await parseJsonResponse<ProjectPlanAutofillResponse>(response);

  if (!response.ok) {
    const message =
      json?.message ||
      rawBody ||
      `Failed to load default autofill data: HTTP ${response.status}`;
    throw new ApiError(message, {
      status: response.status,
      payload: json,
      rawBody,
    });
  }

  return json ?? {};
}

export async function createAdminProjectPlanApplication<TPayload extends object>(
  projectId: string,
  payload: TPayload,
): Promise<void> {
  const response = await apiFetch(
    `/projectplan/${encodeURIComponent(projectId)}/application`,
    {
      method: 'POST',
      body: payload,
    },
  );
  const { data: json, rawBody } = await parseJsonResponse<AdminMutationResponse>(response);

  if (!response.ok) {
    const message =
      json?.message ||
      rawBody ||
      `Failed to create project plan application: HTTP ${response.status}`;
    throw new ApiError(message, {
      status: response.status,
      payload: json,
      rawBody,
    });
  }
}

// --- Registry-driven project plan pages (staging) ---
//
// One pair of endpoints serves every page. A page is persisted only when it
// validates in full, so the progress endpoint can say exactly where an
// operator who stopped partway should resume.

function projectPlanFormPath(projectId: string, formSlug: string, methodology?: number) {
  const base = `/projectplan/${encodeURIComponent(projectId)}/forms/${encodeURIComponent(formSlug)}`;
  return methodology === undefined ? base : `${base}?methodology=${methodology}`;
}

export async function getProjectPlanForm(
  projectId: string,
  formSlug: string,
  methodology?: number,
): Promise<ProjectPlanFormResponse> {
  const response = await apiFetch(projectPlanFormPath(projectId, formSlug, methodology), {
    method: 'GET',
  });
  const { data: json, rawBody } = await parseJsonResponse<ProjectPlanFormResponse>(response);

  if (!response.ok) {
    const message =
      json?.message || rawBody || `Failed to load project plan form: HTTP ${response.status}`;
    throw new ApiError(message, {
      status: response.status,
      payload: json,
      rawBody,
    });
  }

  return json ?? {};
}

/**
 * Saves one page. Rejects with an ApiError carrying field errors when the page
 * is incomplete — nothing is stored in that case.
 */
export async function saveProjectPlanForm<TPayload extends object>(
  projectId: string,
  formSlug: string,
  payload: TPayload,
  methodology?: number,
): Promise<ProjectPlanFormResponse> {
  const response = await apiFetch(projectPlanFormPath(projectId, formSlug, methodology), {
    method: 'PUT',
    body: payload,
  });
  const { data: json, rawBody } = await parseJsonResponse<ProjectPlanFormResponse>(response);

  if (!response.ok) {
    const message =
      json?.message || rawBody || `Failed to save project plan form: HTTP ${response.status}`;
    throw new ApiError(message, {
      status: response.status,
      payload: json,
      rawBody,
    });
  }

  return json ?? {};
}

export async function getProjectPlanProgress(
  projectId: string,
): Promise<ProjectPlanProgressResponse> {
  const response = await apiFetch(
    `/projectplan/${encodeURIComponent(projectId)}/progress`,
    {
      method: 'GET',
    },
  );
  const { data: json, rawBody } = await parseJsonResponse<ProjectPlanProgressResponse>(response);

  if (!response.ok) {
    const message =
      json?.message || rawBody || `Failed to load project plan progress: HTTP ${response.status}`;
    throw new ApiError(message, {
      status: response.status,
      payload: json,
      rawBody,
    });
  }

  return json ?? {};
}

/**
 * Plan progress for several projects in one request.
 *
 * Used by list views: asking per row would be one call per project. Returns an
 * empty map for an empty input rather than calling the API.
 */
export async function getProjectPlanProgressBatch(
  projectIds: string[],
): Promise<Map<string, ProjectPlanProgressSummary>> {
  const ids = projectIds.filter(Boolean);
  if (ids.length === 0) return new Map();

  const query = ids.map((id) => encodeURIComponent(id)).join(',');
  const response = await apiFetch(`/projectplan/progress?projectIds=${query}`, {
    method: 'GET',
  });
  const { data: json, rawBody } =
    await parseJsonResponse<ProjectPlanBatchProgressResponse>(response);

  if (!response.ok) {
    const message =
      json?.message || rawBody || `Failed to load plan progress: HTTP ${response.status}`;
    throw new ApiError(message, { status: response.status, payload: json, rawBody });
  }

  return new Map((json?.projects ?? []).map((p) => [p.projectId, p]));
}
