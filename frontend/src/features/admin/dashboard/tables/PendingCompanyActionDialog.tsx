import type { AdminCompanyListItem } from '@/api/admin/pendingCompanies';
import { FormActions } from '@/components/forms/form-layout';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

type PendingCompanyActionMode = 'approve' | 'reject';

type PendingCompanyActionDialogProps = {
  open: boolean;
  mode: PendingCompanyActionMode | null;
  item: AdminCompanyListItem | null;
  rejectReason: string;
  fieldErrors?: Record<string, string>;
  onOpenChange: (open: boolean) => void;
  onRejectReasonChange: (value: string) => void;
  onConfirm: () => void;
  isSubmitting?: boolean;
};

export function PendingCompanyActionDialog({
  open,
  mode,
  item,
  rejectReason,
  fieldErrors,
  onOpenChange,
  onRejectReasonChange,
  onConfirm,
  isSubmitting,
}: PendingCompanyActionDialogProps) {
  const isApprove = mode === 'approve';
  const canConfirm = isApprove || rejectReason.trim().length > 0;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[calc(100vw-2rem)] max-w-[640px] rounded-xl p-8">
        <DialogHeader>
          <DialogTitle className="text-foreground">
            {isApprove
              ? '申請アカウントを承認しますか？'
              : '申請アカウントを却下しますか？'}
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-5">
          <p className="text-body text-foreground">
            {isApprove
              ? '承認すると、申請者のアカウントが作成されます。'
              : 'この操作を行うと、申請者のアカウントは作成されません。却下理由を入力してください。'}
          </p>

          <div className="space-y-4 rounded-xl border border-border bg-card p-5">
            <div className="space-y-1">
              <p className="text-small text-foreground">メールアドレス</p>
              <p className="break-all text-large font-bold text-foreground">
                {item?.email ?? '-'}
              </p>
            </div>
            <div className="space-y-1">
              <p className="text-small text-foreground">企業名</p>
              <p className="break-all text-large font-bold text-foreground">
                {item?.nameJA ?? '-'}
              </p>
            </div>
          </div>

          {!isApprove && (
            <div className="space-y-2">
              <label htmlFor="rejectReason" className="text-small font-bold">
                却下理由 <span className="text-destructive">*</span>
              </label>
              <p className="text-subtle text-foreground">
                この内容は申請者に通知されます。
              </p>
              <textarea
                id="rejectReason"
                value={rejectReason}
                onChange={(event) => onRejectReasonChange(event.target.value)}
                placeholder="例：登録情報に不備があるため、内容を修正して再申請してください。"
                className="min-h-[120px] w-full rounded-md border border-input bg-background px-4 py-3 text-body outline-none ring-offset-background placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              />
              {fieldErrors?.rejectionReason && (
                <p className="text-small font-medium text-destructive">
                  {fieldErrors.rejectionReason}
                </p>
              )}
            </div>
          )}
        </div>

        <DialogFooter className="mt-8">
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
                label: isApprove ? '承認する' : '申請を却下',
                pendingLabel: '処理中...',
                variant: isApprove ? 'success' : 'destructive',
                disabled: !canConfirm,
                onClick: onConfirm,
              },
            ]}
          />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default PendingCompanyActionDialog;
