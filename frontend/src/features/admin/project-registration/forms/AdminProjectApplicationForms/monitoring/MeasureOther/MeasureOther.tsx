//計量器の概要（特定計量器以外の計量器の場合）

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const measureOther: FieldConfig<FormValues>[] = [
  {
    name: 'measureOther.monitoringItem',
    label: 'モニタリング項目',
    type: 'textarea',
  },
  { name: 'measureOther.instrumentType', label: '計量器の種類', type: 'text' },
  {
    name: 'measureOther.monitoringPoint',
    label: 'モニタリングポイント',
    type: 'text',
  },
  {
    name: 'measureOther.instrumentCalibration',
    label: '計量器の校正方法の説明',
    type: 'textarea',
  },
  {
    name: 'measureOther.monitoringPointDiagram',
    label: 'モニタリングポイントの図示',
    type: 'file',
  },
];
