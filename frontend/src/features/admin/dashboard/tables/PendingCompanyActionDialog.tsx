import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import type { PendingCompanyItem } from '@/api/admin/pendingCompanies';

type PendingCompanyActionMode = 'approve' | 'reject';

type PendingCompanyActionDialogProps = {
  open: boolean;
  mode: PendingCompanyActionMode | null;
  item: PendingCompanyItem | null;
  rejectReason: string;
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
  onOpenChange,
  onRejectReasonChange,
  onConfirm,
}: PendingCompanyActionDialogProps) {
  const isApprove = mode === 'approve';
  const canConfirm = isApprove || rejectReason.trim().length > 0;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md w-[92%] rounded-xl">
        <DialogHeader>
          <DialogTitle>
            {isApprove ? '申請アカウント承認確認' : '申請アカウント却下確認'}
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          <div className="rounded-lg border bg-slate-50 p-4 space-y-2">
            <div>
              <div className="text-xs text-muted-foreground">メールアドレス</div>
              <div className="font-medium break-all">{item?.email ?? '-'}</div>
            </div>
            <div>
              <div className="text-xs text-muted-foreground">企業名</div>
              <div className="font-medium break-all">{item?.nameJA ?? '-'}</div>
            </div>
          </div>

          <div className="text-sm text-muted-foreground">
            {isApprove
              ? 'このアカウントを承認しますか？'
              : 'このアカウントを却下しますか？'}
          </div>

          {!isApprove && (
            <div className="space-y-2">
              <label
                htmlFor="rejectReason"
                className="text-sm font-medium leading-none"
              >
                拒否理由
              </label>
              <textarea
                id="rejectReason"
                value={rejectReason}
                onChange={(e) => onRejectReasonChange(e.target.value)}
                placeholder="拒否理由を入力してください"
                className="min-h-[120px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none ring-offset-background placeholder:text-muted-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              />
            </div>
          )}
        </div>

        <DialogFooter className="mt-4 flex justify-end gap-3">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            戻る
          </Button>
          <Button onClick={onConfirm} disabled={!canConfirm}>
            {isApprove ? '承認する' : '却下する'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default PendingCompanyActionDialog;