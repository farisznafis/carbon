"use client";
// Multi-step form component for admin project forms
import type { FieldValues, UseFormReturn } from "react-hook-form";
import type { FieldConfig } from "@/components/forms/shared/GenericFormFields/GenericFormFields";
import { FormProvider } from "react-hook-form";
import { DynamicFormRenderer } from "./DynamicFormRenderer";
import { steps } from "./projectSteps";
import { logger } from "@/lib/logger";

export type MultiStepFormProps<TFormValues extends FieldValues> = {
  form: UseFormReturn<TFormValues>;
  step: number; // 1-based index
  stepsOverride?: ReadonlyArray<typeof steps[number]>;
};

export function MultiStepForm<TFormValues extends FieldValues>({
  form,
  step,
  stepsOverride,
}: Readonly<MultiStepFormProps<TFormValues>>) {
  const stepList = stepsOverride ?? steps;
  const current = stepList.find((s) => s.id === step);

  if (!current) {
    logger.error(`Step with ID ${step} not found`, {
      availableStepIds: stepList.map((s) => s.id),
    });
    return null;
  }

  return (
    <div className="space-y-4">
      <h2>{current.title}</h2>
      <FormProvider {...form}>
        <DynamicFormRenderer<TFormValues>
          fields={current.fields as FieldConfig<TFormValues>[]}
        />
      </FormProvider>
    </div>
  );
}
