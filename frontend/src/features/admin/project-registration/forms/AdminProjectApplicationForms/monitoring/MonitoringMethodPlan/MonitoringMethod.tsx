//モニタリング方法_計画

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const monitoringMethod: FieldConfig<FormValues>[] = [
  { name: 'monitoringMethod.classification', label: '分類', type: 'text' },
  { name: 'monitoringMethod.overview', label: '概要', type: 'textarea' },
  { name: 'monitoringMethod.frequency', label: '頻度', type: 'text' },
];
