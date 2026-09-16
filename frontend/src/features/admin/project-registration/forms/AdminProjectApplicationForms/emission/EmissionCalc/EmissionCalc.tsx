// 排出削減量の算定方法_方法論

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { EMISSION_UNITS } from '@/lib/constants/emission_helpers';

export const emissionCalc: FieldConfig<FormValues>[] = [
  { name: 'emissionCalc.emissionReduction', label: '排出削減量', type: 'text' },
  {
    name: 'emissionCalc.emissionReductionUnit',
    label: '排出削減量単位',
    type: 'select',
    options: EMISSION_UNITS,
  },
  {
    name: 'emissionCalc.baselineEmission',
    label: 'ベースライン排出量',
    type: 'text',
  },
  {
    name: 'emissionCalc.baselineEmissionUnit',
    label: 'ベースライン排出量単位',
    type: 'select',
    options: EMISSION_UNITS,
  },
  {
    name: 'emissionCalc.postProjectEmission',
    label: 'プロジェクト実施後排出量',
    type: 'text',
  },
  {
    name: 'emissionCalc.postProjectEmissionUnit',
    label: 'プロジェクト実施後排出量単位',
    type: 'select',
    options: EMISSION_UNITS,
  },
];
