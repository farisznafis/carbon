//モニタリング方法_計画

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const monitoringPlan: FieldConfig<FormValues>[] = [
  { name: 'monitoringPlan.itemList', label: 'モニタリング項目', type: 'textarea' },
  {
    name: 'monitoringPlan.methodCollDesc',
    label: 'モニタリングデータの収集方法の説明',
    type: 'textarea',
  },
  {
    name: 'monitoringPlan.methodCollDescImg',
    label: 'モニタリングデータの収集方法の説明（画像）',
    type: 'file',
  },
];
