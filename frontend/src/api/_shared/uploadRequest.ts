import { BASE_URL, getAuthHeaders } from '@/api/config';
import { ApiError } from './errors';
import type { ApiErrorPayload } from './types';

export type UploadRequestOptions = {
  onProgress?: (progress: number | null) => void;
  signal?: AbortSignal;
  fallbackErrorMessage?: string;
};

/** Parses an XHR response body without assuming the server always returns JSON. */
function parseUploadResponse<T>(rawBody: string): T | null {
  if (!rawBody) return null;

  try {
    return JSON.parse(rawBody) as T;
  } catch {
    return null;
  }
}

/** Sends multipart data with real browser upload progress when total bytes are available. */
export function uploadFormDataRequest<T>(
  path: string,
  formData: FormData,
  options: UploadRequestOptions = {},
): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const request = new XMLHttpRequest();
    const abortRequest = () => request.abort();

    request.open('POST', `${BASE_URL}${path}`);

    const headers = new Headers(getAuthHeaders());
    headers.delete('Content-Type');
    if (import.meta.env.DEV) headers.set('ngrok-skip-browser-warning', 'true');
    headers.forEach((value, key) => request.setRequestHeader(key, value));

    request.upload.addEventListener('progress', (event) => {
      if (!event.lengthComputable || event.total <= 0) {
        options.onProgress?.(null);
        return;
      }

      options.onProgress?.(Math.min(100, Math.max(0, (event.loaded / event.total) * 100)));
    });

    request.addEventListener('load', () => {
      options.signal?.removeEventListener('abort', abortRequest);
      const rawBody = request.responseText ?? '';
      const payload = parseUploadResponse<T & ApiErrorPayload>(rawBody);

      if (request.status < 200 || request.status >= 300) {
        reject(
          new ApiError(
            payload?.message ||
              rawBody ||
              `${options.fallbackErrorMessage ?? 'Upload failed'}: HTTP ${request.status}`,
            {
              status: request.status,
              payload,
              rawBody,
            },
          ),
        );
        return;
      }

      resolve(payload as T);
    });

    request.addEventListener('error', () => {
      options.signal?.removeEventListener('abort', abortRequest);
      reject(new Error(options.fallbackErrorMessage ?? 'アップロードに失敗しました。'));
    });

    request.addEventListener('abort', () => {
      options.signal?.removeEventListener('abort', abortRequest);
      reject(new DOMException('アップロードを中止しました。', 'AbortError'));
    });

    if (options.signal?.aborted) {
      abortRequest();
      return;
    }

    options.signal?.addEventListener('abort', abortRequest, { once: true });
    options.onProgress?.(null);
    request.send(formData);
  });
}
