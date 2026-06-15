//追加性の評価_方法論

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const obtainingAdditional: FieldConfig<FormValues>[] = [
  { name: 'obtainingAdditional.item', label: '項目', type: 'text' },
  {
    name: 'obtainingAdditional.acquisitionMethod',
    label: '入手方法',
    type: 'text',
  },
];
