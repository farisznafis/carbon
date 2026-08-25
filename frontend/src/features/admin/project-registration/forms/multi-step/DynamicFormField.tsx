'use client';

import {
  useFormContext,
  type FieldPathValue,
  type FieldValues,
} from 'react-hook-form';

import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

interface DynamicFormFieldProps<TFormValues extends FieldValues = FieldValues> {
  field: FieldConfig<TFormValues>;
}

export function DynamicFormField<TFormValues extends FieldValues = FieldValues>({
  field,
}: DynamicFormFieldProps<TFormValues>) {
  const form = useFormContext<TFormValues>();

  const getNestedValue = (
    source: unknown,
    path: string,
  ): { message?: unknown } | undefined => {
    return path.split('.').reduce<unknown>((current, key) => {
      if (!current || typeof current !== 'object') return undefined;
      return (current as Record<string, unknown>)[key];
    }, source) as { message?: unknown } | undefined;
  };

  const matchesConditionValue = (
    actualValue: unknown,
    expectedValue: unknown | unknown[],
  ) => {
    return Array.isArray(expectedValue)
      ? expectedValue.includes(actualValue)
      : actualValue === expectedValue;
  };

  const toFieldValue = (value: unknown) => {
    return value as FieldPathValue<TFormValues, typeof field.name>;
  };

  const error = getNestedValue(form.formState.errors, field.name as string);
  const hasError = Boolean(error);
  const disabledConditions = field.disabledWhen
    ? Array.isArray(field.disabledWhen)
      ? field.disabledWhen
      : [field.disabledWhen]
    : [];
  const disabled =
    disabledConditions.length > 0 &&
    disabledConditions.every((condition) => {
      const value = form.watch(condition.field);
      return matchesConditionValue(value, condition.value);
    });

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

  return (
    <div className="space-y-2">
      <label htmlFor={field.name as string} className="ml-2 block text-sm font-medium">
        {field.label}
      </label>

      {field.type === 'text' && (
        <Input
          id={field.name as string}
          type="text"
          placeholder={field.placeholder}
          aria-invalid={hasError}
          {...registerProps}
          disabled={disabled}
        />
      )}

      {field.type === 'email' && (
        <Input
          id={field.name as string}
          type="email"
          placeholder={field.placeholder}
          aria-invalid={hasError}
          {...registerProps}
          disabled={disabled}
        />
      )}

      {field.type === 'number' && (
        <Input
          id={field.name as string}
          type="number"
          placeholder={field.placeholder}
          aria-invalid={hasError}
          {...numberRegisterProps}
          disabled={disabled}
        />
      )}

      {field.type === 'date' && (
        <Input
          id={field.name as string}
          type="date"
          aria-invalid={hasError}
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
              (option) => String(option.value) === value,
            );
            form.setValue(field.name, toFieldValue(selected?.value));
          }}
          disabled={disabled}
        >
          <SelectTrigger aria-invalid={hasError}>
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
            className="h-4 w-4 rounded border-input text-primary focus:ring-ring"
            {...registerProps}
            disabled={disabled}
          />
          <label htmlFor={field.name as string} className="text-sm font-normal">
            {field.placeholder || field.label}
          </label>
        </div>
      )}

      {field.type === 'file' && (
        <Input
          id={field.name as string}
          type="file"
          accept={field.accept}
          multiple={field.multiple}
          aria-invalid={hasError}
          onChange={(event) => {
            const files = event.target.files;
            const value = field.multiple ? Array.from(files ?? []) : files?.[0] ?? null;
            form.setValue(field.name, toFieldValue(value));
          }}
          disabled={disabled}
        />
      )}

      {error && (
        <p className="text-sm text-destructive">{String(error.message ?? '')}</p>
      )}
    </div>
  );
}
