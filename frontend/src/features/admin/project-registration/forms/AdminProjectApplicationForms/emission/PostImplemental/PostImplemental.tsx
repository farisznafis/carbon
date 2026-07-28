//プロジェクト実施後の付随的な排出活動

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { MONITOR_CALC_POSTS } from '@/lib/constants/select_types';

export const postImplemental: FieldConfig<FormValues>[] = [
  { name: 'postImplemental.concept', label: '考え方', type: 'textarea' },
  { name: 'postImplemental.emissionAct', label: '排出活動', type: 'textarea' },
  {
    name: 'postImplemental.greenhouseGasType',
    label: '温室効果ガスの種類',
    type: 'text',
  },
  {
    name: 'postImplemental.emissions',
    label: '排出量(t-CO2/年)',
    type: 'text',
  },
  { name: 'postImplemental.impact', label: '影響度(％)', type: 'text' },
  {
    name: 'postImplemental.monitorCalc',
    label: 'モニタリング・算定方法 - 排出量の算定実施',
    type: 'select',
    options: MONITOR_CALC_POSTS,
  },
  {
    name: 'postImplemental.emissionsTotal',
    label: '排出量合計値',
    type: 'text',
  },
  { name: 'postImplemental.impactTotal', label: '影響度合計値', type: 'text' },
];
