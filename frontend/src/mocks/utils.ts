import { HttpResponse, delay } from 'msw';

/** Simulated network latency so loading states stay visible in the demo. */
export const MOCK_LATENCY_MS = 250;

export type MockRole = 'master' | 'user' | 'admin';

export type MockTokenPayload = {
  userId: string;
  role: MockRole;
  companyId?: string;
};

const toBase64Url = (value: string) =>
  btoa(String.fromCharCode(...new TextEncoder().encode(value)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');

/** Builds an unsigned JWT-shaped token that the frontend can decode like a real one. */
export function createMockToken(payload: MockTokenPayload): string {
  const header = toBase64Url(JSON.stringify({ alg: 'none', typ: 'JWT' }));
  const body = toBase64Url(JSON.stringify({ ...payload, iat: Math.floor(Date.now() / 1000) }));
  return `${header}.${body}.mock`;
}

/** Reads the mock session from the Authorization header, if any. */
export function readMockSession(request: Request): MockTokenPayload | null {
  const token = request.headers.get('Authorization')?.replace(/^Bearer\s+/i, '');
  const segment = token?.split('.')[1];
  if (!segment) return null;

  try {
    const base64 = segment.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const bytes = Uint8Array.from(atob(padded), (character) => character.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes)) as MockTokenPayload;
  } catch {
    return null;
  }
}

export async function ok<T extends object>(body: T, status = 200) {
  await delay(MOCK_LATENCY_MS);
  return HttpResponse.json({ statusCode: 'OK', message: null, ...body }, { status });
}

/** Responds with a bare JSON value (array or string) without the statusCode envelope. */
export async function raw(body: unknown, status = 200) {
  await delay(MOCK_LATENCY_MS);
  return HttpResponse.json(body as never, { status });
}

export async function notFound(message = 'データが見つかりません（デモ）') {
  await delay(MOCK_LATENCY_MS);
  return HttpResponse.json({ statusCode: 'NOT_FOUND', message }, { status: 404 });
}

export async function readJson<T>(request: Request): Promise<Partial<T>> {
  try {
    return (await request.json()) as Partial<T>;
  } catch {
    return {};
  }
}

export function paginate<T>(items: T[], page: number, limit: number, firstPage = 1) {
  const safeLimit = Math.max(limit, 1);
  const pageIndex = Math.max(page - firstPage, 0);
  const totalPages = Math.max(Math.ceil(items.length / safeLimit), 1);
  const pageItems = items.slice(pageIndex * safeLimit, (pageIndex + 1) * safeLimit);

  return {
    items: pageItems,
    pagination: {
      page,
      limit: safeLimit,
      totalItems: items.length,
      totalPages,
      nextPageToken: null,
      hasMore: pageIndex + 1 < totalPages,
    },
  };
}

export function numberParam(url: URL, key: string, fallback: number) {
  const value = Number(url.searchParams.get(key));
  return Number.isFinite(value) && url.searchParams.has(key) ? value : fallback;
}

export const nowIso = () => new Date().toISOString();

/** Absolute URL of a generated demo file, served by the /mock-files handler. */
export const mockFileUrl = (fileName: string) =>
  new URL(`${import.meta.env.BASE_URL}mock-files/${encodeURIComponent(fileName)}`, window.location.origin).toString();

let sequence = 1000;
export const nextId = () => ++sequence;
