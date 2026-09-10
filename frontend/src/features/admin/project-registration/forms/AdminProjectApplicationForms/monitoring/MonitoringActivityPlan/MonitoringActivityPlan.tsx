// 11.モニタリング計画（１）活動量

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { monitoringActivityPlanColumns } from '../monitoringPlanRow';

export const monitoringActivityPlan: FieldConfig<FormValues>[] = [
  {
    name: 'monitoringActivityPlan.items',
    label: '（１）活動量（燃料消費量、生成熱量、生産量等）',
    type: 'table',
    columns: monitoringActivityPlanColumns,
    minRows: 1,
    addRowLabel: 'モニタリング方法を追加',
  },
];
