//係数の実測方法に関する説明

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const coefficient: FieldConfig<FormValues>[] = [
  {
    name: 'coefficient.monitoringItem',
    label: 'モニタリング項目',
    type: 'text',
  },
  {
    name: 'coefficient.monitoringDesc',
    label: 'モニタリング項目説明',
    type: 'textarea',
  },
  {
    name: 'coefficient.estimationMethod',
    label: '推定・概算方法',
    type: 'textarea',
  },
];
