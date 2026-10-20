import { AlertCircle, Check, Clock3 } from 'lucide-react';
import type { HTMLAttributes } from 'react';
import { Progress } from '@/components/ui/progress';
import { cn } from '@/lib/utils';
import { InlineLoader } from './indicators';
import { ActionButton } from "@/components/actions";

export type FileUploadItemStatus = 'queued' | 'uploading' | 'success' | 'error';

export type FileUploadProgressItem = {
  id: string;
  fileName: string;
  status: FileUploadItemStatus;
  progress: number | null;
  errorMessage?: string;
};

type FileUploadProgressProps = HTMLAttributes<HTMLDivElement> & {
  items: FileUploadProgressItem[];
  onRetry?: (itemId: string) => void;
};

/** Returns the Japanese upload status copy for one file. */
function getUploadStatusText(item: FileUploadProgressItem) {
  if (item.status === 'queued') return 'アップロード待機中';
  if (item.status === 'success') return 'アップロード完了';
  if (item.status === 'error') return item.errorMessage || 'アップロードに失敗しました';
  return item.progress === null
    ? 'アップロード中...'
    : `アップロード中... ${Math.round(item.progress)}%`;
}

/** Renders file-level upload progress, completion, failure, and retry controls. */
export function FileUploadProgress({ items, onRetry, className, ...props }: FileUploadProgressProps) {
  if (items.length === 0) return null;

  return (
    <div className={cn('space-y-3', className)} aria-live="polite" {...props}>
      {items.map((item) => {
        const isIndeterminate = item.status === 'uploading' && item.progress === null;

        return (
          <div
            key={item.id}
            className="rounded-lg border border-border bg-card p-4"
            aria-busy={item.status === 'uploading' || undefined}
          >
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div className="min-w-0 flex-1">
                <p className="break-all text-small font-bold text-foreground">{item.fileName}</p>
                <div className={cn('mt-1 flex items-center gap-2 text-subtle', item.status === 'error' ? 'text-destructive' : 'text-muted-foreground')}>
                  {item.status === 'success' ? <Check className="h-4 w-4 text-success" aria-hidden="true" /> : null}
                  {item.status === 'error' ? <AlertCircle className="h-4 w-4" aria-hidden="true" /> : null}
                  {item.status === 'queued' ? <Clock3 className="h-4 w-4" aria-hidden="true" /> : null}
                  {item.status === 'uploading' && item.progress === null ? <InlineLoader label="アップロード中" showLabel={false} /> : null}
                  <span>{getUploadStatusText(item)}</span>
                </div>
              </div>
              {item.status === 'error' && onRetry ? (
                <ActionButton kind="retry" onClick={() => onRetry(item.id)} />
              ) : null}
            </div>
            {item.status === 'uploading' ? (
              <div className="mt-3">
                {isIndeterminate ? (
                  <div className="h-2 overflow-hidden rounded-full bg-muted" aria-label="アップロード進行中">
                    <div className="loading-progress-indeterminate h-full w-1/3 rounded-full bg-primary motion-reduce:animate-none" />
                  </div>
                ) : (
                  <Progress value={item.progress ?? 0} className="h-2 bg-muted" aria-label={`${item.fileName} ${Math.round(item.progress ?? 0)}%`} />
                )}
              </div>
            ) : null}
          </div>
        );
      })}
    </div>
  );
}

export type ProcessingStageStatus = 'pending' | 'active' | 'complete' | 'error';

export type ProcessingStage = {
  id: string;
  label: string;
  status: ProcessingStageStatus;
};

type ProcessingProgressProps = HTMLAttributes<HTMLDivElement> & {
  title: string;
  description?: string;
  progress?: number | null;
  stages?: ProcessingStage[];
};

/** Renders honest determinate or indeterminate progress for long-running jobs. */
export function ProcessingProgress({
  title,
  description,
  progress = null,
  stages = [],
  className,
  ...props
}: ProcessingProgressProps) {
  const normalizedProgress = progress === null ? null : Math.min(100, Math.max(0, progress));
  const isComplete =
    normalizedProgress === 100 ||
    (stages.length > 0 && stages.every((stage) => stage.status === 'complete'));

  return (
    <section
      role="status"
      aria-live="polite"
      aria-busy={!isComplete}
      className={cn('rounded-lg border border-border bg-card p-5', className)}
      {...props}
    >
      <h3 className="text-foreground">{title}</h3>
      {description ? <p className="mt-2 text-small text-muted-foreground">{description}</p> : null}
      <div className="mt-5">
        {normalizedProgress === null ? (
          <div className="h-2 overflow-hidden rounded-full bg-muted" aria-label="処理中">
            <div className="loading-progress-indeterminate h-full w-1/3 rounded-full bg-primary motion-reduce:animate-none" />
          </div>
        ) : (
          <Progress value={normalizedProgress} className="h-2 bg-muted" aria-label={`${Math.round(normalizedProgress)}%`} />
        )}
      </div>
      {stages.length > 0 ? (
        <ol className="mt-5 space-y-3">
          {stages.map((stage) => (
            <li key={stage.id} className="flex items-center gap-3 text-small text-foreground">
              <span className={cn('flex h-6 w-6 shrink-0 items-center justify-center rounded-full border', stage.status === 'complete' && 'border-success bg-success text-success-foreground', stage.status === 'active' && 'border-primary bg-secondary text-primary', stage.status === 'error' && 'border-destructive bg-destructive/10 text-destructive', stage.status === 'pending' && 'border-border text-muted-foreground')}>
                {stage.status === 'complete' ? <Check className="h-3.5 w-3.5" aria-hidden="true" /> : null}
                {stage.status === 'active' ? <span className="h-2 w-2 rounded-full bg-primary" aria-hidden="true" /> : null}
                {stage.status === 'error' ? <AlertCircle className="h-3.5 w-3.5" aria-hidden="true" /> : null}
              </span>
              <span>{stage.label}</span>
            </li>
          ))}
        </ol>
      ) : null}
    </section>
  );
}
