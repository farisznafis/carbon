import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../_shared/errors';
import { downloadProjectMembershipPdf, getSiteProjects } from './projects';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('getSiteProjects', () => {
  it('preserves the participation ID returned for a joined project', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            statusCode: 'OK',
            projects: [
              {
                id: 'PROJ/001',
                participationId: 42,
                projectName: '太陽光発電プロジェクト',
                projectNumber: 'P001',
                projectStatus: '実施中',
                isJoined: true,
              },
            ],
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    );

    const projects = await getSiteProjects('site001');

    expect(projects[0]).toEqual(
      expect.objectContaining({
        id: 'PROJ/001',
        participationId: 42,
        isJoined: true,
      }),
    );
  });
});

describe('downloadProjectMembershipPdf', () => {
  it('downloads the PDF using encoded IDs and the response filename', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(new Blob(['pdf'], { type: 'application/pdf' }), {
        status: 200,
        headers: {
          'Content-Disposition':
            "attachment; filename*=UTF-8''%E5%85%A5%E4%BC%9A%E5%B1%8A.pdf",
          'Content-Type': 'application/pdf',
        },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await downloadProjectMembershipPdf('PROJ/001', 42);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/project/PROJ%2F001/participations/42/membership-pdf',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(result.fileName).toBe('入会届.pdf');
    expect(result.blob.type).toBe('application/pdf');
  });

  it('uses a project-specific fallback filename', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(new Blob(['pdf']), {
          status: 200,
        }),
      ),
    );

    const result = await downloadProjectMembershipPdf('PROJ001', 7);

    expect(result.fileName).toBe('membership-PROJ001.pdf');
  });

  it('maps a backend error response to ApiError', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            statusCode: 'PDF_NOT_READY',
            message: 'PDFはまだ生成されていません。',
          }),
          { status: 409, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    );

    await expect(downloadProjectMembershipPdf('PROJ001', 7)).rejects.toEqual(
      expect.objectContaining<ApiError>({
        name: 'ApiError',
        status: 409,
        message: 'PDFはまだ生成されていません。',
      }),
    );
  });
});
