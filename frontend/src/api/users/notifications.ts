import { apiFetch, parseJsonResponse } from '@/api/_shared/httpClient';

export type UserNotification = {
  id: string | number;
  title: string;
  message: string;
  read: boolean;
};

type NotificationResponseItem = {
  id?: string | number;
  notificationId?: string | number;
  title?: string | null;
  message?: string | null;
  read?: boolean | null;
  isRead?: boolean | null;
};

type NotificationsResponse = {
  statusCode?: string;
  notifications?: NotificationResponseItem[];
  data?: NotificationResponseItem[];
  message?: string;
};

type NotificationsApiResponse =
  | NotificationResponseItem[]
  | NotificationsResponse;

const mapNotification = (
  item: NotificationResponseItem,
  index: number,
): UserNotification => ({
  id: item.id ?? item.notificationId ?? index,
  title: item.title ?? '',
  message: item.message ?? '',
  read: Boolean(item.read ?? item.isRead),
});

export async function getUserNotifications(): Promise<UserNotification[]> {
  const response = await apiFetch('/user/notifications', {
    method: 'GET',
  });

  const { data: json, rawBody } =
    await parseJsonResponse<NotificationsApiResponse>(response);

  if (!response.ok) {
    throw new Error(
      (!Array.isArray(json) && json?.message) ||
        rawBody ||
        `Failed to fetch notifications: HTTP ${response.status}`,
    );
  }

  const rows = Array.isArray(json)
    ? json
    : Array.isArray(json?.notifications)
      ? json.notifications
      : Array.isArray(json?.data)
        ? json.data
        : [];

  return rows.map(mapNotification);
}
