import { BASE_URL, getAuthHeaders } from '../config';
import { ApiError, getApiErrorMessage } from './errors';
import {
  getCurrentBrowserRouteKey,
  reportPageForbidden,
  type ForbiddenHandling,
} from './forbiddenAccess';
import type { ApiErrorPayload } from './types';

type ApiBody = BodyInit | object | null | undefined;

type ApiFetchOptions = Omit<RequestInit, 'body' | 'headers'> & {
  auth?: boolean;
  body?: ApiBody;
  forbiddenHandling?: ForbiddenHandling;
  headers?: HeadersInit;
};

type ParsedResponse<T> = {
  data: T | null;
  rawBody: string;
};

type ApiRequestResult<T> = ParsedResponse<T> & {
  response: Response;
};

function isPlainJsonBody(body: ApiBody): body is object {
  return Boolean(
    body &&
      typeof body === 'object' &&
      !(body instanceof FormData) &&
      !(body instanceof Blob) &&
      !(body instanceof URLSearchParams) &&
      !(body instanceof ArrayBuffer),
  );
}

function createHeaders(options: ApiFetchOptions, hasJsonBody: boolean): Headers {
  const headers = new Headers(options.auth === false ? undefined : getAuthHeaders());

  if (hasJsonBody && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  if (import.meta.env.DEV) {
    headers.set('ngrok-skip-browser-warning', 'true');
  }

  new Headers(options.headers).forEach((value, key) => {
    headers.set(key, value);
  });

  if (options.body instanceof FormData) {
    headers.delete('Content-Type');
  }

  return headers;
}

export async function apiFetch(path: string, options: ApiFetchOptions = {}): Promise<Response> {
  const hasJsonBody = isPlainJsonBody(options.body);
  const body = hasJsonBody ? JSON.stringify(options.body) : options.body;
  const { forbiddenHandling, ...requestOptions } = options;
  const requestRouteKey = getCurrentBrowserRouteKey();

  const response = await fetch(`${BASE_URL}${path}`, {
    ...requestOptions,
    body: body as BodyInit | null | undefined,
    headers: createHeaders(options, hasJsonBody),
  });

  const requestMethod = (options.method ?? 'GET').toUpperCase();
  const isAdminPage =
    typeof window !== 'undefined' &&
    window.location.pathname.startsWith('/admin');

  if (
    response.status === 403 &&
    requestMethod === 'GET' &&
    forbiddenHandling !== 'caller' &&
    isAdminPage
  ) {
    reportPageForbidden({ requestPath: path, routeKey: requestRouteKey });
  }

  return response;
}

export async function parseJsonResponse<T>(response: Response): Promise<ParsedResponse<T>> {
  const rawBody = await response.text();

  if (!rawBody) {
    return { data: null, rawBody };
  }

  try {
    return { data: JSON.parse(rawBody) as T, rawBody };
  } catch {
    return { data: null, rawBody };
  }
}

export async function apiRequest<T>(
  path: string,
  options: ApiFetchOptions & { fallbackErrorMessage?: string } = {},
): Promise<T> {
  const { data } = await apiRequestWithMeta<T>(path, options);
  return data as T;
}

export async function apiRequestWithMeta<T>(
  path: string,
  options: ApiFetchOptions & { fallbackErrorMessage?: string } = {},
): Promise<ApiRequestResult<T>> {
  const response = await apiFetch(path, options);
  const { data, rawBody } = await parseJsonResponse<T & ApiErrorPayload>(response);

  if (!response.ok) {
    throw new ApiError(
      getApiErrorMessage(
        response,
        data,
        rawBody,
        options.fallbackErrorMessage ?? 'API request failed',
      ),
      {
        status: response.status,
        payload: data,
        rawBody,
      },
    );
  }

  return { data: data as T, rawBody, response };
}

export async function apiVoidRequest(
  path: string,
  options: ApiFetchOptions & { fallbackErrorMessage?: string } = {},
): Promise<void> {
  await apiRequestWithMeta<unknown>(path, options);
}
