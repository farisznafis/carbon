import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const applicableConditions: FieldConfig<FormValues>[] = [
  {
    name: 'appConditions.methodologyCode',
    label: '方法論コード',
    type: 'text',
  },
  {
    name: 'appConditions.methodologyVersion',
    label: '方法論バージョン',
    type: 'text',
  },
  {
    name: 'appConditions.esgPledgeCase',
    label: 'ESG誓約書類番号',
    type: 'text',
  },
  {
    name: 'appConditions.esgPledgeCaseDate',
    label: 'ESG誓約書提出日',
    type: 'date',
  },
  { name: 'appConditions.businessName', label: '事業者名', type: 'text' },
  {
    name: 'appConditions.representativePosition',
    label: '代表者役職',
    type: 'text',
  },
  {
    name: 'appConditions.representativeName',
    label: '代表者氏名',
    type: 'text',
  },
  { name: 'appConditions.regulation', label: '適用規則', type: 'text' },
];
