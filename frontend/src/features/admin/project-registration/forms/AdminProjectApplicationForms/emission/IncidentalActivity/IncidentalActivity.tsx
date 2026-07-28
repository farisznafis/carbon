// プロジェクト排出量情報（付随的な排出活動）
import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const incidentalActivity: FieldConfig<FormValues>[] = [
  {
    name: 'incidentalActivity.evaluationFormula',
    label: '方法論の評価式',
    type: 'text',
  },
  {
    name: 'incidentalActivity.formulaNumber',
    label: '方法論の式番号',
    type: 'text',
  },
  { name: 'incidentalActivity.symbol', label: '記号', type: 'text' },
  { name: 'incidentalActivity.definition', label: '定義', type: 'text' },
  { name: 'incidentalActivity.unit', label: '単位', type: 'text' },
  { name: 'incidentalActivity.expectedValue', label: '想定値', type: 'text' },
];
