//設備情報の収集・確認方法_方法論（実施前 - 新設）

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

// A new installation has no existing equipment to list, so this page carries
// only the standard-equipment statement.
export const equipmentInfoBeforeNew: FieldConfig<FormValues>[] = [
  {
    name: 'equipmentInfoBeforeNew.standardEquipmentSetting',
    label: '方法論の要求事項に沿って標準的な設備の設定の記載',
    type: 'textarea',
  },
];
