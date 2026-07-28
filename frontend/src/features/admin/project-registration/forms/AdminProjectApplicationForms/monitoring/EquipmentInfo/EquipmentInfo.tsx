//設備情報の収集・確認方法_方法論（実施前 - 更新）

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const equipmentInfo: FieldConfig<FormValues>[] = [
  { name: 'equipmentInfo.manufacturer', label: 'メーカー', type: 'textarea' },
  { name: 'equipmentInfo.modelNumber', label: '型番', type: 'textarea' },
  {
    name: 'equipmentInfo.equipmentSerialNumber',
    label: '機器製造番号',
    type: 'textarea',
  },
  { name: 'equipmentInfo.output', label: '出力', type: 'textarea' },
  { name: 'equipmentInfo.launchDate', label: '導入日', type: 'date' },
  {
    name: 'equipmentInfo.operationStartDate',
    label: '稼働開始日',
    type: 'date',
  },
  {
    name: 'equipmentInfo.legalUsefulLife',
    label: '法定耐用年数',
    type: 'textarea',
  },
  { name: 'equipmentInfo.efficiency', label: '効率', type: 'textarea' },
  { name: 'equipmentInfo.fuel', label: '燃料', type: 'textarea' },
  { name: 'equipmentInfo.usage', label: '用途', type: 'textarea' },
];
