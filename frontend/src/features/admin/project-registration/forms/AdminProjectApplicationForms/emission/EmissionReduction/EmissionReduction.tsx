//排出削減計画

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { EMISSION_UNITS } from '@/lib/constants/emission_helpers';

export const emissionReduction: FieldConfig<FormValues>[] = [
  {
    name: 'emissionReduction.unitEmission',
    label: '単位排出量（t-CO2e/単位）',
    type: 'select',
    options: EMISSION_UNITS,
  },
  {
    name: 'emissionReduction.certTargetPeriodStart',
    label: '認証対象期間開始日',
    type: 'date',
  },
  {
    name: 'emissionReduction.certTargetPeriodEnd',
    label: '認証対象期間終了日',
    type: 'date',
  },
  {
    name: 'emissionReduction.certTargetPeriodYears',
    label: '認証対象期間（年数）',
    type: 'number',
  },
  { name: 'emissionReduction.year', label: '年度', type: 'text' },
  {
    name: 'emissionReduction.newMembersTotal',
    label: '新規参加会員数（累計）',
    type: 'number',
  },
  {
    name: 'emissionReduction.cumulativeMembersTotal',
    label: '参加会員数（累計）',
    type: 'number',
  },
  {
    name: 'emissionReduction.membersTotal',
    label: '参加会員数（当年度）',
    type: 'number',
  },
  {
    name: 'emissionReduction.baselineEmissions',
    label: 'ベースライン排出量（t-CO2e）',
    type: 'number',
  },
  {
    name: 'emissionReduction.baselineEmissionsUnit',
    label: 'ベースライン排出量 単位',
    type: 'text',
    options: EMISSION_UNITS,
  },
  {
    name: 'emissionReduction.afterImplEmissions',
    label: '導入後排出量（t-CO2e）',
    type: 'number',
  },
  {
    name: 'emissionReduction.afterImplEmissionsUnit',
    label: '導入後排出量 単位',
    type: 'text',
    options: EMISSION_UNITS,
  },
  {
    name: 'emissionReduction.baselineEmissionsTotal',
    label: 'ベースライン排出量合計（t-CO2e）',
    type: 'number',
  },
  {
    name: 'emissionReduction.afterImplEmissionsUnitTotal',
    label: '導入後排出量合計（t-CO2e）',
    type: 'number',
  },
  {
    name: 'emissionReduction.emissionReductionTotal',
    label: '排出削減量合計（t-CO2e）',
    type: 'number',
  },
];
