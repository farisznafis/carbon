import type {
  AdminCompanyAccountActionTarget,
  CompanySuspension,
} from '@/api/admin/pendingCompanies';
import { FormActions } from '@/components/forms/form-layout';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

const reasonCodeLabels: Record<CompanySuspension['reasonCode'], string> = {
  BILLING: '請求・支払い',
  POLICY_VIOLATION: 'ポリシー違反',
  SECURITY: 'セキュリティ',
  ADMINISTRATIVE: '管理上の理由',
  OTHER: 'その他',
};

type CompanySuspensionResolveDialogProps = {
  open: boolean;
  item: AdminCompanyAccountActionTarget | null;
  suspensions: CompanySuspension[];
  selectedSuspensionId: number | null;
  resolutionNote: string;
  fieldErrors?: Record<string, string>;
  isLoading?: boolean;
  isSubmitting?: boolean;
  onOpenChange: (open: boolean) => void;
  onResolutionNoteChange: (value: string) => void;
  onConfirm: () => void;
};

export function CompanySuspensionResolveDialog({
  open,
  item,
  suspensions,
  selectedSuspensionId,
  resolutionNote,
  fieldErrors,
  isLoading,
  isSubmitting,
  onOpenChange,
  onResolutionNoteChange,
  onConfirm,
}: CompanySuspensionResolveDialogProps) {
  const selectedSuspension =
    suspensions.find((suspension) => suspension.id === selectedSuspensionId) ??
    null;
  const canConfirm =
    Boolean(selectedSuspension) && resolutionNote.trim().length > 0;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[calc(100vw-2rem)] max-w-[720px] rounded-lg p-8">
        <DialogHeader>
          <DialogTitle className="text-foreground">
            アカウントを有効化しますか？
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-5">
          <div className="space-y-4 rounded-lg bg-muted p-5">
            <div className="space-y-1">
              <p className="text-small text-foreground">企業名</p>
              <p className="break-all text-small font-bold text-foreground">
                {item?.nameJA ?? '-'}
              </p>
            </div>
            <div className="space-y-1">
              <p className="text-small text-foreground">メールアドレス</p>
              <p className="break-all text-small font-bold text-foreground">
                {item?.email ?? '-'}
              </p>
            </div>
          </div>

          <div className="space-y-3">
            <p className="text-small font-bold">有効な停止理由</p>
            {isLoading ? (
              <p className="rounded-lg border border-dashed border-border px-4 py-5 text-small text-foreground">
                読み込み中...
              </p>
            ) : !selectedSuspension ? (
              <p className="rounded-lg border border-dashed border-border px-4 py-5 text-small text-foreground">
                有効な停止理由はありません。
              </p>
            ) : (
              <div className="w-full rounded-lg border border-border bg-card px-4 py-3">
                <p className="text-small font-bold text-foreground">
                  {reasonCodeLabels[selectedSuspension.reasonCode]}
                </p>
                <p className="mt-1 text-small text-foreground">
                  {selectedSuspension.reasonDetail || '-'}
                </p>
              </div>
            )}
            {fieldErrors?.suspensionId && (
              <p className="text-small font-medium text-destructive-strong">
                {fieldErrors.suspensionId}
              </p>
            )}
          </div>

          {selectedSuspension?.reasonCode === 'BILLING' && (
            <div className="rounded-lg bg-muted/40 px-4 py-3 text-small text-foreground">
              支払い関連のアカウント有効化では paymentMethod として CREDIT_CARD を送信します。
            </div>
          )}

          <div className="space-y-2">
            <label htmlFor="resolutionNote" className="text-small font-bold">
              解除メモ <span className="text-destructive-strong">*</span>
            </label>
            <textarea
              id="resolutionNote"
              value={resolutionNote}
              onChange={(event) => onResolutionNoteChange(event.target.value)}
              placeholder="解除理由や確認内容を入力してください。"
              className="min-h-[120px] w-full rounded-md border border-input bg-background px-4 py-3 text-body outline-none ring-offset-background placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            />
            {fieldErrors?.resolutionNote && (
              <p className="text-small font-medium text-destructive-strong">
                {fieldErrors.resolutionNote}
              </p>
            )}
          </div>
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
                label: 'アカウント有効化',
                pendingLabel: '処理中...',
                disabled: !canConfirm || isLoading,
                onClick: onConfirm,
              },
            ]}
          />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
