import {
  deleteNotification,
  getNotifications,
  getUnreadNotificationCount,
  markNotificationAsRead,
} from './index';

describe('notification API', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('uses the notification list and count endpoints and maps the response', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            statusCode: 'OK',
            notifications: [
              {
                id: 1,
                title: 'プロジェクト申請が承認されました',
                message: '拠点Aの参加申請が承認されました。',
                read: false,
                operatingUserId: 10,
                participatingUserId: null,
                createdAt: '2026-07-29T10:30:00',
                updatedAt: '2026-07-29T10:30:00',
              },
            ],
            pagination: {
              page: 1,
              limit: 5,
              totalItems: 1,
              totalPages: 1,
              hasMore: false,
            },
          }),
          { status: 200 },
        ),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            statusCode: 'OK',
            unreadCount: 5,
          }),
          { status: 200 },
        ),
      );
    vi.stubGlobal('fetch', fetchMock);

    const list = await getNotifications({
      unreadOnly: true,
      page: 1,
      size: 5,
    });
    const unreadCount = await getUnreadNotificationCount();

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      '/api/notification/list?unreadOnly=true&page=1&size=5',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/notification/count',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(list.notifications[0]).toMatchObject({
      id: 1,
      read: false,
      createdAt: '2026-07-29T10:30:00',
    });
    expect(list.pagination).toMatchObject({
      page: 1,
      limit: 5,
      totalItems: 1,
    });
    expect(unreadCount).toBe(5);
  });

  it('uses the read and soft-delete PATCH endpoints', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(null, { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);

    await markNotificationAsRead(12);
    await deleteNotification(12);

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      '/api/notification/12/read',
      expect.objectContaining({ method: 'PATCH' }),
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/notification/12/delete',
      expect.objectContaining({ method: 'PATCH' }),
    );
  });
});
