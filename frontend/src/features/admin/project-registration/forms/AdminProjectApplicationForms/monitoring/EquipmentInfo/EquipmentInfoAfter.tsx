//設備情報の収集・確認方法_方法論（削減活動実施後）

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

// Same shape as the 実施前 - 更新 page, except メーカー and 型番 are the PCS's.
export const equipmentInfoAfter: FieldConfig<FormValues>[] = [
  { name: 'equipmentInfoAfter.pcsManufacturer', label: 'メーカー', type: 'textarea' },
  { name: 'equipmentInfoAfter.pcsModel', label: '型番', type: 'textarea' },
  {
    name: 'equipmentInfoAfter.equipmentSerialNumber',
    label: '機器製造番号',
    type: 'textarea',
  },
  { name: 'equipmentInfoAfter.output', label: '出力', type: 'textarea' },
  { name: 'equipmentInfoAfter.launchDate', label: '導入日', type: 'date' },
  {
    name: 'equipmentInfoAfter.operationStartDate',
    label: '稼働開始日',
    type: 'date',
  },
  {
    name: 'equipmentInfoAfter.legalUsefulLife',
    label: '法定耐用年数',
    type: 'textarea',
  },
  { name: 'equipmentInfoAfter.efficiency', label: '効率', type: 'textarea' },
  { name: 'equipmentInfoAfter.fuel', label: '燃料', type: 'textarea' },
  { name: 'equipmentInfoAfter.usage', label: '用途', type: 'textarea' },
];
