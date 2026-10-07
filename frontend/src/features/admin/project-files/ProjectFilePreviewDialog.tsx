import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, Download, ExternalLink } from 'lucide-react';
import { getProjectFileDownloadUrl } from '@/api/admin/projectFiles';
import { ActionButton } from '@/components/actions';
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
import { previewKindOf, type ProjectFileVersion } from './projectFileTypes';
import { readSpreadsheet } from './spreadsheetPreview';
import { SpreadsheetView } from './SpreadsheetView';

/** Presigned URLs live 15 minutes; reuse one for a while, then ask again. */
const PREVIEW_URL_STALE_MS = 10 * 60 * 1000;

type ProjectFilePreviewDialogProps = Readonly<{
  projectId: string;
  typeLabel: string;
  file: ProjectFileVersion | null;
  onOpenChange: (open: boolean) => void;
  onDownload: (file: ProjectFileVersion) => void;
}>;

/** Fetches the file itself; needs the bucket to allow this origin (CORS). */
async function fetchSpreadsheet(url: string) {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return readSpreadsheet(await response.arrayBuffer());
}

/**
 * Shows one version in place: a PDF in the browser's own viewer, an Excel
 * workbook as a read-only grid. Download and a full tab stay one click away.
 */
export function ProjectFilePreviewDialog({
  projectId,
  typeLabel,
  file,
  onOpenChange,
  onDownload,
}: ProjectFilePreviewDialogProps) {
  const kind = file ? previewKindOf(file) : null;

  const urlQuery = useQuery({
    queryKey: ['admin', 'projects', projectId, 'files', 'preview-url', file?.id],
    queryFn: () => getProjectFileDownloadUrl(projectId, file!.id),
    enabled: Boolean(file),
    staleTime: PREVIEW_URL_STALE_MS,
    retry: false,
  });

  const sheetsQuery = useQuery({
    queryKey: ['admin', 'projects', projectId, 'files', 'preview-sheets', file?.id],
    queryFn: () => fetchSpreadsheet(urlQuery.data as string),
    enabled: kind === 'excel' && Boolean(urlQuery.data),
    staleTime: Infinity,
    retry: false,
  });

  const renderFailure = (onRetry: () => void) => (
    <div
      role="alert"
      className="flex h-full flex-col items-center justify-center gap-3 rounded-md border border-border bg-muted/40 p-6 text-center"
    >
      <AlertTriangle aria-hidden="true" className="h-6 w-6 text-warning-strong" />
      <div className="space-y-1">
        <p className="text-sm font-medium text-foreground">ファイルを表示できませんでした。</p>
        <p className="text-small text-muted-foreground">
          再度お試しいただくか、ダウンロードしてご確認ください。
        </p>
      </div>
      <div className="flex flex-wrap justify-center gap-2">
        <ActionButton kind="retry" onClick={onRetry} />
        {file ? (
          <Button type="button" size="md" variant="outline" onClick={() => onDownload(file)}>
            <Download aria-hidden="true" />
            ダウンロード
          </Button>
        ) : null}
      </div>
    </div>
  );

  const loading = (
    <div role="status" aria-label="ファイルを読み込んでいます" className="h-full">
      <Skeleton className="h-full w-full" />
    </div>
  );

  const renderBody = () => {
    if (urlQuery.isLoading) return loading;
    if (urlQuery.isError || !urlQuery.data) return renderFailure(() => void urlQuery.refetch());

    if (kind === 'excel') {
      if (sheetsQuery.isLoading) return loading;
      if (sheetsQuery.isError || !sheetsQuery.data) {
        return renderFailure(() => void sheetsQuery.refetch());
      }
      return <SpreadsheetView sheets={sheetsQuery.data} />;
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
            {kind === 'pdf' ? (
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
            ) : null}
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
