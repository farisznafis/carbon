import { useEffect, useState } from 'react';
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
  /** Type chosen beforehand, e.g. from an empty section's upload button. */
  initialType?: ProjectFileType | null;
  isUploading: boolean;
  /** Resolves when the upload succeeded; rejects with the reason it did not. */
  onUpload: (fileType: ProjectFileType, file: File) => Promise<void>;
  /** Closes the dialog and opens the type's section, so a version can be deleted. */
  onShowFiles: (fileType: ProjectFileType) => void;
}>;

const errorMessage = (error: unknown) =>
  error instanceof Error && error.message
    ? error.message
    : 'アップロードできませんでした。時間をおいて再度お試しください。';

/**
 * Adds one file as the next version of the chosen document type.
 *
 * The version limit stays out of sight until it matters: only a type that is
 * already full shows a warning, because the server would refuse the upload.
 */
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

  const selectedGroup = groups.find((group) => group.type === fileType);
  const isFull = selectedGroup ? !selectedGroup.canUpload : false;
  const nextVersion = selectedGroup ? (selectedGroup.files[0]?.version ?? 0) + 1 : null;

  const handleOpenChange = (next: boolean) => {
    if (isUploading) return;
    onOpenChange(next);
  };

  const handleFileChange = (chosen: File | null) => {
    const problem = chosen ? projectFileProblem(chosen) : null;
    setFileError(problem);
    setFile(problem ? null : chosen);
  };

  const handleSubmit = async () => {
    if (!fileType || !file || isFull) return;
    setSubmitError(null);
    try {
      await onUpload(fileType, file);
      onOpenChange(false);
    } catch (error) {
      setSubmitError(errorMessage(error));
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>ファイルをアップロード</DialogTitle>
          <DialogDescription>
            書類の種類を選び、アップロードするファイルを指定してください。
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-5">
          <div className="space-y-2">
            <Label htmlFor="project-file-type" className="inline-flex items-center gap-1 text-small">
              書類の種類
              <RequiredBadge />
            </Label>
            <Select
              value={fileType ?? ''}
              onValueChange={(value) => setFileType(value as ProjectFileType)}
              disabled={isUploading}
            >
              <SelectTrigger
                id="project-file-type"
                aria-required="true"
                aria-describedby="project-file-type-help"
              >
                <SelectValue placeholder="選択してください" />
              </SelectTrigger>
              <SelectContent>
                {PROJECT_FILE_STAGES.map(({ stage, label }) => (
                  <SelectGroup key={stage}>
                    <SelectLabel className="text-small text-muted-foreground">{label}</SelectLabel>
                    {groups
                      .filter((group) => group.stage === stage)
                      .map((group) => (
                        <SelectItem key={group.type} value={group.type}>
                          {group.label}
                        </SelectItem>
                      ))}
                  </SelectGroup>
                ))}
              </SelectContent>
            </Select>
            {/* Said here, where the upload is decided, rather than on every list row. */}
            <p id="project-file-type-help" className="text-small text-muted-foreground">
              各書類は{groups[0]?.limit ?? DEFAULT_PROJECT_FILE_VERSION_LIMIT}件まで保存できます。
            </p>
          </div>

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
              新しいファイルをアップロードするには、既存のファイルを1件削除してください。
            </NoticeBanner>
          ) : (
            <div className="space-y-2">
              <FileSelectButton
                id="project-file-input"
                variant="design-system-pro"
                label="ファイル"
                required
                accept={PROJECT_FILE_ACCEPT}
                supportedFormats="(.xlsx, .pdf・10MBまで)"
                value={file}
                onChange={handleFileChange}
              />
              {fileError ? (
                <p role="alert" className="text-sm text-destructive">
                  {fileError}
                </p>
              ) : null}
              {nextVersion && file ? (
                <p className="text-small text-muted-foreground">
                  {selectedGroup?.label}の{' '}
                  <span className="font-bold tabular-nums text-foreground">v{nextVersion}</span>{' '}
                  として追加されます。既存のファイルは置き換えられません。
                </p>
              ) : null}
            </div>
          )}

          {submitError ? (
            <p role="alert" className="text-sm text-destructive">
              {submitError}
            </p>
          ) : null}
        </div>

        <DialogFooter>
          <FormActions
            context="compact"
            className="w-full"
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
                disabled: !fileType || !file || isFull,
                onClick: () => void handleSubmit(),
              },
            ]}
          />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
