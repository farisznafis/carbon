// ============================================
// components/forms/DynamicFormField.tsx
// RENAMED TO AVOID CONFLICT ✅
// ============================================
'use client';

import { useEffect, useRef, useState } from 'react';
import { useFormContext, FieldValues, Path, PathValue } from 'react-hook-form';
import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { RequiredBadge } from '@/components/forms/shared/RequiredLabel';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Textarea } from "@/components/ui/textarea";
import { cn } from '@/lib/utils';
import { lookupPostalCode, normalizePostalCode } from '@/lib/postalCode';
import { usePlanFileUploader } from './PlanFileUploadContext';
import { PlanTableField } from './PlanTableField';
import { joinPersonName, splitPersonName, type PersonNameParts } from './personName';
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

    case 'postalCode':
      return (
        <PostalCodeInput<TFormValues>
          field={field}
          name={name}
          fieldId={fieldId}
          disabled={disabled}
          accessibilityProps={accessibilityProps}
        />
      );

    case 'personName':
      return (
        <PersonNameInput<TFormValues>
          name={name}
          fieldId={fieldId}
          disabled={disabled}
          accessibilityProps={accessibilityProps}
        />
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

/**
 * 郵便番号 that fills 都道府県 / 市区町村 / 以降 as soon as it has seven digits.
 *
 * The three address fields stay the official sheet's own, and stay editable:
 * the lookup only gets the operator as far as 町域, and they add 番地・建物名.
 * 以降 is filled only while it is empty or still holds what an earlier lookup
 * put there, so correcting the code never wipes a typed 番地.
 */
function PostalCodeInput<TFormValues extends FieldValues = FieldValues>({
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
  const [status, setStatus] = useState<
    { kind: 'idle' } | { kind: 'searching' } | { kind: 'filled' } | { kind: 'error'; message: string }
  >({ kind: 'idle' });
  const lastFilledTown = useRef<string | null>(null);
  const pending = useRef<AbortController | null>(null);

  useEffect(() => () => pending.current?.abort(), []);

  const sibling = (relative: string) => {
    const path = String(name);
    const parent = path.includes('.') ? path.slice(0, path.lastIndexOf('.') + 1) : '';
    return `${parent}${relative}` as Path<TFormValues>;
  };

  const set = (relative: string, value: string) =>
    form.setValue(sibling(relative), value as PathValue<TFormValues, Path<TFormValues>>, {
      shouldDirty: true,
      shouldValidate: true,
    });

  const handleChange = async (raw: string) => {
    form.setValue(name, raw as PathValue<TFormValues, typeof name>, { shouldDirty: true });

    const digits = normalizePostalCode(raw);
    const targets = field.addressFields;
    if (!digits || !targets) {
      setStatus({ kind: 'idle' });
      return;
    }
    // Stored as the seven digits, whatever was typed around them.
    form.setValue(name, digits as PathValue<TFormValues, typeof name>, { shouldDirty: true });

    pending.current?.abort();
    const controller = new AbortController();
    pending.current = controller;
    setStatus({ kind: 'searching' });

    try {
      const address = await lookupPostalCode(digits, controller.signal);
      if (!address) {
        setStatus({ kind: 'error', message: '該当する住所が見つかりません。住所を直接入力してください。' });
        return;
      }

      set(targets.prefecture, address.prefecture);
      set(targets.city, address.city);

      const current = String(form.getValues(sibling(targets.address)) ?? '');
      if (current.trim() === '' || current === lastFilledTown.current) {
        set(targets.address, address.town);
        lastFilledTown.current = address.town;
      }
      setStatus({ kind: 'filled' });
    } catch (e) {
      if (e instanceof DOMException && e.name === 'AbortError') return;
      setStatus({ kind: 'error', message: '住所を取得できませんでした。住所を直接入力してください。' });
    }
  };

  const value = form.watch(name);

  return (
    <div className="space-y-1">
      <Input
        id={fieldId}
        type="text"
        inputMode="numeric"
        autoComplete="postal-code"
        placeholder={field.placeholder ?? '1000005'}
        maxLength={8}
        value={value === null || value === undefined ? '' : String(value)}
        onChange={(e) => void handleChange(e.target.value)}
        {...accessibilityProps}
        disabled={disabled}
      />
      {status.kind === 'searching' && (
        <p className="text-sm text-muted-foreground">住所を検索中...</p>
      )}
      {status.kind === 'filled' && (
        <p className="text-sm text-muted-foreground">
          住所を入力しました。番地・建物名を追記してください。
        </p>
      )}
      {status.kind === 'error' && <p className="text-sm text-destructive">{status.message}</p>}
    </div>
  );
}

/**
 * 姓 and 名 side by side, kept in the form as the single 氏名 the plan stores.
 *
 * The parts live in local state rather than being re-split from the value on
 * every keystroke: with 姓 still empty, "太郎" typed into 名 would otherwise
 * come back as a 姓. The value is only re-split when it changes from outside —
 * loaded from the server, say.
 */
function PersonNameInput<TFormValues extends FieldValues = FieldValues>({
  name,
  fieldId,
  disabled,
  accessibilityProps,
}: {
  name: Path<TFormValues>;
  fieldId: string;
  disabled: boolean;
  accessibilityProps: Record<string, unknown>;
}) {
  const form = useFormContext<TFormValues>();
  const value = form.watch(name);
  const [parts, setParts] = useState<PersonNameParts>(() => splitPersonName(value));

  useEffect(() => {
    const current = typeof value === 'string' ? value : '';
    if (current.trim() !== joinPersonName(parts)) {
      setParts(splitPersonName(current));
    }
    // Only an outside change to the value should re-split it; `parts` changing
    // is this input's own typing.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  const update = (next: PersonNameParts) => {
    setParts(next);
    form.setValue(name, joinPersonName(next) as PathValue<TFormValues, typeof name>, {
      shouldDirty: true,
      shouldValidate: true,
    });
  };

  return (
    <div className="grid grid-cols-2 gap-3">
      <Input
        id={fieldId}
        type="text"
        placeholder="姓（例: 山田）"
        aria-label="姓"
        value={parts.lastName}
        onChange={(e) => update({ ...parts, lastName: e.target.value })}
        onBlur={() => form.trigger(name)}
        {...accessibilityProps}
        disabled={disabled}
      />
      <Input
        id={`${fieldId}-first`}
        type="text"
        placeholder="名（例: 太郎）"
        aria-label="名"
        value={parts.firstName}
        onChange={(e) => update({ ...parts, firstName: e.target.value })}
        onBlur={() => form.trigger(name)}
        {...accessibilityProps}
        disabled={disabled}
      />
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
