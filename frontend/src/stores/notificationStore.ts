import { create } from 'zustand';
import { getUserNotifications, type UserNotification } from '@/api/users';
import { logger } from '@/lib/logger';

type NotificationState = {
  notifications: UserNotification[];
  loading: boolean;
  error: string | null;
  unreadCount: number;
  loadNotifications: () => Promise<void>;
  markAsRead: (notificationId: UserNotification['id']) => void;
  clearNotifications: () => void;
};

const getUnreadCount = (notifications: UserNotification[]) =>
  notifications.filter((notification) => !notification.read).length;

export const useNotificationStore = create<NotificationState>((set) => ({
  notifications: [],
  loading: false,
  error: null,
  unreadCount: 0,

  loadNotifications: async () => {
    set({ loading: true, error: null });

    try {
      const notifications = await getUserNotifications();

      set({
        notifications,
        unreadCount: getUnreadCount(notifications),
      });
    } catch (error) {
      logger.error('Failed to load notifications:', error);
      set({
        notifications: [],
        unreadCount: 0,
        error: 'お知らせの取得に失敗しました。',
      });
    } finally {
      set({ loading: false });
    }
  },

  markAsRead: (notificationId) =>
    set((state) => {
      const notifications = state.notifications.map((notification) =>
        notification.id === notificationId && !notification.read
          ? { ...notification, read: true }
          : notification,
      );

      return {
        notifications,
        unreadCount: getUnreadCount(notifications),
      };
    }),

  clearNotifications: () =>
    set({
      notifications: [],
      loading: false,
      error: null,
      unreadCount: 0,
    }),
}));
