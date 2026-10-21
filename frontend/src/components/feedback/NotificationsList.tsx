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
import { SkeletonList } from './loading';
import { NotificationDetailDialog } from './NotificationDetailDialog';
import { formatNotificationDate, formatRelativeTime } from './notificationFormatters';

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

  // A refetch keeps the current items on screen instead of blanking the list.
  if (loading && notifications.length === 0) {
    return <SkeletonList items={3} />;
  }

  if (error) {
    return (
      <p role='alert' className='py-6 text-center text-small text-destructive-strong'>
        {error}
      </p>
    );
  }

  if (notifications.length === 0) {
    return (
      <p className='py-6 text-center text-small text-muted-foreground'>
        お知らせはありません
      </p>
    );
  }

  return (
    <>
      {/* Pulled out by the row padding so text lines up with the surrounding heading. */}
      <ul className='-mx-3 divide-y divide-border'>
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
              className='relative px-3 py-3'
            >
              {/* The row's hover fill lives on the button, so the divider above stays a straight line. */}
              <Button
                type='button'
                variant='ghost'
                aria-label={`「${notificationTitle}」の詳細を表示`}
                className='absolute inset-0 z-0 h-full w-full rounded-lg p-0 hover:bg-muted/60 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2'
                onClick={(event) =>
                  handleOpenNotification(notification, event.currentTarget)
                }
              />

              <div className='pointer-events-none relative z-[1] flex min-w-0 gap-2.5'>
                {/* Read rows keep the dot's slot so every title starts on the same line. */}
                <span
                  className={cn(
                    'mt-2 h-2 w-2 shrink-0 rounded-full',
                    isRead ? 'invisible' : 'bg-primary',
                  )}
                  {...(isRead ? { 'aria-hidden': true } : { 'aria-label': '未読' })}
                />

                <div className='min-w-0 flex-1'>
                  <div className='flex items-start justify-between gap-3'>
                    <p
                      className={cn(
                        'min-w-0 break-words text-body',
                        isRead ? 'font-normal' : 'font-bold',
                      )}
                    >
                      {notificationTitle}
                    </p>

                    {onDelete && (
                      <div className='pointer-events-auto relative z-10 -my-1 -mr-1'>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button
                              type='button'
                              variant='ghost'
                              size='icon'
                              className='h-8 w-8 shrink-0 text-muted-foreground'
                              disabled={isProcessing}
                              aria-label='通知メニュー'
                            >
                              <MoreHorizontal className='h-4 w-4' />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align='end'>
                            <DropdownMenuItem
                              className='text-destructive-strong focus:text-destructive-strong'
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

                  <p className='mt-0.5 line-clamp-2 break-words text-small text-muted-foreground'>
                    {notification.message || '-'}
                  </p>

                  <time
                    dateTime={notification.createdAt ?? undefined}
                    title={formatNotificationDate(notification.createdAt)}
                    className='mt-1 block text-subtle tabular-nums text-muted-foreground'
                  >
                    {formatRelativeTime(notification.createdAt)}
                  </time>
                </div>
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
