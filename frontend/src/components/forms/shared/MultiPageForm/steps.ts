// steps.ts
import type { FieldConfig } from "../GenericFormFields/GenericFormFields";
import type { FormValues } from "./formTypes";

// 1. 各ステップのフィールド配列を定義
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