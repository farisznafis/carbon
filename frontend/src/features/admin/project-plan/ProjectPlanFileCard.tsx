import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { formatUploadedAt, useProjectPlanFiles } from './planFiles';

/** The project's current 計画書 file, for the project overview. */
export function ProjectPlanFileCard({ projectId }: Readonly<{ projectId: string }>) {
  const filesQuery = useProjectPlanFiles(projectId);
  const files = filesQuery.data ?? [];
  const latest = files[0];

  return (
    <Card className="h-full rounded-xl border-border shadow-app-sm">
      <CardHeader>
        <CardTitle>計画書</CardTitle>
        <CardDescription>アップロードされた最新の計画書ファイルです。</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {filesQuery.isLoading ? (
          <div role="status" aria-label="計画書ファイルを読み込んでいます" className="space-y-4 py-2">
            <Skeleton className="h-6 w-56" />
            <Skeleton className="h-4 w-32" />
          </div>
        ) : filesQuery.isError ? (
          <p role="alert" className="py-6 text-small text-destructive">
            計画書ファイルを取得できませんでした。
          </p>
        ) : latest ? (
          <>
            <p className="break-all text-foreground">{latest.originalFileName ?? '—'}</p>
            <p className="text-small text-muted-foreground">
              アップロード: {formatUploadedAt(latest.uploadedAt)}
            </p>
            {files.length > 1 ? (
              <p className="text-small text-muted-foreground">
                これまでのアップロード: {files.length} 件
              </p>
            ) : null}
          </>
        ) : (
          <p className="py-6 text-small text-muted-foreground">
            計画書ファイルはまだアップロードされていません。
          </p>
        )}
      </CardContent>
    </Card>
  );
}
