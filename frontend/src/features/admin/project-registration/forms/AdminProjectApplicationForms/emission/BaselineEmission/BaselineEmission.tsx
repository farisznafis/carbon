// 10.4 ベースライン排出量の考え方

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { formulaSymbolColumns } from '../sectionTenRows';

export const baselineEmission: FieldConfig<FormValues>[] = [
  { name: 'baselineEmission.concept', label: '考え方', type: 'textarea' },
  {
    name: 'baselineEmission.evaluationFormula',
    label: '方法論の評価式',
    type: 'text',
    placeholder: '例: EMPJ,S,control=ELPJ,control×CEFelectricity,t',
  },
  { name: 'baselineEmission.formulaNumber', label: '方法論の式番号', type: 'number' },
  {
    name: 'baselineEmission.symbols',
    label: '記号・定義・単位・想定値',
    type: 'table',
    columns: formulaSymbolColumns,
    minRows: 1,
    addRowLabel: '記号を追加',
  },
];
