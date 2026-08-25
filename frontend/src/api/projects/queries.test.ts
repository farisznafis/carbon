import { ApiError } from '../_shared/errors';
import {
  downloadProjectMembershipPdf,
  getProjectDetails,
} from './queries';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('getProjectDetails', () => {
  it('uses the Project public ID in the detail path', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          statusCode: 'OK',
          message: null,
          errors: null,
          project: {
            id: 'PROJ0123',
          },
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    const project = await getProjectDetails('PROJ0123');

    expect(project.id).toBe('PROJ0123');
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/project/PROJ0123',
      expect.objectContaining({ method: 'GET' }),
    );
  });
});

describe('downloadProjectMembershipPdf', () => {
  it('gets a presigned PDF URL using encoded project and participation IDs', async () => {
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

    const result = await downloadProjectMembershipPdf('PROJ/001', 42);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/project/PROJ%2F001/participations/42/membership-pdf',
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

    await expect(
      downloadProjectMembershipPdf('PROJ001', 7),
    ).rejects.toThrow('PDFのダウンロードURLが見つかりませんでした');
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

    await expect(
      downloadProjectMembershipPdf('PROJ001', 7),
    ).rejects.toEqual(
      expect.objectContaining<ApiError>({
        name: 'ApiError',
        status: 409,
        message: 'PDFはまだ生成されていません。',
      }),
    );
  });
});
