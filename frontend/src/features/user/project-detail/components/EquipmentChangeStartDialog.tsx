import type { SiteProjectEquipment } from '@/api/site';
import { FormActions } from '@/components/forms/form-layout';
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { RefreshCw } from 'lucide-react';

type EquipmentChangeStartDialogProps = {
  equipment: SiteProjectEquipment | null;
  onOpenChange: (open: boolean) => void;
  onConfirm: (equipmentId: number) => void;
};

/**
 * Confirms the history-preserving equipment replacement flow before opening its form.
 */
export function EquipmentChangeStartDialog({
  equipment,
  onOpenChange,
  onConfirm,
}: EquipmentChangeStartDialogProps) {
  const equipmentId = equipment?.equipmentId;
  const equipmentName =
    equipment?.deviceNo?.trim() ||
    equipment?.pcsModel?.trim() ||
    equipment?.pcsManufacturer?.trim() ||
    (typeof equipmentId === 'number' ? `設備 ${equipmentId}` : '―');
  const pcsIdentity = [
    equipment?.pcsManufacturer?.trim(),
    equipment?.pcsModel?.trim(),
  ]
    .filter(Boolean)
    .join(' / ');

  return (
    <AlertDialog
      open={equipment !== null}
      onOpenChange={onOpenChange}
    >
      <AlertDialogContent className="max-w-lg">
        <AlertDialogHeader className="space-y-4">
          <div className="flex items-start gap-3">
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary text-primary">
              <RefreshCw className="h-5 w-5" aria-hidden="true" />
            </span>
            <AlertDialogTitle className="pt-1 text-left">
              設備を変更しますか？
            </AlertDialogTitle>
          </div>
          <AlertDialogDescription className="space-y-2 text-left leading-6">
            <span className="block">
              変更すると現在の設備情報は履歴として保存され、新しい設備として登録されます。よろしいですか？
            </span>
            <span className="block text-muted-foreground">
              変更前後の設備は、それぞれのモニタリング期間に紐づいて管理されます。
            </span>
          </AlertDialogDescription>
        </AlertDialogHeader>

        <section
          aria-labelledby="equipment-change-target-title"
          className="rounded-lg border border-border bg-muted/30 p-4"
        >
          <h3
            id="equipment-change-target-title"
            className="mb-2 text-small font-bold text-foreground"
          >
            対象設備
          </h3>
          <p className="break-words font-bold text-foreground">
            {equipmentName}
          </p>
          {pcsIdentity ? (
            <p className="mt-1 break-words text-small text-muted-foreground">
              {pcsIdentity}
            </p>
          ) : null}
        </section>

        <AlertDialogFooter>
          <FormActions
            context="compact"
            className="w-full"
            actions={[
              {
                id: 'cancel',
                label: 'キャンセル',
                variant: 'outline',
                onClick: () => onOpenChange(false),
              },
              {
                id: 'continue',
                label: '変更内容を入力する',
                onClick: () => {
                  if (typeof equipmentId !== 'number') return;
                  onOpenChange(false);
                  onConfirm(equipmentId);
                },
              },
            ]}
          />
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
