import { getProjectDetails } from './queries';

describe('getProjectDetails', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

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
