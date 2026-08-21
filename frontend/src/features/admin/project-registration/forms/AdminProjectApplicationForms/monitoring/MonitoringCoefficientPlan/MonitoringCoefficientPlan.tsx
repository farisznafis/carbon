// 11.モニタリング計画（２）係数

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { monitoringCoefficientPlanColumns } from '../monitoringPlanRow';

export const monitoringCoefficientPlan: FieldConfig<FormValues>[] = [
  {
    name: 'monitoringCoefficientPlan.items',
    label: '（２）係数（単位発熱量、排出係数、エネルギー消費効率、物性値等）',
    type: 'table',
    columns: monitoringCoefficientPlanColumns,
    minRows: 1,
    addRowLabel: 'モニタリング方法を追加',
  },
];
