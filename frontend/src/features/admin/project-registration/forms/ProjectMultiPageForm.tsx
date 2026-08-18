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
};

export function MultiStepForm<TFormValues extends FieldValues>({
  form,
  step,
  stepsOverride,
  readOnlyFields,
}: Readonly<MultiStepFormProps<TFormValues>>) {
  const stepList = stepsOverride ?? steps;
  // Config names are namespaced ("projectOverview.corpNumber"); the backend
  // reports the bare field name.
  const lockedNames = new Set(readOnlyFields ?? []);
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
            (lockedNames.size === 0
              ? current.fields
              : current.fields.map((field) => {
                  const bare = String(field.name).split(".").pop() ?? "";
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
