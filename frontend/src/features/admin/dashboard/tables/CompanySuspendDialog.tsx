import type {
  AdminCompanyListItem,
  SuspendCompanyPayload,
  SuspensionReasonCode,
} from '@/api/admin/pendingCompanies';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

const suspensionReasonOptions: Array<{
  value: SuspensionReasonCode;
  label: string;
}> = [
  { value: 'BILLING', label: '請求・支払い' },
  { value: 'POLICY_VIOLATION', label: 'ポリシー違反' },
  { value: 'SECURITY', label: 'セキュリティ' },
  { value: 'ADMINISTRATIVE', label: '管理上の理由' },
  { value: 'OTHER', label: 'その他' },
];

type CompanySuspendDialogProps = {
  open: boolean;
  item: AdminCompanyListItem | null;
  payload: SuspendCompanyPayload;
  fieldErrors?: Record<string, string>;
  isSubmitting?: boolean;
  onOpenChange: (open: boolean) => void;
  onPayloadChange: (payload: SuspendCompanyPayload) => void;
  onConfirm: () => void;
};

export function CompanySuspendDialog({
  open,
  item,
  payload,
  fieldErrors,
  isSubmitting,
  onOpenChange,
  onPayloadChange,
  onConfirm,
}: CompanySuspendDialogProps) {
  const canConfirm = payload.reasonDetail.trim().length > 0;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="w-[calc(100vw-2rem)] max-w-[640px] rounded-xl p-8">
        <DialogHeader>
          <DialogTitle className="text-foreground">
            企業アカウントを停止しますか？
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-5">
          <div className="space-y-4 rounded-xl border border-border bg-card p-5">
            <div className="space-y-1">
              <p className="text-small text-muted-foreground">企業名</p>
              <p className="break-all text-large font-bold text-foreground">
                {item?.nameJA ?? '-'}
              </p>
            </div>
            <div className="space-y-1">
              <p className="text-small text-muted-foreground">メールアドレス</p>
              <p className="break-all text-large font-bold text-foreground">
                {item?.email ?? '-'}
              </p>
            </div>
          </div>

          <div className="space-y-2">
            <label htmlFor="suspensionReasonCode" className="text-small font-bold">
              停止理由 <span className="text-destructive">*</span>
            </label>
            <select
              id="suspensionReasonCode"
              value={payload.reasonCode}
              onChange={(event) =>
                onPayloadChange({
                  ...payload,
                  reasonCode: event.target.value as SuspensionReasonCode,
                })
              }
              className="h-12 w-full rounded-md border border-input bg-background px-4 text-body outline-none ring-offset-background focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              {suspensionReasonOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {fieldErrors?.reasonCode && (
              <p className="text-small font-medium text-destructive">
                {fieldErrors.reasonCode}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <label htmlFor="suspensionReasonDetail" className="text-small font-bold">
              詳細理由 <span className="text-destructive">*</span>
            </label>
            <textarea
              id="suspensionReasonDetail"
              value={payload.reasonDetail}
              onChange={(event) =>
                onPayloadChange({ ...payload, reasonDetail: event.target.value })
              }
              placeholder="停止理由の詳細を入力してください。"
              className="min-h-[120px] w-full rounded-md border border-input bg-background px-4 py-3 text-body outline-none ring-offset-background placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            />
            {fieldErrors?.reasonDetail && (
              <p className="text-small font-medium text-destructive">
                {fieldErrors.reasonDetail}
              </p>
            )}
          </div>
        </div>

        <DialogFooter className="mt-8 gap-3 sm:justify-end">
          <Button
            type="button"
            variant="outline"
            size="md"
            onClick={() => onOpenChange(false)}
          >
            キャンセル
          </Button>
          <Button
            type="button"
            variant="destructive"
            size="md"
            onClick={onConfirm}
            disabled={!canConfirm || isSubmitting}
          >
            停止する
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
