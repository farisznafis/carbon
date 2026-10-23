import { ApiError } from '@/api/_shared/errors';

const FALLBACK_MESSAGE = 'ログインできませんでした。時間をおいて再度お試しください。';

/**
 * Routes a failed login to the form. A 400 VALIDATION_ERROR goes to its fields (returns null, no
 * banner); anything else returns the banner text. fieldMap maps backend keys to form fields.
 */
export function resolveLoginError<Field extends string>(
  error: unknown,
  fieldMap: Record<string, Field>,
  setFieldError: (field: Field, message: string) => void,
): string | null {
  if (!(error instanceof ApiError)) {
    // TypeError is fetch's network failure; other Errors are app messages worth showing.
    return error instanceof Error && !(error instanceof TypeError) && error.message
      ? error.message
      : FALLBACK_MESSAGE;
  }

  const { status, payload } = error;
  if (status === 400 && payload?.statusCode === 'VALIDATION_ERROR' && payload.errors && typeof payload.errors === 'object') {
    const fieldErrors = Object.entries(payload.errors).filter(
      (entry): entry is [string, string] => Object.prototype.hasOwnProperty.call(fieldMap, entry[0]) &&typeof entry[1] === 'string',
    );
    fieldErrors.forEach(([key, message]) => setFieldError(fieldMap[key], message));
    if (fieldErrors.length > 0) return null;
  }

  if (status >= 500 || !payload?.message) return FALLBACK_MESSAGE;
  return payload.message;
}
