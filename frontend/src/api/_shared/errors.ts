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

export function getApiErrorMessage(
  response: Response,
  payload: ApiErrorPayload | null,
  rawBody: string,
  fallbackMessage: string,
): string {
  return payload?.message || rawBody || `${fallbackMessage}: HTTP ${response.status}`;
}
