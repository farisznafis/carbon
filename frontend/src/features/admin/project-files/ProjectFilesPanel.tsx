import { useState } from 'react';
import * as AccordionPrimitive from '@radix-ui/react-accordion';
import { ChevronRight, Plus, Upload } from 'lucide-react';
import type { ProjectFileType } from '@/api/admin/projectFiles';
import { ActionButton } from '@/components/actions';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { Accordion, AccordionContent, AccordionItem } from '@/components/ui/accordion';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { useAdminPermissions } from '@/features/admin/permissions/useAdminPermissions';
import { formatUploadedAt } from '@/features/admin/project-plan/planFiles';
import { toast } from '@/hooks/use-toast';
import { ProjectFileDeleteDialog } from './ProjectFileDeleteDialog';
import { ProjectFilePreviewDialog } from './ProjectFilePreviewDialog';
import { ProjectFileRow, sectionRowGridClassName } from './ProjectFileRow';
import { ProjectFileUploadDialog } from './ProjectFileUploadDialog';
import { ProjectMembershipPdfSection } from './ProjectMembershipPdfSection';
import {
  PROJECT_FILE_STAGES,
  projectFileTypeLabel,
  type ProjectFileGroup,
  type ProjectFileVersion,
} from './projectFileTypes';
import { downloadProjectFile, useProjectFiles } from './useProjectFiles';

const errorMessage = (error: unknown, fallback: string) =>
  error instanceof Error && error.message ? error.message : fallback;

/** An open upload dialog, with the type chosen beforehand if any. Null when closed. */
export type ProjectFileUploadRequest = { type: ProjectFileType | null } | null;

type ProjectFilesPanelProps = Readonly<{
  projectId: string;
  /** Owned by the tab, whose toolbar holds the global upload button. */
  uploadRequest: ProjectFileUploadRequest;
  onUploadRequestChange: (request: ProjectFileUploadRequest) => void;
}>;

/**
 * The フォーム tab: one collapsible section per document type, like Swagger's
 * operation list, each holding its versions newest first. Each section adds
 * to itself, so the upload dialog opens with its type already chosen.
 */
