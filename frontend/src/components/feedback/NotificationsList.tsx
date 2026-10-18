import { useMemo, useRef, useState } from 'react';
import { MoreHorizontal, Trash2 } from 'lucide-react';
import type { UserNotification } from '@/api/notifications';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { cn } from '@/lib/utils';
import { NotificationDetailDialog } from './NotificationDetailDialog';
import { formatNotificationDate } from './notificationFormatters';

type NotificationAction = (
  notificationId: UserNotification['id'],
) => void | Promise<void>;

type NotificationsListProps = {
  notifications: UserNotification[];
  loading: boolean;
  error?: string | null;
  onMarkAsRead?: NotificationAction;
  onDelete?: NotificationAction;
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
  const [selectedNotificationId, setSelectedNotificationId] = useState<
    UserNotification['id'] | null
  >(null);
  const selectedNotificationTriggerRef = useRef<HTMLButtonElement>(null);

  const orderedNotifications = useMemo(
    () =>
      notifications
        .map((notification, index) => ({ notification, index }))
        .sort(
          (left, right) =>
            Number(left.notification.read) -
              Number(right.notification.read) ||
            left.index - right.index,
        )
        .map(({ notification }) => notification),
    [notifications],
  );

  const selectedNotification =
    notifications.find(
      (notification) => notification.id === selectedNotificationId,
    ) ?? null;

  const runNotificationAction = async (
    notificationId: UserNotification['id'],
    action: NotificationAction | undefined,
  ) => {
    if (!action) return;

    setProcessingNotificationIds((current) =>
      current.includes(notificationId) ? current : [...current, notificationId],
    );

    try {
      await action(notificationId);
    } catch {
      // Error feedback is handled by the action owner.
    } finally {
      setProcessingNotificationIds((current) =>
        current.filter((id) => id !== notificationId),
      );
    }
  };

  const handleOpenNotification = (
    notification: UserNotification,
    trigger: HTMLButtonElement,
  ) => {
    selectedNotificationTriggerRef.current = trigger;
    setSelectedNotificationId(notification.id);

    if (
      !notification.read &&
      !processingNotificationIds.includes(notification.id)
    ) {
      void runNotificationAction(notification.id, onMarkAsRead);
    }
  };

  if (loading) {
    return <div className='text-sm text-foreground'>読み込み中...</div>;
  }

  if (error) {
    return <div className='text-sm text-destructive'>{error}</div>;
  }

  if (notifications.length === 0) {
    return (
      <div className='text-sm text-foreground ml-2'>
        お知らせはありません。
      </div>
    );
  }

  return (
    <>
      <ul className='space-y-3'>
        {orderedNotifications.map((notification) => {
          const isProcessing = processingNotificationIds.includes(
            notification.id,
          );
          const isRead = notification.read;
          const notificationTitle = notification.title || 'お知らせ';

          return (
            <li
              key={notification.id}
              aria-busy={isProcessing || undefined}
              className={cn(
                'relative rounded-lg border border-border p-4 shadow-app-sm transition-colors hover:border-primary/50 hover:bg-secondary/20',
                isRead ? 'bg-muted/40' : 'bg-card',
              )}
            >
              <Button
                type='button'
                variant='ghost'
                aria-label={`「${notificationTitle}」の詳細を表示`}
                className='absolute inset-0 z-0 h-full w-full rounded-lg p-0 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2'
                onClick={(event) =>
                  handleOpenNotification(notification, event.currentTarget)
                }
              />

              <div className='pointer-events-none relative z-[1] min-w-0 space-y-2'>
                <div className='flex items-start justify-between gap-3'>
                  <div className='flex min-w-0 items-start gap-2'>
                    {!isRead && (
                      <span
                        className='mt-2 h-2 w-2 shrink-0 rounded-full bg-primary'
                        aria-label='未読'
                      />
                    )}
                    <div className='min-w-0'>
                      <p
                        className={cn(
                          'break-words leading-6',
                          isRead ? 'font-normal' : 'font-bold',
                        )}
                      >
                        {notificationTitle}
                      </p>
                      <time
                        dateTime={notification.createdAt ?? undefined}
                        className='block text-xs text-foreground'
                      >
                        {formatNotificationDate(notification.createdAt)}
                      </time>
                    </div>
                  </div>

                  {onDelete && (
                    <div className='pointer-events-auto relative z-10'>
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
                              void runNotificationAction(
                                notification.id,
                                onDelete,
                              )
                            }
                          >
                            <Trash2 className='mr-2 h-4 w-4' />
                            削除
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  )}
                </div>

                <p className='line-clamp-2 break-words text-sm leading-6 text-foreground'>
                  {notification.message || '-'}
                </p>
              </div>
            </li>
          );
        })}
      </ul>

      <NotificationDetailDialog
        notification={selectedNotification}
        open={selectedNotification !== null}
        returnFocusRef={selectedNotificationTriggerRef}
        onOpenChange={(open) => {
          if (!open) {
            setSelectedNotificationId(null);
          }
        }}
      />
    </>
  );
}
