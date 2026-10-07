import { useRef, useState, type ChangeEvent } from 'react';
import { FileText, Upload, X } from 'lucide-react';
import { PROJECT_PLAN_FILE_ACCEPT, projectPlanFileProblem } from '@/api/admin/projectFiles';
import { Button } from '@/components/ui/button';

type PlanFilePickerProps = Readonly<{
  file: File | null;
  onChange: (file: File | null) => void;
  disabled?: boolean;
  /** Label of the button that opens the file dialog. */
  buttonLabel?: string;
}>;

const formatSize = (bytes: number) =>
  bytes >= 1024 * 1024
    ? `${(bytes / (1024 * 1024)).toFixed(1)} MB`
    : `${Math.max(1, Math.round(bytes / 1024))} KB`;

/**
 * Chooses the 計画書 file without sending it.
 *
 * A new project has no id to upload against until it is saved, so the file is
 * held here and uploaded by whoever owns the save. A file the server would
 * refuse — wrong kind, too large — is turned away at once, with the reason.
 */
export function PlanFilePicker({
  file,
  onChange,
  disabled = false,
  buttonLabel = 'ファイルを選択',
}: PlanFilePickerProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [problem, setProblem] = useState<string | null>(null);

  const handleChosen = (event: ChangeEvent<HTMLInputElement>) => {
    const chosen = event.target.files?.[0];
    // Cleared so choosing the same file again still fires a change.
    event.target.value = '';
    if (!chosen) return;

    const reason = projectPlanFileProblem(chosen);
    setProblem(reason);
    if (!reason) onChange(chosen);
  };

  return (
    <div className="space-y-3">
      <input
        ref={inputRef}
        type="file"
        accept={PROJECT_PLAN_FILE_ACCEPT}
        className="sr-only"
        aria-label="計画書ファイル"
        tabIndex={-1}
        onChange={handleChosen}
      />

      <div className="flex flex-wrap items-center gap-3">
        <Button
          type="button"
          variant="outline"
          size="md"
          disabled={disabled}
          onClick={() => inputRef.current?.click()}
        >
          <Upload aria-hidden="true" />
          {buttonLabel}
        </Button>

        {file ? (
          <span className="flex min-w-0 items-center gap-2 text-sm">
            <FileText aria-hidden="true" className="h-4 w-4 shrink-0 text-muted-foreground" />
            <span className="break-all">{file.name}</span>
            <span className="shrink-0 text-muted-foreground">（{formatSize(file.size)}）</span>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              disabled={disabled}
              aria-label="選択したファイルを取り消す"
              onClick={() => onChange(null)}
            >
              <X aria-hidden="true" />
            </Button>
          </span>
        ) : (
          <span className="text-sm text-muted-foreground">ファイルは選択されていません。</span>
        )}
      </div>

      <p className="text-sm text-muted-foreground">
        Excel（.xlsx）またはPDF、10MBまで。
      </p>

      {problem ? (
        <p role="alert" className="text-sm text-destructive">
          {problem}
        </p>
      ) : null}
    </div>
  );
}
