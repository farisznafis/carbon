import { describe, expect, it } from 'vitest';
import {
  DEFAULT_UPLOAD_FILE_EXTENSIONS,
  getUploadFileValidationError,
  MAX_UPLOAD_FILE_SIZE_BYTES,
} from './uploadFileValidation';

/** Creates a lightweight File with a deterministic size for upload validation tests. */
function createSizedFile(name: string, size: number): File {
  const file = new File(['content'], name);
  Object.defineProperty(file, 'size', { value: size });
  return file;
}

describe('getUploadFileValidationError', () => {
  it('accepts supported extensions case-insensitively at the 10MB limit', () => {
    const file = createSizedFile('EVIDENCE.PDF', MAX_UPLOAD_FILE_SIZE_BYTES);

    expect(
      getUploadFileValidationError(file, DEFAULT_UPLOAD_FILE_EXTENSIONS),
    ).toBeNull();
  });

  it('rejects files larger than 10MB', () => {
    const file = createSizedFile(
      'evidence.pdf',
      MAX_UPLOAD_FILE_SIZE_BYTES + 1,
    );

    expect(
      getUploadFileValidationError(file, DEFAULT_UPLOAD_FILE_EXTENSIONS),
    ).toBe('ファイルサイズは10MB以下にしてください。');
  });

  it('rejects unsupported extensions and lists the accepted formats', () => {
    const file = createSizedFile('evidence.exe', 100);

    expect(
      getUploadFileValidationError(file, DEFAULT_UPLOAD_FILE_EXTENSIONS),
    ).toBe(
      '対応していないファイル形式です。（.pdf, .csv, .jpg, .jpeg, .png）',
    );
  });
});
