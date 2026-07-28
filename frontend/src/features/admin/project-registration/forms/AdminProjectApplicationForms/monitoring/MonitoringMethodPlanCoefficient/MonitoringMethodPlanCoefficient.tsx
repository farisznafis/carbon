// モニタリング方法_計画（係数）

import { FieldConfig } from "@/components/forms/shared/GenericFormFields/GenericFormFields";
import { FormValues } from "@/features/admin/project-registration/forms/projectFormTypes";

export const monitoringMethodPlanCoefficient: FieldConfig<FormValues>[] = [
  {
    name: "monitoringMethodPlanCoefficient.classification",
    label: "想定値",
    type: "text",
  },
  {
    name: "monitoringMethodPlanCoefficient.overview",
    label: "根拠",
    type: "textarea",
  },
  {
    name: "monitoringMethodPlanCoefficient.frequency",
    label: "備考",
    type: "text",
  },
];
