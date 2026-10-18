import { apiFetch, parseJsonResponse } from '@/api/_shared/httpClient';
import { ApiError } from '@/api/_shared/errors';

/**
 * The companies taking part in a project, each with the 入会届 PDF it submitted.
 * One PDF per company: it is not versioned like the project's own files.
 */
export type ProjectMembershipPdf = {
  companyId: string;
  companyName: string;
  participationId: number;
  pdfStatus: string;
  /** Presigned, short-lived; null until the PDF exists. */
  pdfUrl: string | null;
};

type CompaniesResponse = {
  statusCode?: string;
  message?: string;
  companies?: Partial<ProjectMembershipPdf>[] | null;
};

/** GET /project/{projectId}/companies — the project's companies and their 入会届 PDFs. */
export async function getProjectMembershipPdfs(projectId: string): Promise<ProjectMembershipPdf[]> {
  const response = await apiFetch(`/project/${encodeURIComponent(projectId)}/companies`, {
    method: 'GET',
    forbiddenHandling: 'caller',
  });
  const { data: json, rawBody } = await parseJsonResponse<CompaniesResponse>(response);
  if (!response.ok) {
    throw new ApiError(
      json?.message || rawBody || `Failed to load project companies: HTTP ${response.status}`,
      { status: response.status, payload: json, rawBody },
    );
  }
  return (json?.companies ?? []).map((company) => ({
    companyId: company.companyId ?? '',
    companyName: company.companyName ?? '—',
    participationId: company.participationId ?? 0,
    pdfStatus: company.pdfStatus ?? '',
    pdfUrl: company.pdfUrl || null,
  }));
}
