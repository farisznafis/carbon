// 10.5（１）主要排出活動

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { formulaSymbolColumns } from '../sectionTenRows';

export const mainBaseline: FieldConfig<FormValues>[] = [
  {
    name: 'mainBaseline.evaluationFormula',
    label: '方法論の評価式',
    type: 'text',
    placeholder: '例: EMPJ,S,control=ELPJ,control×CEFelectricity,t',
  },
  { name: 'mainBaseline.formulaNumber', label: '方法論の式番号', type: 'number' },
  {
    name: 'mainBaseline.symbols',
    label: '記号・定義・単位・想定値',
    type: 'table',
    columns: formulaSymbolColumns,
    minRows: 1,
    addRowLabel: '記号を追加',
  },
];
