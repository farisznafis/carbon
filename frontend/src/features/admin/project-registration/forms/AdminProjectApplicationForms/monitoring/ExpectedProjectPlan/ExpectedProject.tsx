//プロジェクト計画での想定

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const expectedProject: FieldConfig<FormValues>[] = [
  { name: 'expectedProject.expectedValue', label: '想定値', type: 'text' },
  { name: 'expectedProject.basis', label: '根拠', type: 'textarea' },
  { name: 'expectedProject.remarks', label: '備考', type: 'textarea' },
];
