import type { Project } from '@/api/projects';
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import { CheckCircle2 } from 'lucide-react';

type ProjectCompletionDialogProps = {
  project: Project | null;
  isSubmitting: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
};

function displayValue(value?: string | null) {
  return value?.trim() || '―';
}

export function ProjectCompletionDialog({
  project,
  isSubmitting,
  onOpenChange,
  onConfirm,
}: ProjectCompletionDialogProps) {
  return (
    <AlertDialog
      open={Boolean(project)}
      onOpenChange={(open) => {
        if (!isSubmitting) onOpenChange(open);
      }}
    >
      <AlertDialogContent className="max-w-lg">
        <AlertDialogHeader className="space-y-4">
          <div className="flex items-start gap-3">
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
              <CheckCircle2 className="h-5 w-5" aria-hidden="true" />
            </span>
            <AlertDialogTitle className="pt-1 text-left">
              ステータスを「終了」に変更しますか？
            </AlertDialogTitle>
          </div>
          <AlertDialogDescription className="space-y-1 text-left leading-6">
            <span className="block">
              この操作を行うと、この申請のステータスは「終了」になります。
            </span>
            <span className="block font-bold text-foreground">
              終了後はステータスを変更できません。
            </span>
          </AlertDialogDescription>
        </AlertDialogHeader>

        <section
          aria-labelledby="completion-target-title"
          className="rounded-lg border border-border bg-muted/30 p-4"
        >
          <h3
            id="completion-target-title"
            className="mb-3 text-small font-bold text-foreground"
          >
            対象申請
          </h3>
          <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-2 text-small">
            <dt className="text-foreground">会社名：</dt>
            <dd className="break-words text-foreground">
              {displayValue(project?.companyName)}
            </dd>
            <dt className="text-foreground">プロジェクト名：</dt>
            <dd className="break-words text-foreground">
              {displayValue(project?.projectName)}
            </dd>
          </dl>
        </section>

        <AlertDialogFooter>
          <AlertDialogCancel disabled={isSubmitting}>
            キャンセル
          </AlertDialogCancel>
          <Button
            type="button"
            disabled={isSubmitting}
            onClick={onConfirm}
          >
            {isSubmitting ? '変更中...' : '終了に変更する'}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
