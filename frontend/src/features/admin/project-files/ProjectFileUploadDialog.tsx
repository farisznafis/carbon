
import { useEffect, useState } from 'react';
import { FileText, Info } from 'lucide-react';

import {
  PROJECT_FILE_ACCEPT,
  projectFileProblem,
  type ProjectFileType,
} from '@/api/admin/projectFiles';

import { FileSelectButton } from '@/components/inputs/FileSelectButton/FileSelectButton';
import { NoticeBanner } from '@/components/feedback/NoticeBanner';
import { FormActions } from '@/components/forms/form-layout';
import { RequiredBadge } from '@/components/forms/shared/RequiredLabel';
import { Button } from '@/components/ui/button';

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

import { Label } from '@/components/ui/label';

import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

import {
  DEFAULT_PROJECT_FILE_VERSION_LIMIT,
  PROJECT_FILE_STAGES,
  type ProjectFileGroup,
} from './projectFileTypes';

type ProjectFileUploadDialogProps = Readonly<{
  open: boolean;
  onOpenChange: (open: boolean) => void;
  groups: ProjectFileGroup[];
  initialType?: ProjectFileType | null;
  isUploading: boolean;
  onUpload: (
    fileType: ProjectFileType,
    file: File
  ) => Promise<void>;
  onShowFiles: (fileType: ProjectFileType) => void;
}>;

const errorMessage = (error: unknown) =>
  error instanceof Error && error.message
    ? error.message
    : 'アップロードできませんでした。時間をおいて再度お試しください。';

