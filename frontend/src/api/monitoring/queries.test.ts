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

  it('normalizes current projectId fields before returning list records', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          items: [
            {
              id: 'MON0001',
              projectId: 'PROJ0123',
              siteId: 'SITE0001',
              participatingUserId: 7,
              dateRecorded: '2026-07-23',
              elPv: 10,
              isActive: true,
            },
          ],
          pagination: {
            page: 1,
            limit: 10,
            totalItems: 1,
            totalPages: 1,
          },
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await getAdminProjectMonitoringRecords({
      projectId: 'PROJ0123',
    });

    expect(result.items[0]).toMatchObject({
      id: 'MON0001',
      projectId: 'PROJ0123',
      siteId: 'SITE0001',
      participatingUserId: 7,
      elPv: 10,
      isActive: true,
    });
  });
});
