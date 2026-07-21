# Multi-step React Hook Form (with Draft Save)

This document explains the multi-step form structure using `react-hook-form`.

Main pieces:

- `FormValues` type
- `FieldConfig` / `GenericFormFields`
- `steps.ts`
- `MultiStepForm.tsx`
- `ParentForm.tsx` (Next / Back / Save / Submit)

---

## 1. Types and generic field renderer

### `FormValues`

Global type for all form values across every step.

```ts
// formTypes.ts
export type FormValues = {
  email: string;
  age?: number;
  country: string;
  birthday: string;
};
```

### `FieldConfig` and `GenericFormFields`

`FieldConfig` describes one field (`name`, `label`, `type`, etc.).  
`GenericFormFields` receives an array of configs and renders them with RHF.

```ts
// GenericFormFields.tsx (conceptual)
export type FieldConfig<TValues> = {
  name: keyof TValues;
  label: string;
  type: "text" | "email" | "number" | "select" | "date";
  options?: { label: string; value: string }[];
};

export function GenericFormFields<TValues>({
  form,
  fields,
}: {
  form: UseFormReturn<TValues>;
  fields: FieldConfig<TValues>[];
}) {
  // map fields → RHF <FormField> components
}
```

Important: `name` must be a key of `FormValues`.

---

## 2. `steps.ts` – fields per step

Define which fields appear on each step, then group them into a `steps` array.

```ts
// steps.ts
import type { FieldConfig } from "./GenericFormFields";
import type { FormValues } from "./formTypes";

export const step1Fields: FieldConfig<FormValues>[] = [
  { name: "email", label: "Email", type: "email" },
  { name: "age", label: "Age", type: "number" },
];

export const step2Fields: FieldConfig<FormValues>[] = [
  {
    name: "country",
    label: "Country",
    type: "select",
    options: [
      { label: "USA", value: "us" },
      { label: "Japan", value: "jp" },
    ],
  },
];

export const step3Fields: FieldConfig<FormValues>[] = [
  { name: "birthday", label: "Birthday", type: "date" },
];

export const steps: FieldConfig<FormValues>[][] = [
  step1Fields,
  step2Fields,
  step3Fields,
];

export const maxStep = steps.length;
```

- `steps[0]` → step 1 fields, `steps[1]` → step 2, etc.
- To add a step: create a new `stepXFields` array and include it in `steps`.

---

## 3. `MultiStepForm.tsx` – render current step only

`ParentForm` passes `step` (1-based) and `form`.  
`MultiStepForm` picks the correct `fields` and renders them.

```tsx
// MultiStepForm.tsx
"use client";

import type { UseFormReturn } from "react-hook-form";
import { GenericFormFields } from "./GenericFormFields";
import type { FormValues } from "./formTypes";
import { steps, maxStep } from "./steps";

type MultiStepFormProps = {
  form: UseFormReturn<FormValues>;
  step: number; // 1-based
};

export { maxStep };

export function MultiStepForm({ form, step }: MultiStepFormProps) {
  const fields = steps[step - 1];

  return (
    <div className="space-y-4">
      <GenericFormFields form={form} fields={fields} />
    </div>
  );
}
```

Role: only “switch fields based on current step.”  
It does NOT own `<form>` or buttons; the parent does.

---

## 4. `ParentForm.tsx` – step logic and buttons

`ParentForm` is responsible for:

- Initializing `useForm`
- Managing `step` state
- Next / Back / Save / Submit logic

```tsx
// ParentForm.tsx
"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { Form } from "@/components/ui/form";
import type { FormValues } from "./formTypes";
import { MultiStepForm } from "./MultiStepForm";
import { steps, maxStep } from "./steps";

export function ParentForm() {
  const [step, setStep] = useState(1);

  const form = useForm<FormValues>({
    defaultValues: {
      email: "",
      age: undefined,
      country: "",
      birthday: "",
    },
    mode: "onChange",
  });

  // Next: validate only current step fields, then move forward
  const goNext = async () => {
    const currentFields = steps[step - 1].map(f => f.name as any);
    const ok = await form.trigger(currentFields);
    if (!ok) return;
    setStep(s => Math.min(s + 1, maxStep));
  };

  // Back: just go back one step
  const goBack = () => {
    setStep(s => Math.max(s - 1, 1));
  };

  // Save: get all form values and save as draft
  const onSave = () => {
    const values = form.getValues();
    console.log("Save draft", values);
    // Call your API here
  };

  // Submit: final submit (only on last step)
  const onSubmit = (values: FormValues) => {
    console.log("Final submit", values);
    // Final submission logic
  };

  const isLastStep = step === maxStep;

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit(onSubmit)}
        className="space-y-6"
      >
        <MultiStepForm form={form} step={step} />

        <div className="flex gap-2 justify-between">
          {/* Left: Save draft */}
          <button
            type="button"
            onClick={onSave}
            className="border px-3 py-1"
          >
            Save
          </button>

          {/* Right: Back / Next / Submit */}
          <div className="flex gap-2">
            {step > 1 && (
              <button
                type="button"
                onClick={goBack}
                className="border px-3 py-1"
              >
                Back
              </button>
            )}

            {!isLastStep && (
              <button
                type="button"
                onClick={goNext}
                className="border px-3 py-1"
              >
                Next
              </button>
            )}

            {isLastStep && (
              <button
                type="submit"
                className="border px-3 py-1"
              >
                Submit
              </button>
            )}
          </div>
        </div>
      </form>
    </Form>
  );
}
```

### Button behavior

- **Next**
  - Uses `form.trigger` on only the current step’s fields
  - If validation fails, stay on the same step
  - If OK, increments `step`
- **Back**
  - Decrements `step` (no validation)
  - Field values are preserved by `react-hook-form`
- **Save**
  - Uses `form.getValues()` to get all form data
  - Ideal for draft-save API calls
- **Submit**
  - Only shown on the last step
  - `onSubmit` receives all values for final submission

---

## 5. How to extend/customize

- Add a step: create another `stepXFields` and append it to `steps`
- Add validation: use a resolver (e.g., zod) or RHF’s validation options
- Sync step with URL: manage `step` via query params or route segments
- Real draft saving: replace `console.log` in `onSave` with an API call

Core idea: a single `useForm` instance holds all data; `steps` controls which fields are visible and validated at each step.