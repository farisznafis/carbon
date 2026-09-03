import type { ApiErrorPayload } from './types';

export class ApiError extends Error {
  readonly status: number;
  readonly payload: ApiErrorPayload | null;
  readonly rawBody: string;

  constructor(
    message: string,
    options: {
      status: number;
      payload?: ApiErrorPayload | null;
      rawBody?: string;
    },
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = options.status;
    this.payload = options.payload ?? null;
    this.rawBody = options.rawBody ?? '';
  }
}

/** Detects an HTML error document so infrastructure pages are not exposed in UI. */
function isHtmlErrorResponse(response: Response, rawBody: string): boolean {
  const contentType = response.headers.get('content-type')?.toLowerCase() ?? '';
  const normalizedBody = rawBody.trimStart().toLowerCase();

  return (
    contentType.includes('text/html') ||
    normalizedBody.startsWith('<!doctype html') ||
    normalizedBody.startsWith('<html')
  );
}

/** Resolves a safe backend error message without rendering raw HTML responses. */
export function getApiErrorMessage(
  response: Response,
  payload: ApiErrorPayload | null,
  rawBody: string,
  fallbackMessage: string,
): string {
  if (payload?.message) return payload.message;

  const normalizedBody = rawBody.trim();
  if (normalizedBody && !isHtmlErrorResponse(response, normalizedBody)) {
    return normalizedBody;
  }

  return `${fallbackMessage}: HTTP ${response.status}`;
}
