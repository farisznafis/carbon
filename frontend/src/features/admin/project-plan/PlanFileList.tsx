import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import {
  getProjectFileDownloadUrl,
  uploadProjectFile,
  type ProjectFile,
} from '@/api/admin/projectFiles';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { toast } from '@/hooks/use-toast';
import { PlanFilePicker } from './PlanFilePicker';
import { formatUploadedAt, projectPlanFilesQueryKey, useProjectPlanFiles } from './planFiles';

const errorMessage = (error: unknown, fallback: string) =>
  error instanceof Error && error.message ? error.message : fallback;

type PlanFileListProps = Readonly<{
  projectId: string;
  /** Shows the upload control. Off on a read-only page. */
  canUpload?: boolean;
}>;

/**
 * The 計画書 files uploaded for a project, each downloadable as it was sent.
 *
 * An upload never replaces an earlier file: it is added to the list and
 * becomes the newest. With `canUpload`, a chosen file is uploaded at once —
 * the project already exists, so there is nothing to wait for.
 */
export function PlanFileList({ projectId, canUpload = false }: PlanFileListProps) {
  const queryClient = useQueryClient();
  const filesQuery = useProjectPlanFiles(projectId);
  const [isUploading, setIsUploading] = useState(false);
  const files = filesQuery.data ?? [];

  const handleUpload = async (file: File | null) => {
    if (!file) return;
    setIsUploading(true);
    try {
      await uploadProjectFile(projectId, 'PROJECT_PLAN', file);
      await queryClient.invalidateQueries({ queryKey: projectPlanFilesQueryKey(projectId) });
      toast({
        title: '計画書をアップロードしました',
        description: file.name,
        variant: 'success',
      });
    } catch (error) {
      toast({
        title: 'アップロードできませんでした',
        description: errorMessage(error, '時間をおいて再度お試しください。'),
        variant: 'destructive',
      });
    } finally {
      setIsUploading(false);
    }
  };

  const handleDownload = async (file: ProjectFile) => {
    try {
      const url = await getProjectFileDownloadUrl(projectId, file.id);
      window.open(url, '_blank', 'noopener,noreferrer');
    } catch (error) {
      toast({
        title: 'ファイルを取得できませんでした',
        description: errorMessage(error, '時間をおいて再度お試しください。'),
        variant: 'destructive',
      });
    }
  };

  const renderFiles = () => {
    if (filesQuery.isLoading) {
      return (
        <div role="status" aria-label="計画書ファイルを読み込んでいます" className="space-y-2">
          <Skeleton className="h-9 w-full" />
          <Skeleton className="h-9 w-full" />
        </div>
      );
    }
    if (filesQuery.isError) {
      return (
        <p role="alert" className="text-sm text-destructive">
          {errorMessage(filesQuery.error, '計画書ファイルを取得できませんでした。')}
        </p>
      );
    }
    if (files.length === 0) {
      return (
        <p className="rounded-md border border-dashed border-border px-4 py-6 text-center text-sm text-muted-foreground">
          計画書ファイルはまだアップロードされていません。
        </p>
      );
    }

    return (
      <ul className="divide-y divide-border rounded-md border border-border">
        {files.map((file, index) => (
          <li
            key={file.id}
            className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:justify-between"
          >
            <div className="min-w-0">
              <p className="flex flex-wrap items-center gap-2 text-sm">
                <span className="break-all">{file.originalFileName ?? '—'}</span>
                {index === 0 ? <StatusBadge tone="success">最新</StatusBadge> : null}
              </p>
              <p className="text-sm text-muted-foreground">
                {formatUploadedAt(file.uploadedAt)}
                {file.uploadedBy ? `（${file.uploadedBy}）` : ''}
              </p>
            </div>
            <Button
              type="button"
              size="sm"
              variant="outline"
              className="self-start sm:self-auto"
              aria-label={`${file.originalFileName ?? 'ファイル'} をダウンロード`}
              onClick={() => void handleDownload(file)}
            >
              <Download aria-hidden="true" />
              ダウンロード
            </Button>
          </li>
        ))}
      </ul>
    );
  };

  return (
    <div className="space-y-4">
      {renderFiles()}
      {canUpload ? (
        <PlanFilePicker
          file={null}
          onChange={(file) => void handleUpload(file)}
          disabled={isUploading}
          buttonLabel={isUploading ? 'アップロード中…' : '新しい計画書をアップロード'}
        />
      ) : null}
    </div>
  );
}
