// ============================================
// components/forms/DynamicFormField.tsx
// RENAMED TO AVOID CONFLICT ✅
// ============================================
'use client';

import { useState } from 'react';
import { useFormContext, FieldValues, PathValue } from 'react-hook-form';
import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { RequiredBadge } from '@/components/forms/shared/RequiredLabel';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Textarea } from "@/components/ui/textarea";
import { cn } from '@/lib/utils';
import { usePlanFileUploader } from './PlanFileUploadContext';
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";

interface DynamicFormFieldProps<TFormValues extends FieldValues = FieldValues> {
  field: FieldConfig<TFormValues>;
}

export function DynamicFormField<TFormValues extends FieldValues = FieldValues>({ 
  field 
}: DynamicFormFieldProps<TFormValues>) {
  const form = useFormContext<TFormValues>();

  const getError = (fieldName: string) => {
    const fieldPath = fieldName.split('.');
    let error: unknown = form.formState.errors;
    for (const key of fieldPath) {
      if (!error) break;
      error = error instanceof Object ? (error as Record<string, unknown>)[key] : undefined;
    }
    return error;
  };

  const error = getError(field.name as string);
  const hasError = !!error;

  // Disabled controls are intentionally excluded from required validation.
  const isDisabled = () => {
    if (!field.disabledWhen) return false;

    const conditions = Array.isArray(field.disabledWhen) 
      ? field.disabledWhen 
      : [field.disabledWhen];

    return conditions.every(condition => {
      const value = form.watch(condition.field);
      return Array.isArray(condition.value)
        ? condition.value.includes(value)
        : value === condition.value;
    });
  };

  // A flat `disabled` wins outright; disabledWhen still covers the rules that
  // key off another field's answer.
  const disabled = field.disabled === true || isDisabled();
  const isRequired = field.required !== false;
  const registerProps = form.register(field.name);
  const numberRegisterProps =
    field.type === 'number'
      ? form.register(field.name, {
          setValueAs: (value) =>
            value === '' || value === null || value === undefined
              ? undefined
              : Number(value),
        })
      : registerProps;

  const fieldId = field.name as string;
  const errorId = `${fieldId}-error`;
  const accessibilityProps = {
    'aria-invalid': hasError,
    'aria-describedby': hasError ? errorId : undefined,
    'aria-required': isRequired,
  } as const;

  return (
    <div
      className={cn(
        'space-y-2',
        ['textarea', 'checkbox', 'file'].includes(field.type) &&
          'md:col-span-2',
      )}
    >
      <label
        htmlFor={fieldId}
        className="inline-flex max-w-full flex-wrap items-center gap-1 text-sm font-medium"
      >
        <span>{field.label}</span>
        {isRequired && <RequiredBadge />}
      </label>

      {field.type === 'text' && (
        <Input
          id={fieldId}
          type="text"
          placeholder={field.placeholder}
          {...registerProps}
          {...accessibilityProps}
          disabled={disabled}
        />
      )}

      {field.type === 'email' && (
        <Input
          id={fieldId}
          type="email"
          placeholder={field.placeholder}
          {...registerProps}
          {...accessibilityProps}
          disabled={disabled}
        />
      )}

      {field.type === 'number' && (
        <Input
          id={fieldId}
          type="number"
          placeholder={field.placeholder}
          {...numberRegisterProps}
          {...accessibilityProps}
          disabled={disabled}
        />
      )}

      {field.type === 'date' && (
        <Input
          id={fieldId}
          type="date"
          {...registerProps}
          {...accessibilityProps}
          disabled={disabled}
        />
      )}

      {field.type === 'textarea' && (
        <Textarea
          id={fieldId}
          placeholder={field.placeholder}
          {...registerProps}
          {...accessibilityProps}
          disabled={disabled}
        />
      )}

      {field.type === 'select' && (
        <Select
          value={
            form.watch(field.name) === null || form.watch(field.name) === undefined
              ? ''
              : String(form.watch(field.name))
          }
          onValueChange={(value) => {
            const selected = field.options?.find(
              (opt) => String(opt.value) === value
            );
            form.setValue(
              field.name,
              selected?.value as PathValue<TFormValues, typeof field.name>,
              { shouldDirty: true, shouldValidate: true },
            );
          }}
          disabled={disabled}
        >
          <SelectTrigger id={fieldId} {...accessibilityProps}>
            <SelectValue placeholder={field.placeholder} />
          </SelectTrigger>
          <SelectContent>
            {field.options?.map((option) => (
              <SelectItem key={String(option.value)} value={String(option.value)}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      )}

      {field.type === 'checkbox' && (
        <div className="flex items-center space-x-2">
          <Checkbox
            id={fieldId}
            checked={Boolean(form.watch(field.name))}
            onCheckedChange={(checked) =>
              form.setValue(
                field.name,
                (checked === true) as PathValue<
                  TFormValues,
                  typeof field.name
                >,
                { shouldDirty: true, shouldValidate: true },
              )
            }
            {...accessibilityProps}
            disabled={disabled}
          />
          <label htmlFor={fieldId} className="text-sm font-normal">
            {field.placeholder || field.label}
          </label>
        </div>
      )}

      {field.type === 'file' && (
        <PlanFileInput<TFormValues>
          field={field}
          fieldId={fieldId}
          disabled={disabled}
          accessibilityProps={accessibilityProps}
        />
      )}

      {error && (
        <p id={errorId} className="text-sm text-destructive">
          {(error as { message?: unknown }).message?.toString()}
        </p>
      )}
    </div>
  );
}

/** The file name out of an S3 key, for showing what is currently attached. */
function fileNameOf(value: unknown) {
  if (typeof value !== 'string' || value === '') return null;
  return value.split('/').pop() ?? value;
}

/**
 * A file input that stores the uploaded key rather than the File itself.
 *
 * Plan pages are validated server-side as text, so the form has to hold the
 * key the upload endpoint returns; keeping the File would send an empty object
 * and the page could never save. Outside the plan wizard there is no uploader
 * and the original raw-File behaviour is kept.
 */
function PlanFileInput<TFormValues extends FieldValues = FieldValues>({
  field,
  fieldId,
  disabled,
  accessibilityProps,
}: {
  field: FieldConfig<TFormValues>;
  fieldId: string;
  disabled?: boolean;
  accessibilityProps: Record<string, unknown>;
}) {
  const form = useFormContext<TFormValues>();
  const uploader = usePlanFileUploader();
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  // Kept from the upload so the operator sees the name they picked; a value
  // loaded from the server only has the key, whose last segment is prefixed
  // with the field name.
  const [uploadedName, setUploadedName] = useState<string | null>(null);

  const attached = uploadedName ?? fileNameOf(form.watch(field.name));

  const handleChange = async (file: File | undefined) => {
    if (!file) return;

    if (!uploader) {
      form.setValue(field.name, file as PathValue<TFormValues, typeof field.name>, {
        shouldDirty: true,
        shouldValidate: true,
      });
      return;
    }

    setUploading(true);
    setUploadError(null);
    try {
      const { s3Key, originalName } = await uploader(String(field.name), file);
      form.setValue(field.name, s3Key as PathValue<TFormValues, typeof field.name>, {
        shouldDirty: true,
        shouldValidate: true,
      });
      setUploadedName(originalName);
    } catch (e) {
      setUploadError(
        e instanceof Error ? e.message : 'ファイルをアップロードできませんでした。',
      );
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="space-y-1">
      <Input
        id={fieldId}
        type="file"
        accept={field.accept ?? 'image/png,image/jpeg,application/pdf'}
        multiple={field.multiple}
        onChange={(e) => void handleChange(e.target.files?.[0])}
        {...accessibilityProps}
        disabled={disabled || uploading}
      />
      {uploading && (
        <p className="text-sm text-muted-foreground">アップロード中...</p>
      )}
      {!uploading && attached && (
        <p className="text-sm text-muted-foreground">添付済み: {attached}</p>
      )}
      {uploadError && <p className="text-sm text-destructive">{uploadError}</p>}
    </div>
  );
}
