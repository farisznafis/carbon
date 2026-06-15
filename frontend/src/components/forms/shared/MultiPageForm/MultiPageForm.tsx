// MultiStepForm.tsx
"use client";

import type { UseFormReturn } from "react-hook-form";
import { FieldConfig, GenericFormFields } from "../GenericFormFields/GenericFormFields";
import type { FormValues } from "./formTypes";

// import or define your step field configs
import {
  step1Fields,
  step2Fields,
  step3Fields
} from "./steps"; // adjust path / number of steps

type MultiStepFormProps = {
  form: UseFormReturn<FormValues>;
  step: number; // 1-based index
};

const steps: FieldConfig<FormValues>[][] = [
  step1Fields,
  step2Fields,
  step3Fields
];

export const MAX_STEP = steps.length;

export function MultiStepForm({ form, step }: Readonly<MultiStepFormProps>) {
  const fields = steps[step - 1];

  return (
    <div className="space-y-4">
      <GenericFormFields form={form} fields={fields} />
    </div>
  );
}