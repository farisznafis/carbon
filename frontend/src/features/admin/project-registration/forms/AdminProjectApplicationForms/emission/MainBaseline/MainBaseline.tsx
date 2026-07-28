// ベースライン排出量（主要排出活動）
import { FieldConfig } from "@/components/forms/shared/GenericFormFields/GenericFormFields";
import { FormValues } from "@/features/admin/project-registration/forms/projectFormTypes";

export const mainBaseline: FieldConfig<FormValues>[] = [
    { name: "mainBaseline.evaluationFormula", label: "方法論の評価式", type: "text" },
    { name: "mainBaseline.formulaNumber", label: "方法論の式番号", type: "text" },
    { name: "mainBaseline.symbol", label: "記号", type: "text" },
    { name: "mainBaseline.definition", label: "定義", type: "text" },
    { name: "mainBaseline.unit", label: "単位", type: "text" },
    { name: "mainBaseline.expectedValue", label: "想定値", type: "text" },

];