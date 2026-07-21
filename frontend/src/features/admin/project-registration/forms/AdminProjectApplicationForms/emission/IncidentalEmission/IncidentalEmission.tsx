//ベースライン排出量（付随的な排出活動）

import { FieldConfig } from "@/components/forms/shared/GenericFormFields/GenericFormFields";
import { FormValues } from "@/features/admin/project-registration/forms/projectFormTypes";

export const incidentalEmission: FieldConfig<FormValues>[] = [
    { name: "incidentalEmission.evaluationFormula", label: "方法論の評価式", type: "text" },
    { name: "incidentalEmission.formulaNumber", label: "方法論の式番号", type: "text" },
    { name: "incidentalEmission.symbol", label: "記号", type: "text" },
    { name: "incidentalEmission.definition", label: "定義", type: "text" },
    { name: "incidentalEmission.unit", label: "単位", type: "text" },
    { name: "incidentalEmission.expectedValue", label: "想定値", type: "text" },

];