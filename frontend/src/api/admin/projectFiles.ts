import { apiFetch, parseJsonResponse } from '@/api/_shared/httpClient';
import { ApiError } from '@/api/_shared/errors';

/**
 * Files attached to a project: the forms we write (プロジェクト計画書,
 * モニタリング報告書, 個別活動実績報告リスト, 追加資料) and the documents the
 * VVB and the J-Credit secretariat send back, which we upload too.
 *
 * Each is kept as a file; the system does not read inside it. Every upload is a new version of its type, the
 * newest is the current one, and the server keeps at most 10 per type. This module is the only place that knows the endpoint, so a
 * change to how the server versions files stays in one file.
 */

/** Mirrors the backend ProjectFileType enum. */
export type ProjectFileType =
  | 'PROJECT_PLAN'
  | 'VALIDATION_REPORT'
  | 'REGISTRATION_RESULT'
  | 'MONITORING_REPORT'
  | 'ACTIVITY_RESULT_LIST'
  | 'VERIFICATION_REPORT'
  | 'CERTIFICATION_RESULT'
  | 'VVB_QUERY_SHEET'
  | 'ADDITIONAL_MATERIAL';

export type ProjectFile = {
  id: number;
  fileType?: ProjectFileType | string;
  ownedByKanrisha?: boolean;
  originalFileName?: string | null;
  uploadedBy?: string | null;
  uploadedAt?: string | null;
  /** Per-type version number; absent from servers older than versioning. */
  versionNo?: number | null;
};

/** How many live versions one type holds, and whether another may be added. */
export type ProjectFileVersionSummary = {
  fileType: ProjectFileType | string;
  activeCount: number;
  limit: number;
  canUpload: boolean;
};

export type ProjectFileListing = {
  files: ProjectFile[];
  versionSummary: ProjectFileVersionSummary[];
};

type BaseResponse = { statusCode?: string; message?: string };
type FileListResponse = BaseResponse & {
  files?: ProjectFile[];
  versionSummary?: ProjectFileVersionSummary[];
};
type FileUploadResponse = BaseResponse & ProjectFile;
type FileDownloadResponse = BaseResponse & { downloadUrl?: string };

/** What the server accepts for any project file: the Excel 様式, or a PDF of it. */
export const PROJECT_FILE_ACCEPT = '.xlsx,.pdf';
export const PROJECT_PLAN_FILE_ACCEPT = PROJECT_FILE_ACCEPT;
export const PROJECT_FILE_MAX_BYTES = 10 * 1024 * 1024;

const CONTENT_TYPE_BY_EXTENSION: Record<string, string> = {
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  pdf: 'application/pdf',
};

const extensionOf = (fileName: string) => fileName.toLowerCase().split('.').pop() ?? '';

/** Why this file cannot be uploaded as a project file, or null when it can. */
export function projectFileProblem(file: File): string | null {
  if (!(extensionOf(file.name) in CONTENT_TYPE_BY_EXTENSION)) {
    return 'Excelファイル（.xlsx）またはPDFファイルを選択してください。';
  }
  if (file.size > PROJECT_FILE_MAX_BYTES) {
    return 'ファイルサイズは10MB以下である必要があります。';
  }
  if (file.size === 0) {
    return 'ファイルが空です。';
  }
  return null;
}

export const projectPlanFileProblem = projectFileProblem;

const filesPath = (projectId: string) => `/project/${encodeURIComponent(projectId)}/files`;

function fail(
  response: Response,
  json: BaseResponse | null | undefined,
  rawBody: string | undefined,
  fallback: string,
): never {
  throw new ApiError(json?.message || rawBody || `${fallback}: HTTP ${response.status}`, {
    status: response.status,
    payload: json,
    rawBody,
  });
}

/** Uploaded files of one type, newest first. */
export async function getProjectFiles(
  projectId: string,
  fileType: ProjectFileType,
): Promise<ProjectFile[]> {
  const response = await apiFetch(`${filesPath(projectId)}?fileType=${fileType}`, {
    method: 'GET',
    forbiddenHandling: 'caller',
  });
  const { data: json, rawBody } = await parseJsonResponse<FileListResponse>(response);
  if (!response.ok) fail(response, json, rawBody, 'Failed to load project files');
  return json?.files ?? [];
}

/**
 * Every live file of the project, newest first, with the per-type version
 * counts the server enforces its limit against.
 */
export async function getProjectFileListing(projectId: string): Promise<ProjectFileListing> {
  const response = await apiFetch(filesPath(projectId), {
    method: 'GET',
    forbiddenHandling: 'caller',
  });
  const { data: json, rawBody } = await parseJsonResponse<FileListResponse>(response);
  if (!response.ok) fail(response, json, rawBody, 'Failed to load project files');
  return { files: json?.files ?? [], versionSummary: json?.versionSummary ?? [] };
}

/**
 * Uploads one file. Never replaces an earlier one.
 *
 * The content type is set from the extension: a machine without Excel reports
 * an .xlsx as octet-stream, which the server would turn away.
 */
export async function uploadProjectFile(
  projectId: string,
  fileType: ProjectFileType,
  file: File,
): Promise<ProjectFile> {
  const contentType = CONTENT_TYPE_BY_EXTENSION[extensionOf(file.name)] ?? file.type;
  const form = new FormData();
  form.append('file', new File([file], file.name, { type: contentType }));

  const response = await apiFetch(`${filesPath(projectId)}?fileType=${fileType}`, {
    method: 'POST',
    body: form,
    forbiddenHandling: 'caller',
  });
  const { data: json, rawBody } = await parseJsonResponse<FileUploadResponse>(response);
  if (!response.ok || !json) fail(response, json, rawBody, 'Failed to upload file');
  return json;
}

/** A short-lived URL for one file. Ask on click: it expires. */
export async function getProjectFileDownloadUrl(
  projectId: string,
  fileId: number,
): Promise<string> {
  const response = await apiFetch(`${filesPath(projectId)}/${fileId}/download`, {
    method: 'GET',
    forbiddenHandling: 'caller',
  });
  const { data: json, rawBody } = await parseJsonResponse<FileDownloadResponse>(response);
  if (!response.ok || !json?.downloadUrl) fail(response, json, rawBody, 'Failed to get file URL');
  return json.downloadUrl;
}

/** Removes one file. The server keeps it soft-deleted; the UI cannot bring it back. */
export async function deleteProjectFile(projectId: string, fileId: number): Promise<void> {
  const response = await apiFetch(`${filesPath(projectId)}/${fileId}`, {
    method: 'DELETE',
    forbiddenHandling: 'caller',
  });
  const { data: json, rawBody } = await parseJsonResponse<BaseResponse>(response);
  if (!response.ok) fail(response, json, rawBody, 'Failed to delete file');
}
