import { useState } from 'react';
import { MoreHorizontal, Trash2 } from 'lucide-react';
import type { UserNotification } from '@/api/notifications';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { formatNotificationDate } from './notificationFormatters';

type NotificationsListProps = {
  notifications: UserNotification[];
  loading: boolean;
  error?: string | null;
  onMarkAsRead?: (
    notificationId: UserNotification['id'],
  ) => void | Promise<void>;
  onDelete?: (notificationId: UserNotification['id']) => void | Promise<void>;
};

export function NotificationsList({
  notifications,
  loading,
  error,
  onMarkAsRead,
  onDelete,
}: NotificationsListProps) {
  const [processingNotificationIds, setProcessingNotificationIds] = useState<
    Array<UserNotification['id']>
  >([]);

  const runNotificationAction = async (
    notificationId: UserNotification['id'],
    action: NotificationsListProps['onMarkAsRead'],
  ) => {
    if (!action) return;

    setProcessingNotificationIds((current) =>
      current.includes(notificationId) ? current : [...current, notificationId],
    );

    try {
      await action(notificationId);
    } finally {
      setProcessingNotificationIds((current) =>
        current.filter((id) => id !== notificationId),
      );
    }
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
    <ul className='space-y-3'>
      {notifications.map((notification) => {
        const isProcessing = processingNotificationIds.includes(
          notification.id,
        );
        const isRead = notification.read;

        return (
          <li
            key={notification.id}
            className='space-y-3 rounded-xl border border-border bg-card p-4 shadow-app-sm'
          >
            <div className='min-w-0 space-y-2'>
              <div className='flex items-center justify-between gap-3'>
                <div className='flex min-w-0 items-start flex-col'>
                  {!isRead && (
                    <span
                      className='mt-2 h-2 w-2 shrink-0 rounded-full bg-primary'
                      aria-label='未読'
                    />
                  )}
                  <p className='break-words font-semibold leading-6'>
                    {notification.title || '-'}
                  </p>
                  <time
                    dateTime={notification.createdAt ?? undefined}
                    className='block text-xs text-muted-foreground'
                  >
                    {formatNotificationDate(notification.createdAt)}
                  </time>
                </div>

                {onDelete && (
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        type='button'
                        variant='ghost'
                        size='icon'
                        className='h-8 w-8 shrink-0'
                        disabled={isProcessing}
                        aria-label='通知メニュー'
                      >
                        <MoreHorizontal className='h-4 w-4' />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align='end'>
                      <DropdownMenuItem
                        className='text-destructive focus:text-destructive'
                        onSelect={() =>
                          void runNotificationAction(notification.id, onDelete)
                        }
                      >
                        <Trash2 className='mr-2 h-4 w-4' />
                        削除
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                )}
              </div>
              <p className='break-words text-sm leading-6 text-muted-foreground'>
                {notification.message || '-'}
              </p>
            </div>

            {!isRead && (
              <Button
                type='button'
                variant='link'
                size='sm'
                className='h-auto p-0'
                disabled={isProcessing}
                onClick={() =>
                  void runNotificationAction(notification.id, onMarkAsRead)
                }
              >
                既読にする
              </Button>
            )}
          </li>
        );
      })}
    </ul>
  );
}
