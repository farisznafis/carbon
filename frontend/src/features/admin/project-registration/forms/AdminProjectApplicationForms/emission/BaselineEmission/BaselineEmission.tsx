//ベースライン排出量の考え方

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const baselineEmission: FieldConfig<FormValues>[] = [
  { name: 'baselineEmission.concept', label: '考え方', type: 'text' },
  {
    name: 'baselineEmission.evaluationFormula',
    label: '方法論の評価式',
    type: 'text',
  },
  {
    name: 'baselineEmission.formulaNumber',
    label: '方法論の式番号',
    type: 'text',
  },
  { name: 'baselineEmission.symbol', label: '記号', type: 'text' },
  { name: 'baselineEmission.definition', label: '定義', type: 'text' },
  { name: 'baselineEmission.unit', label: '単位', type: 'text' },
  { name: 'baselineEmission.expectedValue', label: '想定値', type: 'text' },
];
