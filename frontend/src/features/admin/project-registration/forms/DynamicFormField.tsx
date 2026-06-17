// ============================================
// components/forms/DynamicFormField.tsx
// RENAMED TO AVOID CONFLICT ✅
// ============================================
'use client';

import { useFormContext, FieldValues, PathValue } from 'react-hook-form';
import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { Textarea } from "@/components/ui/textarea";
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

  const inputClasses = `
    w-full rounded-md border px-3 py-2 text-sm
    focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
    disabled:cursor-not-allowed disabled:opacity-50
    ${hasError ? 'border-red-500' : 'border-gray-300'}
  `;

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

  // disable const
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

  return (
    <div className="space-y-2">
      <label htmlFor={field.name as string} className="text-sm font-medium block ml-2">
        {field.label}
      </label>

      {field.type === 'text' && (
        <input
          id={field.name as string}
          type="text"
          placeholder={field.placeholder}
          className={inputClasses}
          {...registerProps}
          disabled={disabled}
        />
      )}

      {field.type === 'email' && (
        <input
          id={field.name as string}
          type="email"
          placeholder={field.placeholder}
          className={inputClasses}
          {...registerProps}
          disabled={disabled}
        />
      )}

      {field.type === 'number' && (
        <input
          id={field.name as string}
          type="number"
          placeholder={field.placeholder}
          className={inputClasses}
          {...numberRegisterProps}
          disabled={disabled}
        />
      )}

      {field.type === 'date' && (
        <input
          id={field.name as string}
          type="date"
          className={inputClasses}
          {...registerProps}
          disabled={disabled}
        />
      )}

      {field.type === 'textarea' && (
        <Textarea
          id={field.name as string}
          placeholder={field.placeholder}
          {...registerProps}
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
            form.setValue(field.name, selected?.value as PathValue<TFormValues, typeof field.name>);
          }}
          disabled={disabled}
        >
          <SelectTrigger className={inputClasses}>
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
          <input
            id={field.name as string}
            type="checkbox"
            className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            {...registerProps}
            disabled={disabled}
          />
          <label htmlFor={field.name as string} className="text-sm font-normal">
            {field.placeholder || field.label}
          </label>
        </div>
      )}

      {field.type === 'file' && (
        <input
          id={field.name as string}
          type="file"
          accept={field.accept}
          multiple={field.multiple}
          className={inputClasses}
          // {...registerProps}
          onChange={(e) =>
            form.setValue(
              field.name,
              e.target.files?.[0] as PathValue<TFormValues, typeof field.name>,
            )
          }
          disabled={disabled}
        />
      )}

      {error && (
        <p className="text-sm text-red-500">
          {(error as { message?: unknown }).message?.toString()}
        </p>
      )}
    </div>
  );
}
