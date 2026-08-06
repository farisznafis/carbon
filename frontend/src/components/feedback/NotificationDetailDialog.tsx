import type { UserNotification } from '@/api/notifications';
import type { RefObject } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { formatNotificationDate } from './notificationFormatters';

type NotificationDetailDialogProps = {
  notification: UserNotification | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  returnFocusRef: RefObject<HTMLButtonElement>;
};

export function NotificationDetailDialog({
  notification,
  open,
  onOpenChange,
  returnFocusRef,
}: Readonly<NotificationDetailDialogProps>) {
  if (!notification) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className='max-h-[90vh] w-[calc(100vw-2rem)] max-w-lg overflow-y-auto rounded-xl p-6 sm:p-8'
        onCloseAutoFocus={(event) => {
          if (returnFocusRef.current) {
            event.preventDefault();
            returnFocusRef.current.focus();
          }
        }}
      >
        <DialogHeader className='space-y-2'>
          <DialogTitle className='break-words text-foreground'>
            {notification.title || 'お知らせ'}
          </DialogTitle>
          <DialogDescription>
            {formatNotificationDate(notification.createdAt)}
          </DialogDescription>
        </DialogHeader>

        <div className='rounded-xl border border-border bg-card p-4'>
          <p className='whitespace-pre-wrap break-words text-body text-foreground'>
            {notification.message || '-'}
          </p>
        </div>
      </DialogContent>
    </Dialog>
  );
}
