//モニタリング方法等に関する説明

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const monitoringExplanation: FieldConfig<FormValues>[] = [
  {
    name: 'monitoringExplanation.monitoringItem',
    label: 'モニタリング項目',
    type: 'text',
  },
  {
    name: 'monitoringExplanation.monitoringDesc',
    label: 'モニタリング項目説明',
    type: 'textarea',
  },
  {
    name: 'monitoringExplanation.estimationMethod',
    label: '推定・概算方法',
    type: 'textarea',
  },
  {
    name: 'monitoringExplanation.monitoringPointDiagram',
    label: 'モニタリングポイントの図示',
    type: 'file',
  },
];
