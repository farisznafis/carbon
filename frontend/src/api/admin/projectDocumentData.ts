import { apiFetch, parseJsonResponse } from '@/api/_shared/httpClient';
import { ApiError } from '@/api/_shared/errors';
import type { ProjectFileType } from '@/api/admin/projectFiles';

/**
 * The values the server reads out of a project's latest uploaded form, grouped
 * the way the form groups them: plain label/value items, and a table where the
 * form has one.
 */

/** How a value should be read; the server may add more kinds. */
export type DocumentDataValueType = 'TEXT' | 'DATE' | 'NUMBER' | (string & {});

export type DocumentDataItem = {
  key: string;
  label: string;
  value: string | null;
  unit: string | null;
  type: DocumentDataValueType;
};

export type DocumentDataColumn = {
  key: string;
  label: string;
  unit: string | null;
  type: DocumentDataValueType;
};

export type DocumentDataTable = {
  columns: DocumentDataColumn[];
  /** One array per row, in column order. */
  rows: (string | null)[][];
};

export type DocumentDataSection = {
  key: string;
  title: string;
  items: DocumentDataItem[];
  table: DocumentDataTable | null;
};

export type ProjectDocumentData = {
  projectId: string;
  fileType: ProjectFileType | string;
  sections: DocumentDataSection[];
};

type DocumentDataResponse = {
  statusCode?: string;
  message?: string;
  projectId?: string;
  fileType?: string;
  sections?: Partial<DocumentDataSection>[] | null;
};

const normalizeTable = (table: DocumentDataTable | null | undefined): DocumentDataTable | null =>
  table && Array.isArray(table.columns) && table.columns.length > 0
    ? { columns: table.columns, rows: Array.isArray(table.rows) ? table.rows : [] }
    : null;

/** GET /project/{projectId}/document-data/{fileType} — the data read from the latest file of one type. */
export async function getProjectDocumentData(
  projectId: string,
  fileType: ProjectFileType,
): Promise<ProjectDocumentData> {
  const response = await apiFetch(
    `/project/${encodeURIComponent(projectId)}/document-data/${fileType}`,
    { method: 'GET', forbiddenHandling: 'caller' },
  );
  const { data: json, rawBody } = await parseJsonResponse<DocumentDataResponse>(response);
  if (!response.ok) {
    throw new ApiError(
      json?.message || rawBody || `Failed to load document data: HTTP ${response.status}`,
      { status: response.status, payload: json, rawBody },
    );
  }

  return {
    projectId: json?.projectId ?? projectId,
    fileType: json?.fileType ?? fileType,
    sections: (json?.sections ?? []).map((section, index) => ({
      key: section.key ?? `section-${index}`,
      title: section.title ?? '',
      items: Array.isArray(section.items) ? section.items : [],
      table: normalizeTable(section.table),
    })),
  };
}
