// //プロジェクト排出量情報（主要排出活動）

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const mainActivity: FieldConfig<FormValues>[] = [
  { name: 'mainActivity.concept', label: '考え方', type: 'textarea' },
  {
    name: 'mainActivity.evaluationFormula',
    label: '方法論の評価式',
    type: 'text',
  },
  { name: 'mainActivity.formulaNumber', label: '方法論の式番号', type: 'text' },
  { name: 'mainActivity.symbol', label: '記号', type: 'text' },
  { name: 'mainActivity.definition', label: '定義', type: 'text' },
  { name: 'mainActivity.unit', label: '単位', type: 'text' },
  { name: 'mainActivity.expectedValue', label: '想定値', type: 'text' },
];
