import { beforeEach, describe, expect, it, vi } from 'vitest';

const notificationApiMocks = vi.hoisted(() => ({
  deleteNotification: vi.fn(),
  getNotifications: vi.fn(),
  getUnreadNotificationCount: vi.fn(),
  markNotificationAsRead: vi.fn(),
}));

vi.mock('@/api/notifications', () => notificationApiMocks);

import { useNotificationStore } from './notificationStore';

const unreadNotification = {
  id: 1,
  title: '新しい通知',
  message: '通知本文',
  read: false,
  operatingUserId: 10,
  participatingUserId: null,
  createdAt: '2026-07-29T10:30:00',
  updatedAt: '2026-07-29T10:30:00',
};

describe('notificationStore', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useNotificationStore.getState().clearNotifications();
    notificationApiMocks.getNotifications.mockResolvedValue({
      notifications: [unreadNotification],
      pagination: {
        page: 0,
        limit: 10,
        totalItems: 1,
        totalPages: 1,
        nextPageToken: null,
        hasMore: false,
      },
    });
    notificationApiMocks.getUnreadNotificationCount.mockResolvedValue(5);
    notificationApiMocks.markNotificationAsRead.mockResolvedValue(undefined);
    notificationApiMocks.deleteNotification.mockResolvedValue(undefined);
  });

  it('loads the list and authoritative unread count together', async () => {
    await useNotificationStore.getState().loadNotifications();

    expect(notificationApiMocks.getNotifications).toHaveBeenCalledTimes(1);
    expect(
      notificationApiMocks.getUnreadNotificationCount,
    ).toHaveBeenCalledTimes(1);
    expect(useNotificationStore.getState()).toMatchObject({
      notifications: [unreadNotification],
      unreadCount: 5,
      loading: false,
      error: null,
    });
  });

  it('updates local read and delete state only after each PATCH succeeds', async () => {
    await useNotificationStore.getState().loadNotifications();
    await useNotificationStore.getState().markAsRead(1);

    expect(notificationApiMocks.markNotificationAsRead).toHaveBeenCalledWith(1);
    expect(useNotificationStore.getState().notifications[0].read).toBe(true);
    expect(useNotificationStore.getState().unreadCount).toBe(4);

    await useNotificationStore.getState().deleteNotification(1);

    expect(notificationApiMocks.deleteNotification).toHaveBeenCalledWith(1);
    expect(useNotificationStore.getState().notifications).toEqual([]);
    expect(useNotificationStore.getState().unreadCount).toBe(4);
    expect(useNotificationStore.getState().pagination?.totalItems).toBe(0);
  });

  it('keeps local notification state unchanged when a mutation fails', async () => {
    await useNotificationStore.getState().loadNotifications();
    notificationApiMocks.markNotificationAsRead.mockRejectedValue(
      new Error('request failed'),
    );

    await expect(
      useNotificationStore.getState().markAsRead(1),
    ).rejects.toThrow('request failed');

    expect(useNotificationStore.getState().notifications[0].read).toBe(false);
    expect(useNotificationStore.getState().unreadCount).toBe(5);
  });
});
