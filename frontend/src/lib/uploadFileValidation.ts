export const MAX_UPLOAD_FILE_SIZE_BYTES = 10 * 1024 * 1024;
export const MAX_UPLOAD_FILE_SIZE_MESSAGE =
  'ファイルサイズは10MB以下にしてください。';
export const FILE_TYPE_ERROR_MESSAGE = '対応していないファイル形式です。';
export const DEFAULT_UPLOAD_FILE_EXTENSIONS = [
  '.pdf',
  '.csv',
  '.jpg',
  '.jpeg',
  '.png',
] as const;

/** Returns the frontend validation message for an unsupported or oversized upload. */
export function getUploadFileValidationError(
  file: File | null,
  allowedExtensions: readonly string[],
): string | null {
  if (!file) return null;

  const lowerFileName = file.name.toLowerCase();
  const isAllowedFileType = allowedExtensions.some((extension) =>
    lowerFileName.endsWith(extension.toLowerCase()),
  );

  if (!isAllowedFileType) {
    return `${FILE_TYPE_ERROR_MESSAGE}（${allowedExtensions.join(', ')}）`;
  }

  if (file.size > MAX_UPLOAD_FILE_SIZE_BYTES) {
    return MAX_UPLOAD_FILE_SIZE_MESSAGE;
  }

  return null;
}
