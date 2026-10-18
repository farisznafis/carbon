import { apiRequest, apiVoidRequest } from '@/api/_shared/httpClient';

export type UserNotification = {
  id: string | number;
  title: string;
  message: string;
  read: boolean;
  operatingUserId: number | null;
  participatingUserId: number | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type NotificationPagination = {
  page: number;
  limit: number;
  totalItems: number;
  totalPages: number;
  nextPageToken: string | null;
  hasMore: boolean;
};

export type NotificationListParams = {
  unreadOnly?: boolean;
  page?: number;
  size?: number;
};

export type NotificationListResult = {
  notifications: UserNotification[];
  pagination: NotificationPagination | null;
};

export type InquiryRequest = {
  companyName: string;
  name: string;
  email: string;
  content: string;
};

export type InquiryResponse = {
  statusCode: string;
  message: string;
};

type NotificationResponseItem = {
  id?: string | number | null;
  notificationId?: string | number | null;
  title?: string | null;
  message?: string | null;
  read?: boolean | null;
  isRead?: boolean | null;
  operatingUserId?: number | null;
  participatingUserId?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type NotificationsResponse = {
  statusCode?: string;
  notifications?: NotificationResponseItem[];
  pagination?: Partial<NotificationPagination> | null;
};

type NotificationCountResponse = {
  statusCode?: string;
  unreadCount?: number | null;
};

const DEFAULT_PAGE = 0;
const DEFAULT_PAGE_SIZE = 10;

function mapNotification(
  item: NotificationResponseItem,
  index: number,
): UserNotification {
  return {
    id: item.id ?? item.notificationId ?? index,
    title: item.title ?? '',
    message: item.message ?? '',
    read: Boolean(item.read ?? item.isRead),
    operatingUserId: item.operatingUserId ?? null,
    participatingUserId: item.participatingUserId ?? null,
    createdAt: item.createdAt ?? null,
    updatedAt: item.updatedAt ?? null,
  };
}

function mapPagination(
  pagination: NotificationsResponse['pagination'],
): NotificationPagination | null {
  if (!pagination) return null;

  return {
    page: pagination.page ?? DEFAULT_PAGE,
    limit: pagination.limit ?? DEFAULT_PAGE_SIZE,
    totalItems: pagination.totalItems ?? 0,
    totalPages: pagination.totalPages ?? 0,
    nextPageToken: pagination.nextPageToken ?? null,
    hasMore: pagination.hasMore ?? false,
  };
}

export async function getNotifications(
  params: NotificationListParams = {},
): Promise<NotificationListResult> {
  const searchParams = new URLSearchParams({
    unreadOnly: String(params.unreadOnly ?? false),
    page: String(params.page ?? DEFAULT_PAGE),
    size: String(params.size ?? DEFAULT_PAGE_SIZE),
  });
  const response = await apiRequest<NotificationsResponse>(
    `/notification/list?${searchParams.toString()}`,
    {
      method: 'GET',
      forbiddenHandling: 'caller',
      fallbackErrorMessage: 'Failed to fetch notifications',
    },
  );

  return {
    notifications: (response.notifications ?? []).map(mapNotification),
    pagination: mapPagination(response.pagination),
  };
}

export async function getUnreadNotificationCount(): Promise<number> {
  const response = await apiRequest<NotificationCountResponse>(
    '/notification/count',
    {
      method: 'GET',
      forbiddenHandling: 'caller',
      fallbackErrorMessage: 'Failed to fetch unread notification count',
    },
  );

  return Math.max(0, response.unreadCount ?? 0);
}

export async function markNotificationAsRead(
  notificationId: UserNotification['id'],
): Promise<void> {
  await apiVoidRequest(
    `/notification/${encodeURIComponent(String(notificationId))}/read`,
    {
      method: 'PATCH',
      fallbackErrorMessage: 'Failed to mark notification as read',
    },
  );
}

export async function deleteNotification(
  notificationId: UserNotification['id'],
): Promise<void> {
  await apiVoidRequest(
    `/notification/${encodeURIComponent(String(notificationId))}/delete`,
    {
      method: 'PATCH',
      fallbackErrorMessage: 'Failed to delete notification',
    },
  );
}

/** POST /notification/inquiry — sends a public inquiry from the contact page. */
export async function createInquiry(
  inquiry: InquiryRequest,
): Promise<InquiryResponse> {
  return apiRequest<InquiryResponse>('/notification/inquiry', {
    method: 'POST',
    auth: false,
    body: inquiry,
    fallbackErrorMessage: 'お問い合わせを送信できませんでした',
  });
}
