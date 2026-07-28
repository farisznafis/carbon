import type { UserNotification } from '@/api/users';
import { NotificationsList } from '@/components/feedback/NotificationsList';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

type NotificationsCardProps = {
  notifications: UserNotification[];
  loading: boolean;
  error?: string | null;
  title?: string;
};

export function NotificationsCard({
  notifications,
  loading,
  error,
  title = 'お知らせ',
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
        />
      </CardContent>
    </Card>
  );
}
