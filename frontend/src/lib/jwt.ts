type JwtPayload = Record<string, unknown>;

/** Decodes one base64url JWT payload without treating it as verified data. */
function decodeJwtPayload(token: string): JwtPayload | null {
  const payloadSegment = token.split('.')[1];
  if (!payloadSegment) return null;

  try {
    const base64 = payloadSegment.replace(/-/g, '+').replace(/_/g, '/');
    const paddedBase64 = base64.padEnd(
      base64.length + ((4 - (base64.length % 4)) % 4),
      '=',
    );
    const binaryPayload = window.atob(paddedBase64);
    const payloadBytes = Uint8Array.from(
      binaryPayload,
      (character) => character.charCodeAt(0),
    );
    const decodedPayload = new TextDecoder().decode(payloadBytes);
    const parsedPayload: unknown = JSON.parse(decodedPayload);

    return parsedPayload && typeof parsedPayload === 'object'
      ? (parsedPayload as JwtPayload)
      : null;
  } catch {
    return null;
  }
}

/** Reads the operator userId claim used only to select the self-profile endpoint. */
export function getJwtUserId(token: string | null | undefined): string | null {
  if (!token) return null;

  const userId = decodeJwtPayload(token)?.userId;
  if (typeof userId !== 'string' && typeof userId !== 'number') return null;

  const normalizedUserId = String(userId).trim();
  return normalizedUserId || null;
}
