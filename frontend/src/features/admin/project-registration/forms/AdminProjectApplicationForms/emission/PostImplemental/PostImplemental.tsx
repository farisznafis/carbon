// 10.2（２）プロジェクト実施後の付随的な排出活動

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { postImplementActivityColumns } from '../sectionTenRows';

export const postImplemental: FieldConfig<FormValues>[] = [
  { name: 'postImplemental.concept', label: '考え方', type: 'textarea' },
  {
    name: 'postImplemental.activities',
    label: '付随的な排出活動',
    type: 'table',
    columns: postImplementActivityColumns,
    minRows: 1,
    addRowLabel: '排出活動を追加',
  },
  // Both 合計 columns are summed from the rows on every save.
  {
    name: 'postImplemental.emissionsTotalValue',
    label: '排出量合計値 (t-CO2/年)',
    type: 'number',
    required: false,
    disabled: true,
  },
  {
    name: 'postImplemental.impactTotalValue',
    label: '影響度合計値 (％)',
    type: 'number',
    required: false,
    disabled: true,
  },
];
