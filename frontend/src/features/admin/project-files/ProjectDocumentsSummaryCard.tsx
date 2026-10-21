import { useState, type ReactNode } from 'react';
import { Download, Eye, FileSpreadsheet, FileText, Upload } from 'lucide-react';
import type { ProjectFileType } from '@/api/admin/projectFiles';
import { ActionButton } from '@/components/actions';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { formatUploadedAt } from '@/features/admin/project-plan/planFiles';
import { cn } from '@/lib/utils';
import { ProjectFilePreviewDialog } from './ProjectFilePreviewDialog';
import {
  previewKindOf,
  projectFileTypeLabel,
  type ProjectFileGroup,
  type ProjectFileVersion,
} from './projectFileTypes';
import { downloadProjectFile, useProjectFiles } from './useProjectFiles';

/**
 * The forms the project is judged on. The VVB's reports and the secretariat's
 * notices answer these, so 概要 stays at three rows and 書類 holds the rest.
 */
const SUMMARY_FILE_TYPES: readonly ProjectFileType[] = [
  'PROJECT_PLAN',
  'MONITORING_REPORT',
  'ACTIVITY_RESULT_LIST',
];

type ProjectDocumentsSummaryCardProps = Readonly<{
  projectId: string;
  /** Opens the 書類 tab with every version. */
  onOpenDocuments: () => void;
  /** Opens the 書類 tab with the upload dialog for this type. */
  onUpload: (fileType: ProjectFileType) => void;
}>;

function IconAction({
  label,
  tooltip,
  onClick,
  children,
}: Readonly<{ label: string; tooltip: string; onClick: () => void; children: ReactNode }>) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button type="button" size="icon" variant="ghost" aria-label={label} onClick={onClick}>
          {children}
        </Button>
      </TooltipTrigger>
      <TooltipContent>{tooltip}</TooltipContent>
    </Tooltip>
  );
}

/** One document type: its current file at a glance, or a way to add the first one. */
function DocumentRow({
  group,
  onPreview,
  onDownload,
  onUpload,
}: Readonly<{
  group: ProjectFileGroup;
  onPreview: (file: ProjectFileVersion) => void;
  onDownload: (file: ProjectFileVersion) => void;
  onUpload: (fileType: ProjectFileType) => void;
}>) {
  const latest = group.files[0];
  const isExcel = latest?.originalFileName?.toLowerCase().endsWith('.xlsx') ?? false;
  const FileIcon = isExcel ? FileSpreadsheet : FileText;
  const name = latest?.originalFileName ?? '—';

  return (
    <li className="flex items-start gap-3 py-3 first:pt-0 last:pb-0">
      <span
        aria-hidden="true"
        className={cn(
          'flex h-9 w-9 shrink-0 items-center justify-center rounded-lg',
          !latest
            ? 'border border-dashed border-border text-muted-foreground'
            : isExcel
              ? 'bg-success-subtle text-success-strong'
              : 'bg-destructive-subtle text-destructive-strong',
        )}
      >
        <FileIcon className="h-5 w-5" />
      </span>

      <div className="min-w-0 flex-1">
        <p className="text-small text-muted-foreground">{group.label}</p>
        {latest ? (
          <>
            <p className="line-clamp-2 break-all text-sm font-medium text-foreground" title={name}>
              {name}
            </p>
            <p className="mt-0.5 text-small tabular-nums text-muted-foreground">
              v{latest.version} ・ {formatUploadedAt(latest.uploadedAt)}
            </p>
          </>
        ) : (
          <p className="text-sm text-muted-foreground">未アップロード</p>
        )}
      </div>

      <div className="flex shrink-0 items-center gap-2">
        {latest ? (
          <>
            {previewKindOf(latest) ? (
              <IconAction label={`${name} を表示`} tooltip="表示" onClick={() => onPreview(latest)}>
                <Eye aria-hidden="true" />
              </IconAction>
            ) : null}
            <IconAction
              label={`${name} をダウンロード`}
              tooltip="ダウンロード"
              onClick={() => onDownload(latest)}
            >
              <Download aria-hidden="true" />
            </IconAction>
          </>
        ) : (
          <Button type="button" size="sm" variant="outline" onClick={() => onUpload(group.type)}>
            <Upload aria-hidden="true" />
            追加
          </Button>
        )}
      </div>
    </li>
  );
}

/**
 * 概要's view of the project's documents: the current version of each type,
 * openable in place. Every version and the upload live in the 書類 tab.
 */
export function ProjectDocumentsSummaryCard({
  projectId,
  onOpenDocuments,
  onUpload,
}: ProjectDocumentsSummaryCardProps) {
  const { listingQuery, groups: allGroups } = useProjectFiles(projectId);
  const groups = allGroups.filter((group) => SUMMARY_FILE_TYPES.includes(group.type));
  const [previewFile, setPreviewFile] = useState<ProjectFileVersion | null>(null);

  const handleDownload = (file: ProjectFileVersion) => void downloadProjectFile(projectId, file);
  const uploadedCount = groups.filter((group) => group.files.length > 0).length;

  const renderBody = () => {
    if (listingQuery.isLoading) {
      return (
        <div role="status" aria-label="書類を読み込んでいます" className="space-y-4">
          {[0, 1, 2].map((index) => (
            <div key={index} className="flex items-center gap-3">
              <Skeleton className="h-9 w-9 rounded-lg" />
              <div className="flex-1 space-y-2">
                <Skeleton className="h-3 w-24" />
                <Skeleton className="h-4 w-48" />
              </div>
            </div>
          ))}
        </div>
      );
    }
    if (listingQuery.isError) {
      return (
        <div role="alert" className="flex flex-col items-start gap-3 py-2">
          <p className="text-small text-destructive-strong">書類を取得できませんでした。</p>
          <ActionButton kind="retry" onClick={() => void listingQuery.refetch()} />
        </div>
      );
    }
    return (
      <ul className="divide-y divide-border">
        {groups.map((group) => (
          <DocumentRow
            key={group.type}
            group={group}
            onPreview={setPreviewFile}
            onDownload={handleDownload}
            onUpload={onUpload}
          />
        ))}
      </ul>
    );
  };

  return (
    <TooltipProvider delayDuration={250}>
      <Card className="flex h-full flex-col rounded-lg border-border">
        <CardHeader className="flex-row items-center justify-between gap-3 space-y-0">
          <div className="flex items-center gap-2">
            <CardTitle>書類</CardTitle>
            {listingQuery.isSuccess ? (
              <StatusBadge className="tabular-nums">
                主要様式 {uploadedCount}/{groups.length}
              </StatusBadge>
            ) : null}
          </div>
          <ActionButton kind="link" onClick={onOpenDocuments}>
            すべて見る
          </ActionButton>
        </CardHeader>
        <CardContent className="flex-1">{renderBody()}</CardContent>
      </Card>

      <ProjectFilePreviewDialog
        projectId={projectId}
        typeLabel={previewFile ? projectFileTypeLabel(String(previewFile.fileType)) : ''}
        file={previewFile}
        onOpenChange={(open) => !open && setPreviewFile(null)}
        onDownload={handleDownload}
      />
    </TooltipProvider>
  );
}
