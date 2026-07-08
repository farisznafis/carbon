// FileSelectButton.tsx
import { useState } from 'react';
import { IconCloudUpload } from '@tabler/icons-react';
import { cn } from '@/lib/utils';

type Props = {
  id: string;
  value?: File | null;
  onChange: (file: File | null) => void;
  label?: string;
  className?: string;
  supportedFormats?: string;
  helperText?: string;
  variant?: 'default' | 'design-system-pro';
};

export function FileSelectButton({
  id,
  value,
  onChange,
  label = 'ファイルを選択',
  className = '',
  supportedFormats = '(.pdf, .csv, .jpg, .jpeg, .png)',
  helperText,
  variant = 'default',
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
        <p className="text-small text-foreground">{label}</p>
        <label
          htmlFor={id}
          onDragEnter={handleDragEnter}
          onDragLeave={handleDragLeave}
          onDragOver={handleDragOver}
          onDrop={handleDrop}
          className={cn(
            'flex min-h-[66px] w-full cursor-pointer items-center gap-2 rounded-[6px] border-2 border-dashed py-3 pl-4 pr-4 transition-all duration-200',
            isDragging
              ? 'border-primary bg-primary/20 scale-[1.02]'
              : hasFile
                ? 'border-primary bg-[#EFF6EF] hover:bg-[#E8F1E8]'
                : 'border-[#B1B5BB] bg-[#F6F6F6] hover:border-primary hover:bg-secondary/40',
          )}
        >
          <span
            className={cn(
              'flex h-10 w-10 shrink-0 items-center justify-center rounded-[8px] border text-primary transition-all duration-200',
              isDragging
                ? 'border-primary bg-white scale-110'
                : hasFile
                  ? 'border-primary bg-white'
                  : 'border-[#B1B5BB] bg-secondary',
            )}
          >
            <IconCloudUpload className={cn('h-6 w-6 transition-transform duration-200', isDragging && 'scale-110')} />
          </span>
          <span className="flex min-w-0 flex-1 flex-col gap-0.5 xl:flex-row xl:items-center xl:gap-1">
            <span className="truncate text-base font-normal leading-7 text-[#1F2628]">
              {value?.name || 'ファイルを選択'}
            </span>
            <span className="text-sm font-normal leading-5 text-black/70">
              {supportedFormats}
            </span>
          </span>
          {hasFile && (
            <span className="ml-auto inline-flex shrink-0 items-center rounded-full bg-primary px-3 py-1 text-xs font-medium leading-4 text-primary-foreground">
              アップロード済み
            </span>
          )}
        </label>
        <input
          id={id}
          type="file"
          className="hidden"
          onChange={(e) => {
            const file = e.target.files?.[0] ?? null;
            onChange(file);
          }}
        />
        {helperText && (
          <p className="text-sm font-normal leading-5 text-slate-500">
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
        className="flex w-fit cursor-pointer items-center rounded-md bg-gray-200 px-3 py-1 text-sm text-gray-800 shadow-sm hover:bg-gray-300"
      >
        <IconCloudUpload className="mr-2 h-4 w-4" />
        {label}
      </label>
      <input
        id={id}
        type="file"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0] ?? null;
          onChange(file);
        }}
      />
      {value && <span className="text-sm text-gray-700">{value.name}</span>}
    </div>
  );
}
