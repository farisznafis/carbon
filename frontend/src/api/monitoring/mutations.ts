import { apiFetch, apiRequest, apiVoidRequest, parseJsonResponse } from "../_shared/httpClient";
import {
  uploadFormDataRequest,
  type UploadRequestOptions,
} from '../_shared/uploadRequest';
import { ApiError, getApiErrorMessage } from "../_shared/errors";
import type { ApiErrorPayload } from "../_shared/types";
import type {
  MonitoringCancelDecisionPayload,
  MonitoringCancelDecisionResponse,
  MonitoringCancelRequestPayload,
  MonitoringCancelRequestResponse,
  MonitoringData,
  SubmitDataResponse,
} from "./types";
import type { MonitoringEvidenceKind } from "./types";

type UploadResponse = {
  file?: {
    id?: string;
    fileType?: string;
    fileName?: string;
    contentType?: string;
    fileSize?: number;
    url?: string;
    s3Key?: string;
  };
  message?: string;
};

/** Uploads one monitoring evidence file to its endpoint-specific storage route. */
async function uploadEvidenceFile(
  kind: MonitoringEvidenceKind,
  file: File,
  projectId: string,
  siteId: string | number,
  description?: string,
  options?: UploadRequestOptions,
): Promise<string> {
  const endpointMap: Record<MonitoringEvidenceKind, string> = {
    elpv: "upload-elpv-evidence",
    elpvr: "upload-elpvr-evidence",
    elpjBattery: "upload-elpj-battery-specification-evidence",
    elpjControl: "upload-elpj-control-evidence",
  };

  const formData = new FormData();
  formData.append("file", file);

  const query = new URLSearchParams();
  query.set("projectId", String(projectId));
  query.set("siteId", String(siteId));
  if (description) {
    query.set("description", description);
  }

  const json = await uploadFormDataRequest<UploadResponse>(
    `/monitoring/${endpointMap[kind]}?${query.toString()}`,
    formData,
    {
      ...options,
      fallbackErrorMessage: 'エビデンスファイルのアップロードに失敗しました。',
    },
  );

  const uploadedFileLocation = json?.file?.url ?? json?.file?.s3Key ?? '';
  if (!uploadedFileLocation) {
    throw new Error('アップロードしたファイル情報を取得できませんでした。');
  }

  return uploadedFileLocation;
}

/** POST /monitoring — submits a validated monitoring record. */
export async function submitMonitoringData(
  data: MonitoringData,
): Promise<SubmitDataResponse> {
  const response = await apiFetch('/monitoring', {
    method: "POST",
    body: data,
  });

  const { data: json, rawBody } =
    await parseJsonResponse<SubmitDataResponse & ApiErrorPayload>(response);

  if (!response.ok) {
    throw new ApiError(
      getApiErrorMessage(
        response,
        json,
        rawBody,
        "Failed to submit monitoring data",
      ),
      {
        status: response.status,
        payload: json,
        rawBody,
      },
    );
  }

  return json as SubmitDataResponse;
}

/** POST /monitoring/validate — validates monitoring data before confirmation. */
export async function validateMonitoringData(data: MonitoringData): Promise<void> {
  await apiVoidRequest('/monitoring/validate', {
    method: 'POST',
    body: data,
    fallbackErrorMessage: 'Failed to validate monitoring data',
  });
}

/** POST /monitoring/cancel-request — requests cancellation of a monitoring record. */
export async function createMonitoringCancelRequest(
  payload: MonitoringCancelRequestPayload,
): Promise<MonitoringCancelRequestResponse> {
  return apiRequest<MonitoringCancelRequestResponse>(
    '/monitoring/cancel-request',
    {
      method: 'POST',
      body: payload,
      fallbackErrorMessage: 'Failed to create monitoring cancel request',
    },
  );
}

/** POST /monitoring/{id}/cancel-decision — approves or rejects a cancellation request. */
export async function decideMonitoringCancelRequest(
  cancelRequestId: number,
  payload: MonitoringCancelDecisionPayload,
): Promise<MonitoringCancelDecisionResponse> {
  return apiRequest<MonitoringCancelDecisionResponse>(
    `/monitoring/${cancelRequestId}/cancel-decision`,
    {
      method: 'POST',
      body: payload,
      fallbackErrorMessage: 'Failed to decide monitoring cancel request',
    },
  );
}

/** POST /monitoring/upload-elpv-evidence — uploads generation evidence with optional progress. */
export async function uploadElpvFile(
  file: File,
  projectId: string,
  siteId: string | number,
  description?: string,
  options?: UploadRequestOptions,
): Promise<string> {
  return uploadEvidenceFile("elpv", file, projectId, siteId, description, options);
}

/** POST /monitoring/upload-elpvr-evidence — uploads consumption evidence with optional progress. */
export async function uploadElpvrFile(
  file: File,
  projectId: string,
  siteId: string | number,
  description?: string,
  options?: UploadRequestOptions,
): Promise<string> {
  return uploadEvidenceFile("elpvr", file, projectId, siteId, description, options);
}

/** POST /monitoring/upload-elpj-battery-specification-evidence — uploads battery evidence. */
export async function uploadElpjBatteryFile(
  file: File,
  projectId: string,
  siteId: string | number,
  description?: string,
  options?: UploadRequestOptions,
): Promise<string> {
  return uploadEvidenceFile("elpjBattery", file, projectId, siteId, description, options);
}

/** POST /monitoring/upload-elpj-control-evidence — uploads control-device evidence. */
export async function uploadElpjControlFile(
  file: File,
  projectId: string,
  siteId: string | number,
  description?: string,
  options?: UploadRequestOptions,
): Promise<string> {
  return uploadEvidenceFile("elpjControl", file, projectId, siteId, description, options);
}
