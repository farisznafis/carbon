"use client";
// Multi-step form component for admin project forms
import type { FieldValues, UseFormReturn } from "react-hook-form";
import type { FieldConfig } from "@/components/forms/shared/GenericFormFields/GenericFormFields";
import { FormProvider } from "react-hook-form";
import { DynamicFormRenderer } from "./DynamicFormRenderer";
import { steps } from "./projectSteps";
import { logger } from "@/lib/logger";
import { FormSection } from '@/components/forms/form-layout';

export type MultiStepFormProps<TFormValues extends FieldValues> = {
  form: UseFormReturn<TFormValues>;
  step: number; // 1-based index
  stepsOverride?: ReadonlyArray<typeof steps[number]>;
  /**
   * Backend field names filled from SQL. Those inputs are disabled: the server
   * overwrites them on every read, so an edit here would be lost without the
   * operator noticing.
   */
  readOnlyFields?: ReadonlyArray<string>;
  /**
   * 有/無 rules from the backend. A dependent field only applies while its
   * toggle holds the given value, so it is hidden otherwise — the server
   * enforces the same rule via @RequiredIf.
   */
  conditionalFields?: ReadonlyArray<{
    field: string;
    when: string;
    is: boolean;
  }>;
};

export function MultiStepForm<TFormValues extends FieldValues>({
  form,
  step,
  stepsOverride,
  readOnlyFields,
  conditionalFields,
}: Readonly<MultiStepFormProps<TFormValues>>) {
  const stepList = stepsOverride ?? steps;
  // Config names are namespaced ("projectOverview.corpNumber"); the backend
  // reports the bare field name.
  const lockedNames = new Set(readOnlyFields ?? []);
  const rulesByField = new Map(
    (conditionalFields ?? []).map((rule) => [rule.field, rule]),
  );

  /**
   * Config names are namespaced ("projectApplication.emitterCode") while the
   * backend reports bare names, so the toggle is looked up under the same
   * prefix as the field it governs.
   */
  const applies = (fieldName: string) => {
    const bare = fieldName.split('.').pop() ?? '';
    const rule = rulesByField.get(bare);
    if (!rule) return true;

    const prefix = fieldName.slice(0, fieldName.length - bare.length);
    return form.getValues(`${prefix}${rule.when}` as never) === rule.is;
  };
  const current = stepList.find((s) => s.id === step);

  if (!current) {
    logger.error(`Step with ID ${step} not found`, {
      availableStepIds: stepList.map((s) => s.id),
    });
    return null;
  }

  return (
    <FormSection number={step} title={current.title}>
      <FormProvider {...form}>
        <DynamicFormRenderer<TFormValues>
          fields={
            (lockedNames.size === 0 && rulesByField.size === 0
              ? current.fields
              : current.fields
                  .filter((field) => applies(String(field.name)))
                  .map((field) => {
                    const bare = String(field.name).split('.').pop() ?? '';
                    return lockedNames.has(bare)
                      ? { ...field, disabled: true }
                      : field;
                  })) as FieldConfig<TFormValues>[]
          }
        />
      </FormProvider>
    </FormSection>
  );
}
