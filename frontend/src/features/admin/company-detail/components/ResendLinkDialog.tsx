import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { FormActions } from '@/components/forms/form-layout';

interface ResendLinkDialogProps {
  open: boolean;
  title: string;
  description: string;
  email: string;
  isSubmitting: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
}

/** Confirms a link resend; every resend invalidates the link sent before. */
export function ResendLinkDialog({
  open,
  title,
  description,
  email,
  isSubmitting,
  onOpenChange,
  onConfirm,
}: Readonly<ResendLinkDialogProps>) {
  return (
    <AlertDialog open={open} onOpenChange={(next) => !isSubmitting && onOpenChange(next)}>
      <AlertDialogContent className="w-[calc(100vw-2rem)] max-w-md rounded-lg">
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>

        <dl className="rounded-lg border border-border bg-muted/30 px-4 py-2">
          <div className="grid gap-4 py-2 sm:grid-cols-[100px_1fr]">
            <dt className="text-sm text-foreground">送信先</dt>
            <dd className="break-all text-sm font-medium text-foreground">{email || '―'}</dd>
          </div>
        </dl>

        <p className="text-sm font-medium text-destructive-strong">
          以前に送信したリンクは無効になります。
        </p>

        <AlertDialogFooter>
          <FormActions
            context="compact"
            className="w-full"
            disabled={isSubmitting}
            pendingActionId={isSubmitting ? 'confirm' : null}
            actions={[
              {
                id: 'cancel',
                label: 'キャンセル',
                variant: 'outline',
                onClick: () => onOpenChange(false),
              },
              {
                id: 'confirm',
                label: '再送信する',
                pendingLabel: '送信中...',
                onClick: onConfirm,
              },
            ]}
          />
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
