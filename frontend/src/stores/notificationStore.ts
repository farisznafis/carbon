import { create } from 'zustand';
import {
  deleteNotification as deleteNotificationRequest,
  getNotifications,
  getUnreadNotificationCount,
  markNotificationAsRead,
  type NotificationPagination,
  type UserNotification,
} from '@/api/notifications';
import { logger } from '@/lib/logger';

type NotificationState = {
  notifications: UserNotification[];
  pagination: NotificationPagination | null;
  loading: boolean;
  error: string | null;
  unreadCount: number;
  loadNotifications: () => Promise<void>;
  markAsRead: (notificationId: UserNotification['id']) => Promise<void>;
  deleteNotification: (
    notificationId: UserNotification['id'],
  ) => Promise<void>;
  clearNotifications: () => void;
};

export const useNotificationStore = create<NotificationState>((set) => ({
  notifications: [],
  pagination: null,
  loading: false,
  error: null,
  unreadCount: 0,

  loadNotifications: async () => {
    set({ loading: true, error: null });

    try {
      const [listResult, unreadCount] = await Promise.all([
        getNotifications(),
        getUnreadNotificationCount(),
      ]);

      set({
        notifications: listResult.notifications,
        pagination: listResult.pagination,
        unreadCount,
      });
    } catch (error) {
      logger.error('Failed to load notifications:', error);
      set({
        notifications: [],
        pagination: null,
        unreadCount: 0,
        error: 'お知らせの取得に失敗しました。',
      });
    } finally {
      set({ loading: false });
    }
  },

  markAsRead: async (notificationId) => {
    await markNotificationAsRead(notificationId);

    set((state) => {
      const target = state.notifications.find(
        (notification) => notification.id === notificationId,
      );
      const notifications = state.notifications.map((notification) =>
        notification.id === notificationId && !notification.read
          ? { ...notification, read: true }
          : notification,
      );

      return {
        notifications,
        unreadCount:
          target && !target.read
            ? Math.max(0, state.unreadCount - 1)
            : state.unreadCount,
      };
    });
  },

  deleteNotification: async (notificationId) => {
    await deleteNotificationRequest(notificationId);

    set((state) => {
      const target = state.notifications.find(
        (notification) => notification.id === notificationId,
      );

      return {
        notifications: state.notifications.filter(
          (notification) => notification.id !== notificationId,
        ),
        unreadCount:
          target && !target.read
            ? Math.max(0, state.unreadCount - 1)
            : state.unreadCount,
        pagination: state.pagination
          ? {
              ...state.pagination,
              totalItems: Math.max(0, state.pagination.totalItems - 1),
            }
          : null,
      };
    });
  },

  clearNotifications: () =>
    set({
      notifications: [],
      pagination: null,
      loading: false,
      error: null,
      unreadCount: 0,
    }),
}));
