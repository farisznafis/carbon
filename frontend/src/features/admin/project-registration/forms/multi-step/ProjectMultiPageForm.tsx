"use client";
import type { FieldValues, UseFormReturn } from "react-hook-form";
import { FormProvider } from "react-hook-form";
import { DynamicFormRenderer } from "./DynamicFormRenderer";
import type { FormValues } from "../projectFormTypes";
import { steps } from "../projectSteps";
import type { FieldConfig } from "@/components/forms/shared/GenericFormFields/GenericFormFields";
import { logger } from "@/lib/logger";

export type FormStep<TFormValues extends FieldValues = FormValues> = {
  id: number;
  title: string;
  fields: FieldConfig<TFormValues>[];
};

export type MultiStepFormProps<TFormValues extends FieldValues = FormValues> = {
  form: UseFormReturn<TFormValues>;
  step: number; // 1-based index
  stepsOverride?: ReadonlyArray<FormStep<TFormValues>>;
};

const MAX_STEP = steps.length;

export function MultiStepForm<TFormValues extends FieldValues = FormValues>({
  form,
  step,
  stepsOverride,
}: Readonly<MultiStepFormProps<TFormValues>>) {
  const stepList = stepsOverride ?? (steps as ReadonlyArray<FormStep<TFormValues>>);
  const current = stepList.find((s) => s.id === step);

  if (!current) {
    logger.error(`Step with ID ${step} not found`);
    return null;
  }

  return (
    <div className="space-y-4">
      <h2>{current.title}</h2>
      <FormProvider {...form}>
        <DynamicFormRenderer fields={current.fields} />
      </FormProvider>
    </div>
  );
}
