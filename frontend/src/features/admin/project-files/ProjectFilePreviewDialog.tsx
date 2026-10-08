import { useEffect, useState } from 'react';
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
import {
  previewKindOf,
  type ProjectFilePreviewKind,
  type ProjectFileVersion,
} from './projectFileTypes';
import { readSpreadsheet } from './spreadsheetPreview';
import { SpreadsheetView } from './SpreadsheetView';

/** Presigned URLs live 15 minutes; reuse one for a while, then ask again. */
const PREVIEW_URL_STALE_MS = 10 * 60 * 1000;

type PreviewContent =
  | { kind: 'pdf'; blob: Blob }
  | { kind: 'excel'; sheets: Awaited<ReturnType<typeof readSpreadsheet>> };

/**
 * Fetches the file itself; needs the bucket to allow this origin (CORS).
 *
 * The signed URL carries Content-Disposition: attachment, so pointing an iframe
 * at it downloads the PDF instead of showing it. A blob URL ignores that header.
 */
async function fetchPreviewContent(
  url: string,
  kind: ProjectFilePreviewKind,
): Promise<PreviewContent> {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  const buffer = await response.arrayBuffer();
  if (kind === 'pdf') {
    // Typed here: the bucket may report octet-stream, which the PDF viewer refuses.
    return { kind, blob: new Blob([buffer], { type: 'application/pdf' }) };
  }
  return { kind, sheets: await readSpreadsheet(buffer) };
}

/** An object URL for the blob, revoked when the blob changes or the dialog unmounts. */
function useObjectUrl(blob: Blob | null) {
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    if (!blob) {
      setUrl(null);
      return;
    }
    const objectUrl = URL.createObjectURL(blob);
    setUrl(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [blob]);
  return url;
}

/** Anything the dialog can show: where to get its signed URL, and how to name it. */
export type FilePreviewTarget = {
  /** Identifies the file across opens, so its fetched content is cached once. */
  cacheKey: readonly unknown[];
  fileName: string;
  description: string;
  kind: ProjectFilePreviewKind | null;
  resolveUrl: () => Promise<string>;
  download: () => void;
};

/**
 * Shows one file in place: a PDF in the browser's own viewer, an Excel
 * workbook as a read-only grid. Download and a full tab stay one click away.
 */
export function FilePreviewDialog({
  target,
  onOpenChange,
}: Readonly<{ target: FilePreviewTarget | null; onOpenChange: (open: boolean) => void }>) {
  const kind = target?.kind ?? null;

  const urlQuery = useQuery({
    queryKey: [...(target?.cacheKey ?? []), 'preview-url'],
    queryFn: () => target!.resolveUrl(),
    enabled: Boolean(target),
    staleTime: PREVIEW_URL_STALE_MS,
    retry: false,
  });

  const contentQuery = useQuery({
    queryKey: [...(target?.cacheKey ?? []), 'preview-content'],
    queryFn: () => fetchPreviewContent(urlQuery.data as string, kind as ProjectFilePreviewKind),
    enabled: kind !== null && Boolean(urlQuery.data),
    staleTime: Infinity,
    retry: false,
  });

  const content = contentQuery.data;
  const pdfUrl = useObjectUrl(content?.kind === 'pdf' ? content.blob : null);

  /**
   * A failed link is usually passing, so retry leads. A file that could not be
   * read in the browser will fail the same way again, so download leads instead.
   */
  const renderFailure = (cause: 'link' | 'content', onRetry: () => void) => {
    const downloadLeads = cause === 'content';
    return (
      <div
        role="alert"
        className="flex h-full flex-col items-center justify-center gap-3 rounded-md border border-border bg-muted/40 p-6 text-center"
      >
        <AlertTriangle aria-hidden="true" className="h-6 w-6 text-warning-strong" />
        <div className="space-y-1">
          <p className="text-sm font-medium text-foreground">
            {downloadLeads
              ? 'このファイルはブラウザで表示できませんでした。'
              : 'ファイルを表示できませんでした。'}
          </p>
          <p className="text-small text-muted-foreground">
            {downloadLeads
              ? 'ダウンロードしてご確認ください。'
              : '再度お試しいただくか、ダウンロードしてご確認ください。'}
          </p>
        </div>
        <div className="flex flex-wrap justify-center gap-2">
          {target && downloadLeads ? (
            <Button type="button" size="md" onClick={target.download}>
              <Download aria-hidden="true" />
              ダウンロード
            </Button>
          ) : null}
          <ActionButton kind="retry" onClick={onRetry} />
          {target && !downloadLeads ? (
            <Button type="button" size="md" variant="outline" onClick={target.download}>
              <Download aria-hidden="true" />
              ダウンロード
            </Button>
          ) : null}
        </div>
      </div>
    );
  };

  const loading = (
    <div role="status" aria-label="ファイルを読み込んでいます" className="h-full">
      <Skeleton className="h-full w-full" />
    </div>
  );

  const renderBody = () => {
    if (urlQuery.isLoading) return loading;
    if (urlQuery.isError || !urlQuery.data) {
      return renderFailure('link', () => void urlQuery.refetch());
    }

    if (contentQuery.isLoading) return loading;
    if (contentQuery.isError || !content) {
      return renderFailure('content', () => void contentQuery.refetch());
    }
    if (content.kind === 'excel') return <SpreadsheetView sheets={content.sheets} />;
    if (!pdfUrl) return loading;

    return (
      <iframe
        src={pdfUrl}
        title={target?.fileName ?? 'ファイルのプレビュー'}
        className="h-full w-full rounded-md border border-border bg-muted"
      />
    );
  };

  return (
    <Dialog open={Boolean(target)} onOpenChange={onOpenChange}>
      <DialogContent className="flex h-[85dvh] max-w-5xl flex-col gap-4">
        <DialogHeader className="pr-8">
          <DialogTitle className="break-all leading-6">{target?.fileName ?? '—'}</DialogTitle>
          <DialogDescription>{target?.description}</DialogDescription>
          <div className="flex flex-wrap gap-2 pt-2">
            {kind === 'pdf' ? (
              <Button
                type="button"
                size="sm"
                variant="outline"
                disabled={!pdfUrl}
                onClick={() => pdfUrl && window.open(pdfUrl, '_blank', 'noopener,noreferrer')}
              >
                <ExternalLink aria-hidden="true" />
                新しいタブで開く
              </Button>
            ) : null}
            {target ? (
              <Button type="button" size="sm" variant="outline" onClick={target.download}>
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

type ProjectFilePreviewDialogProps = Readonly<{
  projectId: string;
  typeLabel: string;
  file: ProjectFileVersion | null;
  onOpenChange: (open: boolean) => void;
  onDownload: (file: ProjectFileVersion) => void;
}>;

/** One version of a project file, named by its type, version and upload. */
export function ProjectFilePreviewDialog({
  projectId,
  typeLabel,
  file,
  onOpenChange,
  onDownload,
}: ProjectFilePreviewDialogProps) {
  const target: FilePreviewTarget | null = file
    ? {
        cacheKey: ['admin', 'projects', projectId, 'files', file.id],
        fileName: file.originalFileName ?? '—',
        description: `${typeLabel} v${file.version} ・ ${formatUploadedAt(file.uploadedAt)}${
          file.uploadedBy ? `（${file.uploadedBy}）` : ''
        }`,
        kind: previewKindOf(file),
        resolveUrl: () => getProjectFileDownloadUrl(projectId, file.id),
        download: () => onDownload(file),
      }
    : null;

  return <FilePreviewDialog target={target} onOpenChange={onOpenChange} />;
}
