//計量器の概要（特定計量器の場合）

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const measureSpecified: FieldConfig<FormValues>[] = [
  {
    name: 'measureSpecified.monitoringItem',
    label: 'モニタリング項目',
    type: 'textarea',
  },
  {
    name: 'measureSpecified.instrumentType',
    label: '計量器の種類',
    type: 'text',
  },
  {
    name: 'measureSpecified.monitoringPoint',
    label: 'モニタリングポイント',
    type: 'text',
  },
  {
    name: 'measureSpecified.validityPeriod',
    label: '検定の有効期限',
    type: 'text',
  },
  {
    name: 'measureSpecified.methodCollImg',
    label: '備モニタリングポイントの図示考',
    type: 'file',
  },
];
