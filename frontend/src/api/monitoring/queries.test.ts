import { getAdminProjectMonitoringRecords, getMonitoringRecent } from './queries';

function jsonResponse(body: unknown) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('getMonitoringRecent', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('reads the latest record from the current camelCase response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({
          statusCode: 'OK',
          data: {
            projectId: 'PROJ0123',
            dateRecorded: '2026-10-01',
            elPv: 1200.5,
            elPvr: 800,
            elpjBatteryValue: 50,
            elpjControlValue: 720,
            elpjAccuracy: 98,
          },
        }),
      ),
    );

    const [latest] = await getMonitoringRecent({ projectId: 'PROJ0123', siteId: 'SITE0001' });

    expect(latest).toMatchObject({
      dateRecorded: '2026-10-01',
      ELpv_value: 1200.5,
      ELpvr_value: 800,
      ELPJ_battery_value: 50,
      ELPJ_control_value: 720,
      ELPJ_accuracy: 98,
    });
  });

  it('returns no rows when nothing is registered yet', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ statusCode: 'OK', message: 'OK' })));

    await expect(getMonitoringRecent({ projectId: 'PROJ0123', siteId: 'SITE0001' })).resolves.toEqual([]);
  });
});

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