export function ProjectFileUploadDialog({
  open,
  onOpenChange,
  groups,
  initialType = null,
  isUploading,
  onUpload,
  onShowFiles,
}: ProjectFileUploadDialogProps) {
  const [fileType, setFileType] = useState<ProjectFileType | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;

    setFileType(initialType);
    setFile(null);
    setFileError(null);
    setSubmitError(null);
  }, [open, initialType]);

  const hasPresetType = initialType !== null;
  const effectiveFileType = initialType ?? fileType;

  const selectedGroup = groups.find(
    (group) => group.type === effectiveFileType
  );

  const isFull = selectedGroup
    ? !selectedGroup.canUpload
    : false;

  const nextVersion = selectedGroup
    ? (selectedGroup.files[0]?.version ?? 0) + 1
    : null;

  const versionLimit =
    selectedGroup?.limit ??
    groups[0]?.limit ??
    DEFAULT_PROJECT_FILE_VERSION_LIMIT;

  const handleOpenChange = (next: boolean) => {
    if (isUploading) return;
    onOpenChange(next);
  };

  const handleTypeChange = (value: string) => {
    setFileType(value as ProjectFileType);
    setFile(null);
    setFileError(null);
    setSubmitError(null);
  };

  const handleFileChange = (chosen: File | null) => {
    const problem = chosen ? projectFileProblem(chosen) : null;

    setFileError(problem);
    setFile(problem ? null : chosen);
    setSubmitError(null);
  };

  const handleSubmit = async () => {
    if (!effectiveFileType || !file || isFull || isUploading) return;

    setSubmitError(null);

    try {
      await onUpload(effectiveFileType, file);
      onOpenChange(false);
    } catch (error) {
      setSubmitError(errorMessage(error));
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        className="
          max-h-[90dvh]
          w-[calc(100%-2rem)]
          gap-0
          overflow-y-auto
          rounded-xl
          border
          border-border
          bg-card
          p-0
          shadow-app-md
          sm:max-w-[480px]
        "
      >
        {/* Header */}
        <DialogHeader className="space-y-1 px-6 pb-4 pt-6 pr-12 text-left">
          <DialogTitle className="text-lg font-bold leading-7 text-foreground">
            ファイルをアップロード
          </DialogTitle>

          <DialogDescription className="text-sm leading-5 text-muted-foreground">
            {hasPresetType
              ? 'アップロードするファイルを選択してください。'
              : '書類の種類とアップロードするファイルを選択してください。'}
          </DialogDescription>
        </DialogHeader>

        {/* Content */}
        <div className="space-y-5 px-6 pb-6">

          {/* Document type */}
          {hasPresetType ? (
            <div
              className="
                flex items-start gap-3
                rounded-lg
                border border-border
                bg-muted/50
                p-3.5
              "
            >
              <span
                className="
                  flex h-9 w-9 shrink-0
                  items-center justify-center
                  rounded-lg border border-border
                  bg-card text-primary-strong
                "
              >
                <FileText
                  aria-hidden="true"
                  className="h-4 w-4"
                />
              </span>

              <div className="min-w-0 flex-1 space-y-1">
                <p className="text-xs text-muted-foreground">
                  書類の種類
                </p>

                <p className="break-words text-sm font-semibold text-foreground">
                  {selectedGroup?.label ?? '—'}
                </p>

                <p className="text-xs leading-5 text-muted-foreground">
                  各書類は{versionLimit}件まで保存できます。
                </p>
              </div>
            </div>
          ) : (
            <div className="space-y-2">
              <Label
                htmlFor="project-file-type"
                className="inline-flex items-center gap-1 text-sm font-medium"
              >
                書類の種類
                <RequiredBadge />
              </Label>

              <Select
                value={fileType ?? ''}
                onValueChange={handleTypeChange}
                disabled={isUploading}
              >
                <SelectTrigger
                  id="project-file-type"
                  aria-required="true"
                  aria-describedby="project-file-type-help"
                  className="w-full"
                >
                  <SelectValue placeholder="選択してください" />
                </SelectTrigger>

                <SelectContent>
                  {PROJECT_FILE_STAGES.map(({ stage, label }) => (
                    <SelectGroup key={stage}>
                      <SelectLabel className="text-small text-muted-foreground">
                        {label}
                      </SelectLabel>

                      {groups
                        .filter((group) => group.stage === stage)
                        .map((group) => (
                          <SelectItem
                            key={group.type}
                            value={group.type}
                          >
                            {group.label}
                          </SelectItem>
                        ))}
                    </SelectGroup>
                  ))}
                </SelectContent>
              </Select>

              <p
                id="project-file-type-help"
                className="text-xs text-muted-foreground"
              >
                各書類は{versionLimit}件まで保存できます。
              </p>
            </div>
          )}

          {/* File upload or limit warning */}
          {isFull && selectedGroup ? (
            <NoticeBanner
              tone="warning"
              title="アップロードできるファイル数の上限に達しています"
              action={
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={() => onShowFiles(selectedGroup.type)}
                >
                  ファイル一覧を開く
                </Button>
              }
            >
              {selectedGroup.label}は{selectedGroup.limit}件まで保存できます。
              新しいファイルをアップロードするには、
              既存のファイルを1件削除してください。
            </NoticeBanner>
          ) : (
            <div className="space-y-3">
              <FileSelectButton
                id="project-file-input"
                variant="design-system-pro"
                label="ファイル"
                required
                accept={PROJECT_FILE_ACCEPT}
                supportedFormats="(.xlsx, .pdf・10MBまで)"
                value={file}
                onChange={handleFileChange}
                className="
                  [&>label]:min-h-[96px]
                  [&>label]:rounded-lg
                  [&>label]:border-border
                  [&>label]:bg-card
                  [&>label]:px-4
                  [&>label]:py-4
                "
              />

              {fileError && (
                <p
                  role="alert"
                  className="text-sm leading-5 text-destructive"
                >
                  {fileError}
                </p>
              )}

              {/* Version information after selecting a file */}
              {nextVersion && file && (
                <div
                  className="
                    flex items-start gap-2.5
                    rounded-lg bg-accent
                    px-3 py-2.5
                  "
                >
                  <Info
                    aria-hidden="true"
                    className="mt-0.5 h-4 w-4 shrink-0 text-primary-strong"
                  />

                  <p className="text-xs leading-5 text-secondary-foreground">
                    {selectedGroup?.label}の{' '}
                    <span className="font-bold tabular-nums text-foreground">
                      v{nextVersion}
                    </span>{' '}
                    として追加されます。
                    既存のファイルは置き換えられません。
                  </p>
                </div>
              )}
            </div>
          )}

          {/* Submission error */}
          {submitError && (
            <p
              role="alert"
              className="text-sm leading-5 text-destructive"
            >
              {submitError}
            </p>
          )}
        </div>

        {/* Footer */}
        <DialogFooter
          className="
            border-t border-border
            bg-muted/30
            px-6 py-4
          "
        >
          <FormActions
            context="standard"
            className="
              grid-cols-2
              w-full
              gap-3
              sm:ml-auto
              sm:w-[288px]
            "
            disabled={isUploading}
            pendingActionId={isUploading ? 'upload' : null}
            actions={[
              {
                id: 'cancel',
                label: 'キャンセル',
                variant: 'outline',
                onClick: () => handleOpenChange(false),
              },
              {
                id: 'upload',
                label: 'アップロード',
                pendingLabel: 'アップロード中…',
                variant: 'default',
                disabled: !effectiveFileType || !file || isFull,
                onClick: () => void handleSubmit(),
              },
            ]}
          />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
