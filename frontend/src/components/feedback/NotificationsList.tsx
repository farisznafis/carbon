import { useEffect, useRef, useState } from 'react';
import type { UserNotification } from '@/api/users';
import { Badge } from '@/components/ui/badge';

type NotificationsListProps = {
  notifications: UserNotification[];
  loading: boolean;
  error?: string | null;
  onMarkAsRead?: (notificationId: UserNotification['id']) => void;
};

export function NotificationsList({
  notifications,
  loading,
  error,
  onMarkAsRead,
}: NotificationsListProps) {
  const [fadingNotificationIds, setFadingNotificationIds] = useState<
    Array<UserNotification['id']>
  >([]);
  const timeoutRefs = useRef<Array<ReturnType<typeof setTimeout>>>([]);

  useEffect(() => {
    const timeouts = timeoutRefs.current;

    return () => {
      timeouts.forEach(clearTimeout);
    };
  }, []);

  const markAsReadWithFade = (notificationId: UserNotification['id']) => {
    setFadingNotificationIds((current) =>
      current.includes(notificationId) ? current : [...current, notificationId],
    );

    const timeout = setTimeout(() => {
      onMarkAsRead?.(notificationId);
      setFadingNotificationIds((current) =>
        current.filter((id) => id !== notificationId),
      );
    }, 250);

    timeoutRefs.current.push(timeout);
  };

  if (loading) {
    return <div className='text-sm text-muted-foreground'>読み込み中...</div>;
  }

  if (error) {
    return <div className='text-sm text-destructive'>{error}</div>;
  }

  if (notifications.length === 0) {
    return (
      <div className='text-sm text-muted-foreground'>
        お知らせはありません。
      </div>
    );
  }

  return (
    <ul className='divide-y rounded-lg border'>
      {notifications.map((notification) => {
        const isFading = fadingNotificationIds.includes(notification.id);
        const isRead = notification.read;

        return (
          <li
            key={notification.id}
            className={`flex flex-col gap-2 p-4 transition-colors duration-300 sm:flex-row sm:items-start sm:justify-between ${
              isRead ? 'bg-muted/50' : ''
            }`}
          >
            <div className='min-w-0 space-y-1'>
              <p
                className={`font-semibold break-words transition-colors duration-300 ${
                  isRead ? 'text-muted-foreground' : ''
                }`}
              >
                {notification.title || '-'}
              </p>
              <p
                className={`text-sm break-words transition-colors duration-300 ${
                  isRead ? 'text-muted-foreground/80' : 'text-muted-foreground'
                }`}
              >
                {notification.message || '-'}
              </p>
            </div>
            <div className='flex min-w-12 shrink-0 justify-start sm:justify-end'>
              {!isRead && (
                <button
                  type='button'
                  aria-label='既読にする'
                  disabled={isFading}
                  onClick={() => markAsReadWithFade(notification.id)}
                  className={`w-fit shrink-0 transition-opacity duration-300 disabled:cursor-default ${
                    isFading ? 'opacity-0' : 'opacity-100'
                  }`}
                >
                  <Badge className='cursor-pointer'>
                    未読
                  </Badge>
                </button>
              )}
            </div>
          </li>
        );
      })}
    </ul>
  );
}
