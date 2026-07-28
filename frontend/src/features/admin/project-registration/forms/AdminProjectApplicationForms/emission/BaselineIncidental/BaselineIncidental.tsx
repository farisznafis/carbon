// // /ベースラインの付随的な排出活動
//  concept: z.string(),
//   プロジェクト実施後排出量単位

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { MONITOR_CALC_BASELINES } from '@/lib/constants/select_types';

export const baselineIncidental: FieldConfig<FormValues>[] = [
  { name: 'baselineIncidental.concept', label: '考え方', type: 'textarea' },
  { name: 'baselineIncidental.emissionAct', label: '排出活動', type: 'textarea' },
  {
    name: 'baselineIncidental.greenhouseGasType',
    label: '温室効果ガスの種類',
    type: 'text',
  },
  {
    name: 'baselineIncidental.emissions',
    label: '排出量(t-CO2/年)',
    type: 'text',
  },
  {
    name: 'baselineIncidental.monitorCalcBeingDone',
    label: '排出量の算定実施有無',
    type: 'select',
    options: MONITOR_CALC_BASELINES,
  },
  {
    name: 'baselineIncidental.emissionsTotalValue',
    label: '排出量合計値',
    type: 'text',
  },
];
