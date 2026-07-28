// プロジェクト計画での想定（係数）

import { FieldConfig } from "@/components/forms/shared/GenericFormFields/GenericFormFields";
import { FormValues } from "@/features/admin/project-registration/forms/projectFormTypes";

export const expectedProjectPlanCoefficient: FieldConfig<FormValues>[] = [
  {
    name: "expectedProjectPlanCoefficient.expectedValue",
    label: "想定値",
    type: "text",
  },
  { name: "expectedProjectPlanCoefficient.basis", label: "根拠", type: "textarea" },
  {
    name: "expectedProjectPlanCoefficient.remarks",
    label: "備考",
    type: "textarea",
  },
];
