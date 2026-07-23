import {
  apiFetch,
  apiRequest,
  parseJsonResponse,
} from '@/api/_shared/httpClient';
import type {
  SiteProjectEquipment,
  SiteProjectEquipmentFileType,
  SiteProjectEquipmentFileUploadResponse,
  SiteProjectEquipmentResponse,
  SiteProjectEquipmentValidationFile,
  SiteProjectEquipmentValidationItem,
  SiteProjectEquipmentValidationResponse,
  SiteProjectConfirmationItemsValidationRequest,
  SiteProjectConfirmationItemsValidationResponse,
  SiteProjectJoinRequest,
  SiteProjectJoinResponse,
} from './types';

export async function uploadSiteProjectEquipmentFile(
  siteId: string,
  projectId: string,
  fileType: SiteProjectEquipmentFileType,
  file: File,
): Promise<SiteProjectEquipmentValidationFile> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await apiFetch(
    `/site/project/${encodeURIComponent(siteId)}/${encodeURIComponent(projectId)}/files/upload?fileType=${encodeURIComponent(fileType)}`,
    {
      method: 'POST',
      body: formData,
    },
  );
  const { data: json, rawBody } =
    await parseJsonResponse<SiteProjectEquipmentFileUploadResponse>(response);

  if (!response.ok) {
    throw new Error(
      rawBody || `設備ファイルのアップロードに失敗しました: HTTP ${response.status}`,
    );
  }

  if (!json?.s3Key || !json.fileId) {
    throw new Error('アップロードしたファイル情報を取得できませんでした。');
  }

  return {
    fileType,
    s3Key: json.s3Key,
    fileName: json.originalName || file.name,
    fileId: json.fileId,
  };
}

export async function validateSiteProjectEquipment(
  equipment: SiteProjectEquipmentValidationItem,
): Promise<SiteProjectEquipmentValidationResponse> {
  return apiRequest<SiteProjectEquipmentValidationResponse>(
    '/site/project/equipment/validate',
    {
      method: 'POST',
      body: equipment,
      fallbackErrorMessage: '設備情報の検証に失敗しました。',
    },
  );
}

export async function validateSiteProjectConfirmationItems(
  payload: SiteProjectConfirmationItemsValidationRequest,
): Promise<SiteProjectConfirmationItemsValidationResponse> {
  return apiRequest<SiteProjectConfirmationItemsValidationResponse>(
    '/site/project/confirmation-items/validate',
    {
      method: 'POST',
      body: payload,
      fallbackErrorMessage: '同意事項の検証に失敗しました。',
    },
  );
}

export async function joinSiteProject(
  projectId: string,
  payload: SiteProjectJoinRequest,
): Promise<SiteProjectJoinResponse> {
  return apiRequest<SiteProjectJoinResponse>(
    `/site/project/projects/${encodeURIComponent(projectId)}/join`,
    {
      method: 'POST',
      body: payload,
      fallbackErrorMessage: 'プロジェクト参加申請に失敗しました。',
    },
  );
}

export async function getSiteProjectEquipment(
  siteId: string,
  projectId: string,
): Promise<SiteProjectEquipment[]> {
  if (!siteId || !projectId) return [];

  const response = await apiFetch(
    `/site/${encodeURIComponent(siteId)}/${encodeURIComponent(projectId)}/equipment`,
    {
      method: 'GET',
    },
  );

  const { data: json, rawBody } =
    await parseJsonResponse<SiteProjectEquipmentResponse>(response);

  if (!response.ok) {
    throw new Error(
      json?.message ||
        rawBody ||
        `Failed to fetch site project equipment: HTTP ${response.status}`,
    );
  }

  return Array.isArray(json?.equipments) ? json.equipments : [];
}
