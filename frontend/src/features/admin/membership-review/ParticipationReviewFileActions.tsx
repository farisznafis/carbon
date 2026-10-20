import { useState } from 'react';
import { Download, ExternalLink, LoaderCircle } from 'lucide-react';
import type { ParticipationReviewFile } from '@/api/admin';
import { getSiteProjectEquipmentPresignedUrl } from '@/api/site';
import { Button } from '@/components/ui/button';
import { useToast } from '@/hooks/use-toast';
import { formatFileType } from './participationReviewDisplay';

type FileAction = 'preview' | 'download';

function getFileName(file: ParticipationReviewFile) {
  const fallbackName = file.fileId || 'download';
  const encodedName = file.s3Key.split('/').filter(Boolean).at(-1);
  if (!encodedName) return fallbackName;

  try {
    return decodeURIComponent(encodedName);
  } catch {
    return encodedName;
  }
}

function getErrorMessage(error: unknown) {
  return error instanceof Error && error.message
    ? error.message
    : 'ファイルの取得に失敗しました。';
}

function openInNewTab(url: string) {
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.target = '_blank';
  anchor.rel = 'noopener noreferrer';
  anchor.click();
}

async function downloadFile(url: string, fileName: string) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`ファイルのダウンロードに失敗しました: HTTP ${response.status}`);
  }

  const objectUrl = URL.createObjectURL(await response.blob());
  const anchor = document.createElement('a');
  anchor.href = objectUrl;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
}

export function ParticipationReviewFileActions({
  file,
}: {
  file: ParticipationReviewFile;
}) {
  const { toast } = useToast();
  const [activeAction, setActiveAction] = useState<FileAction | null>(null);
  const isBusy = activeAction !== null;
  const fileLabel = file.fileType ? formatFileType(file.fileType) : getFileName(file);

  const runFileAction = async (action: FileAction) => {
    if (!file.s3Key || isBusy) return;

    setActiveAction(action);
    try {
      const presignedUrl = await getSiteProjectEquipmentPresignedUrl(
        file.s3Key,
      );

      if (action === 'preview') {
        openInNewTab(presignedUrl);
      } else {
        await downloadFile(presignedUrl, getFileName(file));
      }
    } catch (error) {
      toast({
        title:
          action === 'preview'
            ? 'ファイルを表示できませんでした'
            : 'ファイルをダウンロードできませんでした',
        description: getErrorMessage(error),
        variant: 'destructive',
      });
    } finally {
      setActiveAction(null);
    }
  };

  return (
    <div className="flex flex-wrap gap-2">
      <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={!file.s3Key || isBusy}
        aria-label={`${fileLabel}を表示`}
        onClick={() => void runFileAction('preview')}
      >
        {activeAction === 'preview' ? (
          <LoaderCircle className="animate-spin" />
        ) : (
          <ExternalLink />
        )}
        表示
      </Button>
      {/* <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={!file.s3Key || isBusy}
        aria-label={`${fileLabel}をダウンロード`}
        onClick={() => void runFileAction('download')}
      >
        {activeAction === 'download' ? (
          <LoaderCircle className="animate-spin" />
        ) : (
          <Download />
        )}
        ダウンロード
      </Button> */}
    </div>
  );
}
