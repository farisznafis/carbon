// /モニタリング項目_計画

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const monitoringItem: FieldConfig<FormValues>[] = [
  { name: 'monitoringItem.symbol', label: '記号', type: 'text' },
  { name: 'monitoringItem.definition', label: '定義', type: 'textarea' },
  { name: 'monitoringItem.unit', label: '単位', type: 'text' },
];
