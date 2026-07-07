import { apiFetch, apiVoidRequest, parseJsonResponse } from "../_shared/httpClient";
import { ApiError, getApiErrorMessage } from "../_shared/errors";
import type { ApiErrorPayload } from "../_shared/types";
import type { MonitoringData, SubmitDataResponse } from "./types";
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

async function uploadEvidenceFile(
  kind: MonitoringEvidenceKind,
  file: File,
  projectId: string | number,
  siteId: string | number,
  description?: string,
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

  const response = await apiFetch(
    `/monitoring/${endpointMap[kind]}?${query.toString()}`,
    {
      method: "POST",
      body: formData,
    },
  );

  const { data: json, rawBody } = await parseJsonResponse<UploadResponse>(response);

  if (!response.ok) {
    throw new Error(
      json?.message || rawBody || `Upload failed: HTTP ${response.status}`,
    );
  }

  return json?.file?.url ?? json?.file?.s3Key ?? "";
}

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

export async function validateMonitoringData(data: MonitoringData): Promise<void> {
  await apiVoidRequest('/monitoring/validate', {
    method: 'POST',
    body: data,
    fallbackErrorMessage: 'Failed to validate monitoring data',
  });
}

export async function uploadElpvFile(
  file: File,
  projectId: string | number,
  siteId: string | number,
  description?: string,
): Promise<string> {
  return uploadEvidenceFile("elpv", file, projectId, siteId, description);
}

export async function uploadElpvrFile(
  file: File,
  projectId: string | number,
  siteId: string | number,
  description?: string,
): Promise<string> {
  return uploadEvidenceFile("elpvr", file, projectId, siteId, description);
}

export async function uploadElpjBatteryFile(
  file: File,
  projectId: string | number,
  siteId: string | number,
  description?: string,
): Promise<string> {
  return uploadEvidenceFile("elpjBattery", file, projectId, siteId, description);
}

export async function uploadElpjControlFile(
  file: File,
  projectId: string | number,
  siteId: string | number,
  description?: string,
): Promise<string> {
  return uploadEvidenceFile("elpjControl", file, projectId, siteId, description);
}
