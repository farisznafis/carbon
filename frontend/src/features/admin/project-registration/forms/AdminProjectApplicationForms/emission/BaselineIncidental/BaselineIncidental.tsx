// 10.2（１）ベースラインの付随的な排出活動

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { baselineIncidentalActivityColumns } from '../sectionTenRows';

export const baselineIncidental: FieldConfig<FormValues>[] = [
  { name: 'baselineIncidental.concept', label: '考え方', type: 'textarea' },
  {
    name: 'baselineIncidental.activities',
    label: '付随的な排出活動',
    type: 'table',
    columns: baselineIncidentalActivityColumns,
    minRows: 1,
    addRowLabel: '排出活動を追加',
  },
  // The 合計 under the table. Summed from the rows on every save, never typed —
  // which is also why the sheet's own ※1 warning about the total falling out of
  // step with added rows does not apply here.
  {
    name: 'baselineIncidental.emissionsTotalValue',
    label: '排出量合計値 (t-CO2/年)',
    type: 'number',
    required: false,
    disabled: true,
  },
];
