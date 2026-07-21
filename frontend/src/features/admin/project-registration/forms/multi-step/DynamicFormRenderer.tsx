'use client';

import { useFormContext, type FieldValues } from 'react-hook-form';
import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { DynamicFormField } from './DynamicFormField';

interface DynamicFormRendererProps<TFormValues extends FieldValues = FieldValues> {
  fields: FieldConfig<TFormValues>[];
  className?: string;
}

export function DynamicFormRenderer<TFormValues extends FieldValues = FieldValues>({ 
  fields, 
  className = 'space-y-4' 
}: DynamicFormRendererProps<TFormValues>) {
  const form = useFormContext<TFormValues>();

  const matchesConditionValue = (
    actualValue: unknown,
    expectedValue: unknown | unknown[],
  ) => {
    return Array.isArray(expectedValue)
      ? expectedValue.includes(actualValue)
      : actualValue === expectedValue;
  };

  const isFieldVisible = (field: FieldConfig<TFormValues>) => {
    if (field.hideWhen) {
      const hideConditions = Array.isArray(field.hideWhen) 
        ? field.hideWhen 
        : [field.hideWhen];

      const shouldHide = hideConditions.every(condition => {
        const value = form.watch(condition.field);
        return matchesConditionValue(value, condition.value);
      });

      if (shouldHide) return false;
    }

    if (!field.showWhen) return true;

    const showConditions = Array.isArray(field.showWhen) 
      ? field.showWhen 
      : [field.showWhen];

    return showConditions.every(condition => {
      const value = form.watch(condition.field);
      return matchesConditionValue(value, condition.value);
    });
  };

  return (
    <div className={className}>
      {fields.map((field) => {
        try {
          if (!isFieldVisible(field)) {
            return null;
          }

          return (
            <DynamicFormField<TFormValues> 
              key={field.name as string} 
              field={field} 
            />
          );
        } catch {
          return <div key={field.name as string}>Error rendering {field.name}</div>;
        }
      })}
    </div>
  );
}
