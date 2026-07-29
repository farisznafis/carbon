import type { UserNotification } from '@/api/notifications';
import { NotificationsList } from '@/components/feedback/NotificationsList';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

type NotificationsCardProps = {
  notifications: UserNotification[];
  loading: boolean;
  error?: string | null;
  title?: string;
  onMarkAsRead?: (
    notificationId: UserNotification['id'],
  ) => void | Promise<void>;
  onDelete?: (notificationId: UserNotification['id']) => void | Promise<void>;
};

export function NotificationsCard({
  notifications,
  loading,
  error,
  title = 'お知らせ',
  onMarkAsRead,
  onDelete,
}: NotificationsCardProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent className='p-8 pt-0'>
        <NotificationsList
          notifications={notifications}
          loading={loading}
          error={error}
          onMarkAsRead={onMarkAsRead}
          onDelete={onDelete}
        />
      </CardContent>
    </Card>
  );
}
