import type { ReactNode } from 'react';
import { Download, Eye, FileSpreadsheet, FileText, Trash2 } from 'lucide-react';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { Button } from '@/components/ui/button';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { formatUploadedAt } from '@/features/admin/project-plan/planFiles';
import { cn } from '@/lib/utils';
import { previewKindOf, type ProjectFileVersion } from './projectFileTypes';

type ProjectFileRowProps = Readonly<{
  file: ProjectFileVersion;
  isLatest: boolean;
  onPreview: (file: ProjectFileVersion) => void;
  onDownload: (file: ProjectFileVersion) => void;
  onDelete: (file: ProjectFileVersion) => void;
}>;

/** Label, count and latest upload share columns across section rows so the dates scan down. */
export const sectionRowGridClassName =
  'flex min-w-0 flex-1 flex-col gap-0.5 sm:grid sm:grid-cols-[minmax(0,1fr)_3.5rem_9.5rem] sm:items-center sm:gap-4';

/** Icon button whose name is both its accessible label and its tooltip. */
export function RowIconButton({
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
      className={cn(destructive && 'text-destructive-strong hover:bg-destructive-subtle hover:text-destructive-strong')}
    >
      {children}
    </Button>
  );

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        {/* A disabled button fires no pointer events, so the span carries the tooltip. */}
        {disabled ? (
          <span tabIndex={0} aria-label={tooltip} className="cursor-not-allowed">
            {button}
          </span>
        ) : (
          button
        )}
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
  const previewKind = previewKindOf(file);
  const isExcel = name.toLowerCase().endsWith('.xlsx');
  const FileIcon = isExcel ? FileSpreadsheet : FileText;

  return (
    <li className="flex flex-col gap-2 px-4 py-3 sm:flex-row sm:items-center sm:gap-4">
      <span className="w-10 shrink-0 text-small font-bold tabular-nums text-muted-foreground">
        v{file.version}
      </span>

      <div className="flex min-w-0 flex-1 items-start gap-3">
        <FileIcon
          aria-hidden="true"
          className={cn('mt-0.5 h-5 w-5 shrink-0', isExcel ? 'text-success-strong' : 'text-destructive-strong')}
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

      <div className="flex shrink-0 items-center gap-2 self-end sm:self-auto">
        <RowIconButton
          label={`${name} を表示`}
          tooltip={previewKind ? '表示' : 'この形式は表示できません。ダウンロードしてご確認ください'}
          disabled={!previewKind}
          onClick={() => onPreview(file)}
        >
          <Eye aria-hidden="true" />
        </RowIconButton>
        <RowIconButton label={`${name} をダウンロード`} tooltip="ダウンロード" onClick={() => onDownload(file)}>
          <Download aria-hidden="true" />
        </RowIconButton>
        {/* Kept apart from the safe actions so a destructive click is never a near miss. */}
        <span aria-hidden="true" className="mx-1 h-5 w-px bg-border" />
        <RowIconButton label={`${name} を削除`} tooltip="削除" destructive onClick={() => onDelete(file)}>
          <Trash2 aria-hidden="true" />
        </RowIconButton>
      </div>
    </li>
  );
}
