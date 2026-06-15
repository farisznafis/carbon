// モニタリング項目_計画（係数）

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const monitoringItemPlanCoefficient: FieldConfig<FormValues>[] = [
  {
    name: 'monitoringItemPlanCoefficient.symbol',
    label: '想定値',
    type: 'text',
  },
  {
    name: 'monitoringItemPlanCoefficient.definition',
    label: '根拠',
    type: 'textarea',
  },
  {
    name: 'monitoringItemPlanCoefficient.unit',
    label: '備考',
    type: 'text',
  },
];
