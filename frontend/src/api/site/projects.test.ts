import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../_shared/errors';
import {
  downloadProjectMembershipPdf,
  getMyProjectParticipations,
  getSiteProjects,
} from './projects';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('getSiteProjects', () => {
  it('maps joined project information without requiring a participation ID', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            statusCode: 'OK',
            projects: [
              {
                id: 'PROJ/001',
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
        isJoined: true,
      }),
    );
  });
});

describe('getMyProjectParticipations', () => {
  it('loads participation IDs using the encoded project ID', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify([
          {
            participationId: 42,
            siteId: 'site001',
            siteName: '東京拠点',
            status: 'APPROVED',
            pdfStatus: 'READY',
          },
        ]),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    await expect(getMyProjectParticipations('PROJ/001')).resolves.toEqual([
      {
        participationId: 42,
        siteId: 'site001',
        siteName: '東京拠点',
        status: 'APPROVED',
        pdfStatus: 'READY',
      },
    ]);
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/project/PROJ%2F001/my-participations',
      expect.objectContaining({ method: 'GET' }),
    );
  });
});

describe('downloadProjectMembershipPdf', () => {
  it('gets a presigned PDF URL using the participation ID', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          pdfUrl: 'https://example.com/membership.pdf',
          expiresIn: 300,
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await downloadProjectMembershipPdf(42);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/project/participation/42/pdf',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(result).toEqual({
      pdfUrl: 'https://example.com/membership.pdf',
      expiresIn: 300,
    });
  });

  it('rejects a successful response without a PDF URL', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ expiresIn: 0 }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    );

    await expect(downloadProjectMembershipPdf(7)).rejects.toThrow(
      'PDFのダウンロードURLが見つかりませんでした',
    );
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

    await expect(downloadProjectMembershipPdf(7)).rejects.toEqual(
      expect.objectContaining<ApiError>({
        name: 'ApiError',
        status: 409,
        message: 'PDFはまだ生成されていません。',
      }),
    );
  });
});
