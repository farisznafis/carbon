import { useQuery } from '@tanstack/react-query';
import { Download, ExternalLink } from 'lucide-react';
import { getProjectFileDownloadUrl } from '@/api/admin/projectFiles';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Skeleton } from '@/components/ui/skeleton';
import { formatUploadedAt } from '@/features/admin/project-plan/planFiles';
import type { ProjectFileVersion } from './projectFileTypes';

/** Presigned URLs live 15 minutes; reuse one for a while, then ask again. */
const PREVIEW_URL_STALE_MS = 10 * 60 * 1000;

type ProjectFilePreviewDialogProps = Readonly<{
  projectId: string;
  typeLabel: string;
  file: ProjectFileVersion | null;
  onOpenChange: (open: boolean) => void;
  onDownload: (file: ProjectFileVersion) => void;
}>;

/** Shows a PDF version in place, with a way out to a full tab or a download. */
export function ProjectFilePreviewDialog({
  projectId,
  typeLabel,
  file,
  onOpenChange,
  onDownload,
}: ProjectFilePreviewDialogProps) {
  const urlQuery = useQuery({
    queryKey: ['admin', 'projects', projectId, 'files', 'preview-url', file?.id],
    queryFn: () => getProjectFileDownloadUrl(projectId, file!.id),
    enabled: Boolean(file),
    staleTime: PREVIEW_URL_STALE_MS,
    retry: false,
  });

  const renderBody = () => {
    if (urlQuery.isLoading) {
      return (
        <div role="status" aria-label="ファイルを読み込んでいます" className="h-full">
          <Skeleton className="h-full w-full" />
        </div>
      );
    }
    if (urlQuery.isError || !urlQuery.data) {
      return (
        <p role="alert" className="py-10 text-center text-sm text-destructive">
          ファイルを表示できませんでした。ダウンロードしてご確認ください。
        </p>
      );
    }
    return (
      <iframe
        src={urlQuery.data}
        title={file?.originalFileName ?? 'ファイルのプレビュー'}
        className="h-full w-full rounded-md border border-border bg-muted"
      />
    );
  };

  return (
    <Dialog open={Boolean(file)} onOpenChange={onOpenChange}>
      <DialogContent className="flex h-[85dvh] max-w-5xl flex-col gap-4">
        <DialogHeader className="pr-8">
          <DialogTitle className="break-all leading-6">
            {file?.originalFileName ?? '—'}
          </DialogTitle>
          <DialogDescription>
            {typeLabel} v{file?.version} ・ {formatUploadedAt(file?.uploadedAt)}
            {file?.uploadedBy ? `（${file.uploadedBy}）` : ''}
          </DialogDescription>
          <div className="flex flex-wrap gap-2 pt-2">
            <Button
              type="button"
              size="sm"
              variant="outline"
              disabled={!urlQuery.data}
              onClick={() =>
                urlQuery.data && window.open(urlQuery.data, '_blank', 'noopener,noreferrer')
              }
            >
              <ExternalLink aria-hidden="true" />
              新しいタブで開く
            </Button>
            {file ? (
              <Button type="button" size="sm" variant="outline" onClick={() => onDownload(file)}>
                <Download aria-hidden="true" />
                ダウンロード
              </Button>
            ) : null}
          </div>
        </DialogHeader>
        <div className="min-h-0 flex-1">{renderBody()}</div>
      </DialogContent>
    </Dialog>
  );
}
