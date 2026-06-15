// this file for help hide optional forms
'use client';

import { useFormContext, FieldValues } from 'react-hook-form';
import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { DynamicFormField } from './DynamicFormField';
import { logger } from '@/lib/logger';

interface DynamicFormRendererProps<TFormValues extends FieldValues = FieldValues> {
  fields: FieldConfig<TFormValues>[];
  className?: string;
}

export function DynamicFormRenderer<TFormValues extends FieldValues = FieldValues>({ 
  fields, 
  className = 'space-y-4' 
}: DynamicFormRendererProps<TFormValues>) {
  const form = useFormContext<TFormValues>();

  const isFieldVisible = (field: FieldConfig<TFormValues>) => {
    // Check hideWhen first
    if (field.hideWhen) {
      const hideConditions = Array.isArray(field.hideWhen) 
        ? field.hideWhen 
        : [field.hideWhen];

      const shouldHide = hideConditions.every(condition => {
        const value = form.watch(condition.field);
        return Array.isArray(condition.value)
          ? condition.value.includes(value)
          : value === condition.value;
      });

      if (shouldHide) return false; // Don't render
    }

    // Check showWhen
    if (!field.showWhen) return true;

    const showConditions = Array.isArray(field.showWhen) 
      ? field.showWhen 
      : [field.showWhen];

    return showConditions.every(condition => {
      const value = form.watch(condition.field);
      return Array.isArray(condition.value)
        ? condition.value.includes(value)
        : value === condition.value;
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
        } catch (error) {
          logger.error('Error rendering field', field.name, error);
          return <div key={field.name as string}>Error rendering {field.name}</div>;
        }
      })}
    </div>
  );
}
