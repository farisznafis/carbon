import { FormActions } from '@/components/forms/form-layout';
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import type { ProjectFileVersion } from './projectFileTypes';

type ProjectFileDeleteDialogProps = Readonly<{
  typeLabel: string;
  file: ProjectFileVersion | null;
  isDeleting: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: (file: ProjectFileVersion) => void;
}>;

/** Asks before a version is removed: the UI offers no way to restore it. */
export function ProjectFileDeleteDialog({
  typeLabel,
  file,
  isDeleting,
  onOpenChange,
  onConfirm,
}: ProjectFileDeleteDialogProps) {
  return (
    <AlertDialog
      open={Boolean(file)}
      onOpenChange={(open) => {
        if (!isDeleting) onOpenChange(open);
      }}
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>ファイルを削除しますか？</AlertDialogTitle>
          <AlertDialogDescription className="break-all">
            {typeLabel} v{file?.version}「{file?.originalFileName ?? '—'}」を削除します。
            この操作は取り消せません。
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <FormActions
            context="compact"
            className="w-full"
            disabled={isDeleting}
            pendingActionId={isDeleting ? 'delete' : null}
            actions={[
              {
                id: 'cancel',
                label: 'キャンセル',
                variant: 'outline',
                onClick: () => onOpenChange(false),
              },
              {
                id: 'delete',
                label: '削除する',
                pendingLabel: '削除中…',
                variant: 'destructive',
                onClick: () => file && onConfirm(file),
              },
            ]}
          />
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
