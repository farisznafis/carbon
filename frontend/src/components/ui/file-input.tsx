import * as React from "react";
import { FileText, Upload, X } from "lucide-react";

import { cn } from "@/lib/utils";

export interface FileInputProps
  extends Omit<
    React.InputHTMLAttributes<HTMLInputElement>,
    "type" | "value" | "onChange"
  > {
  error?: string;
  helperText?: string;
  label?: string;
  onFileChange: (file: File | null) => void;
  value?: File | null;
}

const FileInput = React.forwardRef<HTMLInputElement, FileInputProps>(
  (
    {
      accept,
      className,
      disabled,
      error,
      helperText,
      id,
      label = "ファイルを選択",
      onFileChange,
      required,
      value,
      ...props
    },
    ref,
  ) => {
    const generatedId = React.useId();
    const inputId = id ?? generatedId;
    const localRef = React.useRef<HTMLInputElement | null>(null);

    const setRefs = (node: HTMLInputElement | null) => {
      localRef.current = node;

      if (typeof ref === "function") {
        ref(node);
        return;
      }

      if (ref) {
        ref.current = node;
      }
    };

    const handleClear = () => {
      if (localRef.current) {
        localRef.current.value = "";
      }

      onFileChange(null);
    };

    return (
      <div className={cn("space-y-2", className)}>
        <input
          ref={setRefs}
          id={inputId}
          type="file"
          accept={accept}
          disabled={disabled}
          required={required}
          className="peer sr-only"
          aria-invalid={Boolean(error)}
          onChange={(event) => {
            onFileChange(event.target.files?.[0] ?? null);
          }}
          {...props}
        />
        <label
          htmlFor={inputId}
          className={cn(
            "flex min-h-28 cursor-pointer items-center gap-4 rounded-lg border border-dashed border-input bg-card px-5 py-4 text-foreground shadow-app-sm transition-colors hover:border-primary hover:bg-secondary/40",
            "peer-focus-visible:outline-none peer-focus-visible:ring-2 peer-focus-visible:ring-ring peer-focus-visible:ring-offset-2",
            error && "border-destructive peer-focus-visible:ring-destructive",
            disabled &&
              "cursor-not-allowed border-transparent bg-disabled text-disabled-foreground shadow-none hover:border-transparent hover:bg-disabled",
          )}
          aria-disabled={disabled}
        >
          <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-lg border border-primary/30 bg-secondary text-primary">
            {value ? (
              <FileText className="h-7 w-7" />
            ) : (
              <Upload className="h-7 w-7" />
            )}
          </span>
          <span className="min-w-0 flex-1">
            <span className="block text-xl font-medium leading-7 text-muted-foreground">
              {value ? value.name : label}
              {required && <span className="ml-1 text-destructive">*</span>}
            </span>
          </span>
          {value && !disabled && (
            <button
              type="button"
              className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              onClick={(event) => {
                event.preventDefault();
                handleClear();
              }}
              aria-label="選択したファイルを削除"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </label>
        {(error || helperText || accept) && (
          <p
            className={cn(
              "text-sm leading-6 text-muted-foreground",
              error && "text-destructive",
            )}
          >
            {error || helperText || `対応形式: ${accept}`}
          </p>
        )}
      </div>
    );
  },
);
FileInput.displayName = "FileInput";

export { FileInput };
