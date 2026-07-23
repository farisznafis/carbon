import { getAdminProjectMonitoringRecords } from './queries';

describe('getAdminProjectMonitoringRecords', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('uses the Project public ID in the monitoring list path', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          items: [],
          pagination: {
            page: 1,
            limit: 10,
            totalItems: 0,
            totalPages: 1,
            nextPageToken: null,
            hasMore: false,
          },
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    await getAdminProjectMonitoringRecords({
      projectId: 'PROJ0123',
    });

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/monitoring/PROJ0123/list?page=1&limit=10',
      expect.objectContaining({ method: 'GET' }),
    );
  });
});
