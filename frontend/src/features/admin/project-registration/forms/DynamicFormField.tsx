// ============================================
// components/forms/DynamicFormField.tsx
// RENAMED TO AVOID CONFLICT ✅
// ============================================
'use client';

import { useFormContext, FieldValues, PathValue } from 'react-hook-form';
import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { RequiredBadge } from '@/components/forms/shared/RequiredLabel';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Textarea } from "@/components/ui/textarea";
import { cn } from '@/lib/utils';
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

  const disabled = isDisabled();
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
        <Input
          id={fieldId}
          type="file"
          accept={field.accept}
          multiple={field.multiple}
          // {...registerProps}
          onChange={(e) =>
            form.setValue(
              field.name,
              e.target.files?.[0] as PathValue<TFormValues, typeof field.name>,
              { shouldDirty: true, shouldValidate: true },
            )
          }
          {...accessibilityProps}
          disabled={disabled}
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
