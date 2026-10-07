import type { ReactNode } from 'react';
import { Download, Eye, FileSpreadsheet, FileText, Trash2 } from 'lucide-react';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { Button } from '@/components/ui/button';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { formatUploadedAt } from '@/features/admin/project-plan/planFiles';
import { cn } from '@/lib/utils';
import { isPreviewableFile, type ProjectFileVersion } from './projectFileTypes';

type ProjectFileRowProps = Readonly<{
  file: ProjectFileVersion;
  isLatest: boolean;
  onPreview: (file: ProjectFileVersion) => void;
  onDownload: (file: ProjectFileVersion) => void;
  onDelete: (file: ProjectFileVersion) => void;
}>;

/** Icon button whose name is both its accessible label and its tooltip. */
function RowIconButton({
  label,
  tooltip = label,
  disabled = false,
  destructive = false,
  onClick,
  children,
}: Readonly<{
  label: string;
  tooltip?: string;
  disabled?: boolean;
  destructive?: boolean;
  onClick: () => void;
  children: ReactNode;
}>) {
  const button = (
    <Button
      type="button"
      size="icon"
      variant="ghost"
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
      className={cn(destructive && 'text-destructive hover:bg-destructive-subtle hover:text-destructive-strong')}
    >
      {children}
    </Button>
  );

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        {/* A disabled button fires no pointer events, so the span carries the tooltip. */}
        {disabled ? <span tabIndex={0} aria-label={tooltip}>{button}</span> : button}
      </TooltipTrigger>
      <TooltipContent>{tooltip}</TooltipContent>
    </Tooltip>
  );
}

/** One version of a document: what it is, who sent it, and what can be done with it. */
export function ProjectFileRow({
  file,
  isLatest,
  onPreview,
  onDownload,
  onDelete,
}: ProjectFileRowProps) {
  const name = file.originalFileName ?? '—';
  const previewable = isPreviewableFile(file);
  const FileIcon = previewable ? FileText : FileSpreadsheet;

  return (
    <li className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:gap-4">
      <span className="w-10 shrink-0 text-small font-bold tabular-nums text-muted-foreground">
        v{file.version}
      </span>

      <div className="flex min-w-0 flex-1 items-start gap-3">
        <FileIcon
          aria-hidden="true"
          className={cn('mt-0.5 h-5 w-5 shrink-0', previewable ? 'text-destructive' : 'text-success')}
        />
        <div className="min-w-0">
          <p className="flex flex-wrap items-center gap-2 text-sm text-foreground">
            <span className="break-all font-medium">{name}</span>
            {isLatest ? <StatusBadge tone="success">最新</StatusBadge> : null}
          </p>
          <p className="text-small text-muted-foreground">
            {formatUploadedAt(file.uploadedAt)}
            {file.uploadedBy ? `・${file.uploadedBy}` : ''}
          </p>
        </div>
      </div>

      <div className="flex shrink-0 items-center gap-1 self-end sm:self-auto">
        <RowIconButton
          label={`${name} を表示`}
          tooltip={previewable ? '表示' : 'Excelファイルはダウンロードして確認してください'}
          disabled={!previewable}
          onClick={() => onPreview(file)}
        >
          <Eye aria-hidden="true" />
        </RowIconButton>
        <RowIconButton label={`${name} をダウンロード`} tooltip="ダウンロード" onClick={() => onDownload(file)}>
          <Download aria-hidden="true" />
        </RowIconButton>
        <RowIconButton label={`${name} を削除`} tooltip="削除" destructive onClick={() => onDelete(file)}>
          <Trash2 aria-hidden="true" />
        </RowIconButton>
      </div>
    </li>
  );
}
