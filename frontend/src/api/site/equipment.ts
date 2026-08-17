import {
  apiFetch,
  apiRequest,
  parseJsonResponse,
} from '@/api/_shared/httpClient';
import { ApiError } from '@/api/_shared/errors';
import type {
  SiteProjectEquipment,
  SiteProjectEquipmentApiItem,
  SiteProjectEquipmentApiFile,
  SiteProjectEquipmentDetailResponse,
  SiteEquipmentHistory,
  SiteEquipmentHistoryApiItem,
  SiteEquipmentListResponse,
  SiteProjectEquipmentFileType,
  SiteProjectEquipmentFileUploadResponse,
  SiteProjectEquipmentResponse,
  SiteProjectEquipmentUpdateRequest,
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

function normalizedText(...values: unknown[]): string | null {
  const value = values.find(
    (candidate) =>
      (typeof candidate === 'string' && candidate.trim().length > 0) ||
      typeof candidate === 'number',
  );

  return value == null ? null : String(value).trim();
}

function normalizeSiteProjectEquipmentFile(
  file: SiteProjectEquipmentApiFile,
) {
  return {
    id: typeof file.id === 'number' ? file.id : null,
    fileType: normalizedText(file.fileType, file.FileType, file.file_type),
    fileId: normalizedText(
      file.fileId,
      file.FileId,
      file.file_id,
      file.id,
    ),
    s3Key: normalizedText(file.s3Key, file.S3Key, file.s3key, file.s3_key),
    fileName: normalizedText(
      file.fileName,
      file.FileName,
      file.originalName,
    ),
  };
}

function normalizeSiteProjectEquipment(
  equipment: SiteProjectEquipmentApiItem,
): SiteProjectEquipment {
  const rawFiles = Array.isArray(equipment.files)
    ? equipment.files
    : Array.isArray(equipment.Files)
      ? equipment.Files
      : [];

  return {
    ...equipment,
    equipmentId: equipment.equipmentId ?? equipment.id ?? null,
    powerCondUnitCount:
      equipment.powerCondUnitCount ?? equipment.pcsUnitCount ?? null,
    powerCondStandby:
      equipment.powerCondStandby ?? equipment.pcsStandby ?? null,
    files: rawFiles.map(normalizeSiteProjectEquipmentFile),
  };
}

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

  return Array.isArray(json?.equipments)
    ? json.equipments.map(normalizeSiteProjectEquipment)
    : [];
}

export async function getSiteEquipmentList(
  siteId: string,
): Promise<SiteProjectEquipment[]> {
  if (!siteId) return [];

  const response = await apiFetch(
    `/site/${encodeURIComponent(siteId)}/equipment`,
    { method: 'GET' },
  );
  const { data: json, rawBody } =
    await parseJsonResponse<SiteEquipmentListResponse>(response);

  if (!response.ok) {
    const message =
      !Array.isArray(json) && json?.message
        ? json.message
        : rawBody;
    throw new Error(
      message || `設備一覧の取得に失敗しました: HTTP ${response.status}`,
    );
  }

  const equipments = Array.isArray(json) ? json : json?.equipments;
  return Array.isArray(equipments)
    ? equipments.map(normalizeSiteProjectEquipment)
    : [];
}

export async function getSiteEquipmentById(
  siteId: string,
  equipmentId: number,
): Promise<SiteProjectEquipment> {
  return normalizeSiteProjectEquipment(
    await apiRequest<SiteProjectEquipmentDetailResponse>(
      `/site/${encodeURIComponent(siteId)}/equipment/${encodeURIComponent(String(equipmentId))}`,
      {
        method: 'GET',
        fallbackErrorMessage: '設備情報の取得に失敗しました。',
      },
    ),
  );
}

export async function getSiteEquipmentHistory(
  siteId: string,
  equipmentId: number,
): Promise<SiteEquipmentHistory[]> {
  const history = await apiRequest<SiteEquipmentHistoryApiItem[]>(
    `/site/${encodeURIComponent(siteId)}/equipment/${encodeURIComponent(String(equipmentId))}/history`,
    {
      method: 'GET',
      fallbackErrorMessage: '設備変更履歴の取得に失敗しました。',
    },
  );

  return Array.isArray(history)
    ? history.map((entry) => ({
        id: typeof entry.id === 'number' ? entry.id : null,
        changeDate: normalizedText(entry.changeDate),
        previousEquipmentId:
          typeof entry.previousEquipmentId === 'number'
            ? entry.previousEquipmentId
            : null,
        monitoringStartDate: normalizedText(entry.monitoringStartDate),
        monitoringEndDate: normalizedText(entry.monitoringEndDate),
        isActive:
          typeof entry.isActive === 'boolean' ? entry.isActive : null,
        registeredBy: normalizedText(entry.registeredBy),
        registeredAt: normalizedText(entry.registeredAt),
        equipment: normalizeSiteProjectEquipment(entry.equipment ?? {}),
      }))
    : [];
}

export async function updateSiteEquipment(
  siteId: string,
  equipmentId: number,
  payload: SiteProjectEquipmentUpdateRequest,
): Promise<SiteProjectEquipment> {
  return normalizeSiteProjectEquipment(
    await apiRequest<SiteProjectEquipmentDetailResponse>(
      `/site/${encodeURIComponent(siteId)}/equipment/${encodeURIComponent(String(equipmentId))}`,
      {
        method: 'PATCH',
        body: payload,
        fallbackErrorMessage: '設備情報の変更に失敗しました。',
      },
    ),
  );
}
