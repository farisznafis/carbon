import {
  apiFetch,
  apiRequest,
  parseJsonResponse,
} from '@/api/_shared/httpClient';
import { ApiError } from '@/api/_shared/errors';
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
  SiteProjectJoinSustainabilityCommitmentRequest,
  SiteProjectJoinSustainabilityCommitmentResponse,
  SiteProjectFinalAgreementValidationRequest,
  SiteProjectFinalAgreementValidationResponse,
  SiteProjectJoinRequest,
  SiteProjectJoinResponse,
} from './types';

export async function uploadSiteProjectEquipmentFile(
  siteId: string,
  fileType: SiteProjectEquipmentFileType,
  file: File,
): Promise<SiteProjectEquipmentValidationFile> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await apiFetch(
    `/site/project/sites/${encodeURIComponent(siteId)}/files/upload?fileType=${encodeURIComponent(fileType)}`,
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

export async function getSiteProjectEquipmentPresignedUrl(
  s3Key: string,
): Promise<string> {
  const normalizedS3Key = s3Key.trim();
  if (!normalizedS3Key) {
    throw new Error('ファイルキーが指定されていません。');
  }

  const response = await apiFetch(
    `/site/project/presigned-url?s3Key=${encodeURIComponent(normalizedS3Key)}`,
    { method: 'GET' },
  );
  const { data, rawBody } = await parseJsonResponse<unknown>(response);

  if (!response.ok) {
    const responseMessage =
      data !== null &&
      typeof data === 'object' &&
      !Array.isArray(data) &&
      typeof (data as Record<string, unknown>).message === 'string'
        ? String((data as Record<string, unknown>).message)
        : '';

    throw new ApiError(
      responseMessage ||
        rawBody ||
        `ファイルURLの取得に失敗しました: HTTP ${response.status}`,
      {
        status: response.status,
        payload: data,
        rawBody,
      },
    );
  }

  const presignedUrl =
    typeof data === 'string' ? data.trim() : rawBody.trim();

  if (!presignedUrl) {
    throw new Error('ファイルURLを取得できませんでした。');
  }

  let parsedUrl: URL;
  try {
    parsedUrl = new URL(presignedUrl);
  } catch {
    throw new Error('取得したファイルURLが不正です。');
  }

  if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
    throw new Error('取得したファイルURLが不正です。');
  }

  return parsedUrl.toString();
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

export async function validateSiteProjectSustainabilityCommitment(
  payload: SiteProjectJoinSustainabilityCommitmentRequest,
): Promise<SiteProjectJoinSustainabilityCommitmentResponse> {
  return apiRequest<SiteProjectJoinSustainabilityCommitmentResponse>(
    '/site/project/sustainability-commitment/validate',
    {
      method: 'POST',
      body: payload,
      fallbackErrorMessage: '持続可能性約束の検証に失敗しました。',
    },
  );
}

export async function validateSiteProjectFinalAgreement(
  payload: SiteProjectFinalAgreementValidationRequest,
): Promise<SiteProjectFinalAgreementValidationResponse> {
  return apiRequest<SiteProjectFinalAgreementValidationResponse>(
    '/site/project/final/validate',
    {
      method: 'POST',
      body: payload,
      fallbackErrorMessage: '入会規約の検証に失敗しました。',
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
