// FileSelectButton.tsx
import { useState } from 'react';
import { IconCloudUpload } from '@tabler/icons-react';
import { RequiredBadge } from '@/components/forms/shared/RequiredLabel';
import { cn } from '@/lib/utils';

type Props = {
  id: string;
  value?: File | null;
  onChange: (file: File | null) => void;
  label?: string;
  className?: string;
  supportedFormats?: string;
  labelDescription?: string;
  helperText?: string;
  accept?: string;
  variant?: 'default' | 'design-system-pro';
  required?: boolean;
};

export function FileSelectButton({
  id,
  value,
  onChange,
  label = 'ファイルを選択またはドラッグ＆ドロップ',
  className = '',
  supportedFormats = '(.pdf, .csv, .jpg, .jpeg, .png)',
  labelDescription,
  helperText,
  accept,
  variant = 'default',
  required = false,
}: Readonly<Props>) {
  const [isDragging, setIsDragging] = useState(false);
  const [dragCounter, setDragCounter] = useState(0);

  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragCounter((prev) => prev + 1);
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragCounter((prev) => {
      const newCount = prev - 1;
      if (newCount === 0) {
        setIsDragging(false);
      }
      return newCount;
    });
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragCounter(0);
    setIsDragging(false);

    const files = e.dataTransfer.files;
    if (files.length > 0) {
      onChange(files[0]);
    }
  };

  if (variant === 'design-system-pro') {
    const hasFile = Boolean(value);

    return (
      <div className={cn('flex w-full flex-col items-start gap-2', className)}>
        <div className="space-y-1">
          <p className="inline-flex items-center gap-1 text-small text-foreground">
            <span>{label}</span>
            {required && <RequiredBadge />}
          </p>
          {labelDescription && (
            <p className="text-small font-normal text-muted-foreground">
              {labelDescription}
            </p>
          )}
        </div>
        <label
          htmlFor={id}
          onDragEnter={handleDragEnter}
          onDragLeave={handleDragLeave}
          onDragOver={handleDragOver}
          onDrop={handleDrop}
          className={cn(
            'flex min-h-16 w-full cursor-pointer items-center gap-2 rounded-md border-2 border-dashed px-4 py-3 transition-all duration-200',
            isDragging
              ? 'scale-[1.02] border-primary bg-primary/20'
              : hasFile
                ? 'border-primary bg-primary/10 hover:bg-primary/15'
                : 'border-input bg-background hover:border-primary hover:bg-secondary/40',
          )}
        >
          <span
            className={cn(
              'flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border text-primary-strong transition-all duration-200',
              isDragging
                ? 'scale-110 border-primary bg-card'
                : hasFile
                  ? 'border-primary bg-card'
                  : 'border-input bg-secondary',
            )}
          >
            <IconCloudUpload className={cn('h-6 w-6 transition-transform duration-200', isDragging && 'scale-110')} />
          </span>
          <span className="flex min-w-0 flex-1 flex-col gap-0.5">
            <span className="truncate text-base font-normal leading-7 text-foreground">
              {value?.name || 'ファイルを選択またはドラッグ＆ドロップ'}
            </span>
            <span className="text-small font-normal text-foreground">
              {supportedFormats}
            </span>
          </span>
          {hasFile && (
            <span className="ml-auto inline-flex shrink-0 items-center rounded-full bg-primary-strong px-3 py-1 text-xs font-medium leading-4 text-primary-foreground">
              アップロード済み
            </span>
          )}
        </label>
        <input
          id={id}
          type="file"
          accept={accept}
          className="hidden"
          aria-required={required}
          onChange={(e) => {
            const file = e.target.files?.[0] ?? null;
            e.currentTarget.value = '';
            onChange(file);
          }}
        />
        {helperText && (
          <p className="text-small font-normal text-foreground">
            {helperText}
          </p>
        )}
      </div>
    );
  }

  return (
    <div className={cn('space-y-2', className)}>
      <label
        htmlFor={id}
        className="flex w-fit cursor-pointer items-center rounded-md bg-secondary px-3 py-1 text-sm text-secondary-foreground hover:bg-secondary/80"
      >
        <IconCloudUpload className="mr-2 h-4 w-4" />
        {label}
      </label>
      <input
        id={id}
        type="file"
        accept={accept}
        className="hidden"
        aria-required={required}
        onChange={(e) => {
          const file = e.target.files?.[0] ?? null;
          e.currentTarget.value = '';
          onChange(file);
        }}
      />
      {value && <span className="text-sm text-foreground">{value.name}</span>}
    </div>
  );
}
