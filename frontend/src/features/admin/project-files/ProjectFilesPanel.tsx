import { useState } from 'react';
import * as AccordionPrimitive from '@radix-ui/react-accordion';
import { ChevronRight, FolderOpen, Plus } from 'lucide-react';
import type { ProjectFileType } from '@/api/admin/projectFiles';
import { ActionButton } from '@/components/actions';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { Accordion, AccordionContent, AccordionItem } from '@/components/ui/accordion';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { TooltipProvider } from '@/components/ui/tooltip';
import { useAdminPermissions } from '@/features/admin/permissions/useAdminPermissions';
import { formatUploadedAt } from '@/features/admin/project-plan/planFiles';
import { toast } from '@/hooks/use-toast';
import { ProjectFileDeleteDialog } from './ProjectFileDeleteDialog';
import { ProjectFilePreviewDialog } from './ProjectFilePreviewDialog';
import { ProjectFileRow } from './ProjectFileRow';
import { ProjectFileUploadDialog } from './ProjectFileUploadDialog';
import { projectFileTypeLabel, type ProjectFileVersion } from './projectFileTypes';
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

    return (
      <Accordion
        type="multiple"
        value={expandedSections}
        onValueChange={setOpenSections}
        className="space-y-3"
      >
        {groups.map((group) => {
          const latest = group.files[0];
          return (
            <AccordionItem
              key={group.type}
              value={group.type}
              className="overflow-hidden rounded-lg border border-border bg-card shadow-app-sm"
            >
              {/* The add button sits beside the trigger, not in it: a button cannot hold a button. */}
              <AccordionPrimitive.Header className="flex items-center gap-2 pr-3 transition-colors hover:bg-muted/60 data-[state=open]:border-b data-[state=open]:border-border">
                <AccordionPrimitive.Trigger className="group flex min-w-0 flex-1 items-center gap-3 py-3 pl-4 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring">
                  <ChevronRight
                    aria-hidden="true"
                    className="h-4 w-4 shrink-0 text-muted-foreground transition-transform duration-200 group-data-[state=open]:rotate-90 motion-reduce:transition-none"
                  />
                  <span className="flex min-w-0 flex-1 flex-col gap-1 sm:flex-row sm:items-center sm:gap-3">
                    <span className="font-bold text-foreground">{group.label}</span>
                    <span className="flex flex-wrap items-center gap-2 text-small font-normal text-muted-foreground">
                      {latest ? (
                        <>
                          <StatusBadge className="tabular-nums">{group.files.length}件</StatusBadge>
                          最新 {formatUploadedAt(latest.uploadedAt)}
                        </>
                      ) : (
                        '未アップロード'
                      )}
                    </span>
                  </span>
                </AccordionPrimitive.Trigger>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  className="shrink-0"
                  aria-label={`${group.label}にファイルを追加`}
                  onClick={() => openUpload(group.type)}
                >
                  <Plus aria-hidden="true" />
                  追加
                </Button>
              </AccordionPrimitive.Header>
              <AccordionContent className="p-0">
                {group.files.length > 0 ? (
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
                ) : (
                  <div className="flex flex-col items-center gap-3 px-4 py-8 text-center">
                    <FolderOpen aria-hidden="true" className="h-8 w-8 text-muted-foreground" />
                    <div>
                      <p className="text-sm text-foreground">
                        {group.label}はまだアップロードされていません。
                      </p>
                      <p className="mt-1 text-small text-muted-foreground">
                        {group.description}「追加」からアップロードできます。
                      </p>
                    </div>
                  </div>
                )}
              </AccordionContent>
            </AccordionItem>
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
