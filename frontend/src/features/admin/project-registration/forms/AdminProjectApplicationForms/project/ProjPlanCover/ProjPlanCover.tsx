import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const projPlanCover: FieldConfig<FormValues>[] = [
  { name: 'projPlanCover.version', label: 'バージョン', type: 'text' },
  { name: 'projPlanCover.projectName', label: 'プロジェクト名', type: 'text' },
  {
    name: 'projPlanCover.projectNumber',
    label: 'プロジェクト番号',
    type: 'text',
  },
  {
    name: 'projPlanCover.projectOperatorName',
    label: 'プログラム型運営・管理者（法人名）',
    type: 'text',
  },
  {
    name: 'projPlanCover.applicationDate',
    label: '申請日（西暦年月日）',
    type: 'date',
  },
  {
    name: 'projPlanCover.projectRegistrationDate',
    label: 'プロジェクト登録日（西暦年月日）',
    type: 'date',
  },
];
