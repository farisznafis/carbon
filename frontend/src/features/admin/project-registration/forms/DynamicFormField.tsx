// ============================================
// components/forms/DynamicFormField.tsx
// RENAMED TO AVOID CONFLICT ✅
// ============================================
'use client';

import { useState } from 'react';
import { useFormContext, FieldValues, Path, PathValue } from 'react-hook-form';
import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { RequiredBadge } from '@/components/forms/shared/RequiredLabel';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Textarea } from "@/components/ui/textarea";
import { cn } from '@/lib/utils';
import { usePlanFileUploader } from './PlanFileUploadContext';
import { PlanTableField } from './PlanTableField';
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";

interface DynamicFormFieldProps<TFormValues extends FieldValues = FieldValues> {
  field: FieldConfig<TFormValues>;
  /**
   * The path the control binds to, when it is not `field.name` — a table cell
   * reuses one column config across every row, so the row index only exists
   * here.
   */
  name?: Path<TFormValues>;
}

/** Walks the nested error object the way react-hook-form nests it. */
export function errorAt(errors: unknown, path: string): { message?: unknown } | undefined {
  let node: unknown = errors;
  for (const key of path.split('.')) {
    if (!node) break;
    node = node instanceof Object ? (node as Record<string, unknown>)[key] : undefined;
  }
  return node as { message?: unknown } | undefined;
}

/**
 * Whether a field is locked outright or by another field's answer.
 *
 * Exported because a table renders its cells itself and still has to apply the
 * same rules — a column locked because SQL fills it must be locked in every
 * row, not only where a label happens to be drawn.
 */
export function useFieldDisabled<TFormValues extends FieldValues = FieldValues>(
  field: FieldConfig<TFormValues>,
  prefix = '',
) {
  const form = useFormContext<TFormValues>();

  // Disabled controls are intentionally excluded from required validation.
  const byCondition = () => {
    if (!field.disabledWhen) return false;

    const conditions = Array.isArray(field.disabledWhen)
      ? field.disabledWhen
      : [field.disabledWhen];

    return conditions.every(condition => {
      const value = form.watch(`${prefix}${condition.field}` as Path<TFormValues>);
      return Array.isArray(condition.value)
        ? condition.value.includes(value)
        : value === condition.value;
    });
  };

  // A flat `disabled` wins outright; disabledWhen still covers the rules that
  // key off another field's answer.
  return field.disabled === true || byCondition();
}

export function DynamicFormField<TFormValues extends FieldValues = FieldValues>({
  field,
  name,
}: DynamicFormFieldProps<TFormValues>) {
  const form = useFormContext<TFormValues>();
  const path = (name ?? field.name) as Path<TFormValues>;

  const error = errorAt(form.formState.errors, path as string);
  const hasError = !!error;

  const disabled = useFieldDisabled(field);
  const isRequired = field.required !== false;

  const fieldId = path as string;
  const errorId = `${fieldId}-error`;
  const accessibilityProps = {
    'aria-invalid': hasError,
    'aria-describedby': hasError ? errorId : undefined,
    'aria-required': isRequired,
  } as const;

  // A table draws its own header instead of a single label, and spans the grid.
  if (field.type === 'table') {
    return <PlanTableField<TFormValues> field={field} name={path} />;
  }

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

      <FieldControl<TFormValues>
        field={field}
        name={path}
        fieldId={fieldId}
        disabled={disabled}
        accessibilityProps={accessibilityProps}
      />

      {error && (
        <p id={errorId} className="text-sm text-destructive">
          {error.message?.toString()}
        </p>
      )}
    </div>
  );
}

/**
 * The input itself, without a label or error line.
 *
 * Split out so a table cell binds the same control to `rows.3.symbol` while
 * the label lives once in the column header. Keeping one switch is the point:
 * a second copy would drift the first time a field type gains a behaviour.
 */
export function FieldControl<TFormValues extends FieldValues = FieldValues>({
  field,
  name,
  fieldId,
  disabled,
  accessibilityProps,
}: {
  field: FieldConfig<TFormValues>;
  name: Path<TFormValues>;
  fieldId: string;
  disabled: boolean;
  accessibilityProps: Record<string, unknown>;
}) {
  const form = useFormContext<TFormValues>();
  const registerProps = form.register(name);
  const numberRegisterProps =
    field.type === 'number'
      ? form.register(name, {
          setValueAs: (value) =>
            value === '' || value === null || value === undefined
              ? undefined
              : Number(value),
        })
      : registerProps;

  switch (field.type) {
    case 'email':
      return (
        <Input
          id={fieldId}
          type="email"
          placeholder={field.placeholder}
          {...registerProps}
          {...accessibilityProps}
          disabled={disabled}
        />
      );

    case 'number':
      return (
        <Input
          id={fieldId}
          type="number"
          placeholder={field.placeholder}
          {...numberRegisterProps}
          {...accessibilityProps}
          disabled={disabled}
        />
      );

    case 'date':
      return (
        <Input
          id={fieldId}
          type="date"
          {...registerProps}
          {...accessibilityProps}
          disabled={disabled}
        />
      );

    case 'textarea':
      return (
        <Textarea
          id={fieldId}
          placeholder={field.placeholder}
          {...registerProps}
          {...accessibilityProps}
          disabled={disabled}
        />
      );

    case 'select':
      return (
        <Select
          value={
            form.watch(name) === null || form.watch(name) === undefined
              ? ''
              : String(form.watch(name))
          }
          onValueChange={(value) => {
            const selected = field.options?.find(
              (opt) => String(opt.value) === value
            );
            form.setValue(
              name,
              selected?.value as PathValue<TFormValues, typeof name>,
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
      );

    case 'checkbox':
      return (
        <div className="flex items-center space-x-2">
          <Checkbox
            id={fieldId}
            checked={Boolean(form.watch(name))}
            onCheckedChange={(checked) =>
              form.setValue(
                name,
                (checked === true) as PathValue<TFormValues, typeof name>,
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
      );

    case 'file':
      return (
        <PlanFileInput<TFormValues>
          field={field}
          name={name}
          fieldId={fieldId}
          disabled={disabled}
          accessibilityProps={accessibilityProps}
        />
      );

    default:
      return (
        <Input
          id={fieldId}
          type="text"
          placeholder={field.placeholder}
          {...registerProps}
          {...accessibilityProps}
          disabled={disabled}
        />
      );
  }
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
  name,
  fieldId,
  disabled,
  accessibilityProps,
}: {
  field: FieldConfig<TFormValues>;
  name: Path<TFormValues>;
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

  const attached = uploadedName ?? fileNameOf(form.watch(name));

  const handleChange = async (file: File | undefined) => {
    if (!file) return;

    if (!uploader) {
      form.setValue(name, file as PathValue<TFormValues, typeof name>, {
        shouldDirty: true,
        shouldValidate: true,
      });
      return;
    }

    setUploading(true);
    setUploadError(null);
    try {
      const { s3Key, originalName } = await uploader(String(name), file);
      form.setValue(name, s3Key as PathValue<TFormValues, typeof name>, {
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