export function ProjectFilesPanel({
  projectId,
  uploadRequest,
  onUploadRequestChange,
}: ProjectFilesPanelProps) {
  const permissions = useAdminPermissions();
  const { listingQuery, groups, uploadMutation, deleteMutation } = useProjectFiles(projectId);

  // Null until the user toggles a section: until then the sections with files are open.
  const [openSections, setOpenSections] = useState<string[] | null>(null);
  const [previewFile, setPreviewFile] = useState<ProjectFileVersion | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ProjectFileVersion | null>(null);

  const expandedSections =
    openSections ?? groups.filter((group) => group.files.length > 0).map((group) => group.type);

  const openUpload = (presetType: ProjectFileType | null = null) =>
    permissions.guardAction('PROJECT_UPDATE', () =>
      onUploadRequestChange({ type: presetType }),
    );

  const expandSection = (fileType: ProjectFileType) =>
    setOpenSections((current) => {
      const base = current ?? expandedSections;
      return base.includes(fileType) ? base : [...base, fileType];
    });

  /** From the upload dialog's limit warning: back to the list to delete a version. */
  const showSection = (fileType: ProjectFileType) => {
    onUploadRequestChange(null);
    expandSection(fileType);
  };

  const handleUpload = async (fileType: ProjectFileType, file: File) => {
    await uploadMutation.mutateAsync({ fileType, file });
    expandSection(fileType);
    toast({
      title: `${projectFileTypeLabel(fileType)}をアップロードしました`,
      description: file.name,
      variant: 'success',
    });
  };

  const handleDownload = (file: ProjectFileVersion) => downloadProjectFile(projectId, file);

  const requestDelete = (file: ProjectFileVersion) =>
    permissions.guardAction('PROJECT_UPDATE', () => setDeleteTarget(file));

  const confirmDelete = async (file: ProjectFileVersion) => {
    try {
      await deleteMutation.mutateAsync(file.id);
      setDeleteTarget(null);
      toast({
        title: 'ファイルを削除しました',
        description: file.originalFileName ?? undefined,
        variant: 'success',
      });
    } catch (error) {
      toast({
        title: '削除できませんでした',
        description: errorMessage(error, '時間をおいて再度お試しください。'),
        variant: 'destructive',
      });
    }
  };

  const renderSections = () => {
    if (listingQuery.isLoading) {
      return (
        <div role="status" aria-label="ファイルを読み込んでいます" className="space-y-2">
          {[0, 1, 2].map((index) => (
            <Skeleton key={index} className="h-16 w-full rounded-lg" />
          ))}
        </div>
      );
    }

    if (listingQuery.isError) {
      return (
        <div
          role="alert"
          className="flex flex-col items-center gap-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center"
        >
          <p className="text-small font-medium text-foreground">
            {errorMessage(listingQuery.error, 'ファイル一覧を取得できませんでした。')}
          </p>
          <ActionButton kind="retry" onClick={() => void listingQuery.refetch()} />
        </div>
      );
    }

    /**
     * A type with nothing in it yet: nothing to expand, so the row only invites the first upload.
     * Its name keeps the siblings' weight; only the status dims, or the row reads as a child.
     */
    const renderEmptySection = (group: ProjectFileGroup) => (
      <div key={group.type} className="flex min-h-11 items-center gap-3 py-1.5 pl-4 pr-3">
        <span aria-hidden="true" className="h-4 w-4 shrink-0" />
        <span className={sectionRowGridClassName}>
          <span className="truncate text-sm font-medium text-foreground" title={group.description}>
            {group.label}
          </span>
          <span className="text-small text-muted-foreground sm:col-span-2">未アップロード</span>
        </span>
        <Button
          type="button"
          size="sm"
          variant="outline"
          className="shrink-0 text-primary-strong"
          aria-label={`${group.label}にファイルを追加`}
          onClick={() => openUpload(group.type)}
        >
          <Upload aria-hidden="true" />
          アップロード
        </Button>
      </div>
    );

    const renderSection = (group: ProjectFileGroup) => {
      const latest = group.files[0];
      if (!latest) return renderEmptySection(group);
      return (
        <AccordionItem key={group.type} value={group.type} className="border-b-0">
          {/* The add button sits beside the trigger, not in it: a button cannot hold a button. */}
          <AccordionPrimitive.Header className="flex items-center gap-2 pr-3 transition-colors hover:bg-muted/60">
            <AccordionPrimitive.Trigger className="group flex min-w-0 flex-1 items-center gap-3 py-3 pl-4 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring">
              <ChevronRight
                aria-hidden="true"
                className="h-4 w-4 shrink-0 text-muted-foreground transition-transform duration-200 group-data-[state=open]:rotate-90 motion-reduce:transition-none"
              />
              <span className={sectionRowGridClassName}>
                <span className="truncate text-sm font-medium text-foreground">{group.label}</span>
                <span className="flex items-center gap-2 text-small text-muted-foreground sm:contents">
                  <StatusBadge className="w-fit tabular-nums">{group.files.length}件</StatusBadge>
                  <span className="tabular-nums">最新 {formatUploadedAt(latest.uploadedAt)}</span>
                </span>
              </span>
            </AccordionPrimitive.Trigger>
            {/*
              Icon only, as most rows carry one, but outlined like the empty rows'
              button so it reads as a control and both line up at one height.
            */}
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  className="w-8 shrink-0 px-0 text-primary-strong"
                  aria-label={`${group.label}にファイルを追加`}
                  onClick={() => openUpload(group.type)}
                >
                  <Plus aria-hidden="true" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>新しい版を追加</TooltipContent>
            </Tooltip>
          </AccordionPrimitive.Header>
          <AccordionContent className="border-t border-border bg-muted/30 p-0">
            <ul className="divide-y divide-border">
              {group.files.map((file, index) => (
                <ProjectFileRow
                  key={file.id}
                  file={file}
                  isLatest={index === 0}
                  onPreview={setPreviewFile}
                  onDownload={(target) => void handleDownload(target)}
                  onDelete={requestDelete}
                />
              ))}
            </ul>
          </AccordionContent>
        </AccordionItem>
      );
    };

    // One accordion for every section; each J-Credit stage is one card of rows.
    return (
      <Accordion
        type="multiple"
        value={expandedSections}
        onValueChange={setOpenSections}
        className="space-y-6"
      >
        {PROJECT_FILE_STAGES.map(({ stage, label }) => {
          const stageGroups = groups.filter((group) => group.stage === stage);
          const uploadedCount = stageGroups.filter((group) => group.files.length > 0).length;
          return (
            <section
              key={stage}
              aria-labelledby={`project-file-stage-${stage}`}
              className="space-y-2"
            >
              <div className="flex items-baseline justify-between gap-3 px-1">
                <h3
                  id={`project-file-stage-${stage}`}
                  className="text-sm font-bold text-foreground"
                >
                  {label}
                </h3>
                <span className="text-small tabular-nums text-muted-foreground">
                  {uploadedCount} / {stageGroups.length} アップロード済
                </span>
              </div>
              <div className="divide-y divide-border overflow-hidden rounded-lg border border-border bg-card">
                {stageGroups.flatMap((group) =>
                  // 入会届 sits with the monitoring forms but is not one of the versioned types.
                  group.type === 'ACTIVITY_RESULT_LIST'
                    ? [renderSection(group), <ProjectMembershipPdfSection key="membership" projectId={projectId} />]
                    : [renderSection(group)],
                )}
              </div>
            </section>
          );
        })}
      </Accordion>
    );
  };

  const previewGroupLabel = previewFile ? projectFileTypeLabel(String(previewFile.fileType)) : '';
  const deleteGroupLabel = deleteTarget ? projectFileTypeLabel(String(deleteTarget.fileType)) : '';

  return (
    <TooltipProvider delayDuration={250}>
      {renderSections()}

      <ProjectFileUploadDialog
        open={uploadRequest !== null}
        onOpenChange={(open) => !open && onUploadRequestChange(null)}
        groups={groups}
        initialType={uploadRequest?.type ?? null}
        isUploading={uploadMutation.isPending}
        onUpload={handleUpload}
        onShowFiles={showSection}
      />

      <ProjectFilePreviewDialog
        projectId={projectId}
        typeLabel={previewGroupLabel}
        file={previewFile}
        onOpenChange={(open) => !open && setPreviewFile(null)}
        onDownload={(file) => void handleDownload(file)}
      />

      <ProjectFileDeleteDialog
        typeLabel={deleteGroupLabel}
        file={deleteTarget}
        isDeleting={deleteMutation.isPending}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        onConfirm={(file) => void confirmDelete(file)}
      />
    </TooltipProvider>
  );
}
