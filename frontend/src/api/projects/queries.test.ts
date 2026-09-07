import { ApiError } from '../_shared/errors';
import {
  downloadProjectMembershipPdf,
  getProjectDetails,
  getProjectMemberTermsInfo,
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

describe('getProjectMemberTermsInfo', () => {
  it('gets project-specific member terms using an encoded project ID', async () => {
    const responseBody = {
      statusCode: 'OK',
      message: '',
      projectName: '関東太陽光発電プロジェクト',
      operatorNameJa: '株式会社ABC',
      adminRepName: '山田太郎',
      projectOverview: '地域の太陽光発電設備を集約する事業',
      usePurpose: '地域脱炭素施策',
      startedAt: '2026-09-03T01:05:12.040Z',
    };
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(responseBody), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await expect(getProjectMemberTermsInfo('PROJ/001')).resolves.toEqual(
      responseBody,
    );
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/project/PROJ%2F001/member-terms-info',
      expect.objectContaining({ method: 'GET' }),
    );
  });

  it('rejects an invalid terms start timestamp', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            statusCode: 'OK',
            message: '',
            projectName: 'Project',
            operatorNameJa: 'Operator',
            adminRepName: 'Admin',
            projectOverview: 'Overview',
            usePurpose: 'Purpose',
            startedAt: 'invalid-date',
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    );

    await expect(getProjectMemberTermsInfo('PROJ001')).rejects.toThrow(
      '入会規約の施行日が正しくありません',
    );
  });
});

describe('downloadProjectMembershipPdf', () => {
  it('gets a presigned PDF URL using an encoded project ID', async () => {
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

    const result = await downloadProjectMembershipPdf('PROJ/001');

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/project/PROJ%2F001/membership-pdf',
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
      downloadProjectMembershipPdf('PROJ001'),
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

    await expect(downloadProjectMembershipPdf('PROJ001')).rejects.toEqual(
      expect.objectContaining<ApiError>({
        name: 'ApiError',
        status: 409,
        message: 'PDFはまだ生成されていません。',
      }),
    );
  });
});
