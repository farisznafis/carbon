//追加性の評価_方法論

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const additionalAssessment: FieldConfig<FormValues>[] = [
  {
    name: 'additionalAssessment.assessingAdditionality',
    label: '追加性の評価方法',
    type: 'textarea',
  },
  {
    name: 'additionalAssessment.assessingAdditionalityImg',
    label: '追加性の評価方法',
    type: 'file',
  },
];
