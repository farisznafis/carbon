import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { FormActions } from '@/components/forms/form-layout';

interface AdminMasterPasswordResetDialogProps {
  open: boolean;
  masterName: string;
  masterEmail: string;
  isSubmitting: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
}

export function AdminMasterPasswordResetDialog({
  open,
  masterName,
  masterEmail,
  isSubmitting,
  onOpenChange,
  onConfirm,
}: AdminMasterPasswordResetDialogProps) {
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent className="w-[calc(100vw-2rem)] max-w-md rounded-xl">
        <AlertDialogHeader>
          <AlertDialogTitle>パスワードリセットの確認</AlertDialogTitle>
          <AlertDialogDescription>
            以下のマスターアカウントへパスワードリセット用リンクを送信します。
          </AlertDialogDescription>
        </AlertDialogHeader>

        <dl className="rounded-lg border border-border bg-muted/30 px-4 py-2">
          <div className="grid gap-4 border-b border-border/60 py-2 sm:grid-cols-[100px_1fr]">
            <dt className="text-sm text-foreground">氏名</dt>
            <dd className="break-words text-sm font-medium text-foreground">
              {masterName || '―'}
            </dd>
          </div>
          <div className="grid gap-4 py-2 sm:grid-cols-[100px_1fr]">
            <dt className="text-sm text-foreground">メールアドレス</dt>
            <dd className="break-all text-sm font-medium text-foreground">
              {masterEmail || '―'}
            </dd>
          </div>
        </dl>

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
                label: '送信する',
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
