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
