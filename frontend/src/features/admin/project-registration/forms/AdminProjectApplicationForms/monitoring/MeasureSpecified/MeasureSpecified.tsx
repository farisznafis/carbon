// 12.1（１）① 計量器の概要（特定計量器の場合）

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { specifiedInstrumentColumns } from '../measureInstrumentRow';

export const measureSpecified: FieldConfig<FormValues>[] = [
  {
    name: 'measureSpecified.instruments',
    label: '①特定計量器の場合',
    type: 'table',
    columns: specifiedInstrumentColumns,
    minRows: 1,
    addRowLabel: '計量器を追加',
  },
  {
    name: 'measureSpecified.monitoringPointDiagram',
    label: 'モニタリングポイントの図示',
    type: 'file',
  },
];
