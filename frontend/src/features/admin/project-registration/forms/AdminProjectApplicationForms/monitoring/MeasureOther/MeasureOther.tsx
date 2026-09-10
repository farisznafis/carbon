// 12.1（１）② 計量器の概要（特定計量器以外の計量器の場合）

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { otherInstrumentColumns } from '../measureInstrumentRow';

export const measureOther: FieldConfig<FormValues>[] = [
  {
    name: 'measureOther.instruments',
    label: '②特定計量器以外の計量器の場合',
    type: 'table',
    columns: otherInstrumentColumns,
    minRows: 1,
    addRowLabel: '計量器を追加',
  },
  {
    name: 'measureOther.monitoringPointDiagram',
    label: 'モニタリングポイントの図示',
    type: 'file',
  },
];
