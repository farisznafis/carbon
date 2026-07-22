import type {
  AdminCompanyAccountActionTarget,
  SuspendCompanyPayload,
} from '@/api/admin/pendingCompanies';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';

type CompanySuspendDialogProps = {
  open: boolean;
  item: AdminCompanyAccountActionTarget | null;
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
            アカウントを停止しますか？
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-5">
          <div className="space-y-4 rounded-xl border border-border bg-card p-5">
            <div className="space-y-1">
              <p className="text-small text-muted-foreground">企業名</p>
              <p className="break-all text-small font-bold text-foreground">
                {item?.nameJA ?? '-'}
              </p>
            </div>
            <div className="space-y-1">
              <p className="text-small text-muted-foreground">メールアドレス</p>
              <p className="break-all text-small font-bold text-foreground">
                {item?.email ?? '-'}
              </p>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="suspensionReasonDetail" className="text-small font-bold">
              停止理由 <span className="text-destructive">*</span>
            </Label>
            <Textarea
              id="suspensionReasonDetail"
              value={payload.reasonDetail}
              onChange={(event) =>
                onPayloadChange({ ...payload, reasonDetail: event.target.value })
              }
              placeholder="停止理由の詳細を入力してください。"
              className="min-h-[120px] resize-y"
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
            アカウント停止
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
