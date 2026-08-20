//排出削減計画

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { ABSOLUTE_EMISSION_UNITS } from '@/lib/constants/emission_helpers';

/**
 * 4.1 の年度表。1 行 = 1 年度、2013–2050 の 38 行。
 *
 * Names are row-relative; `PlanTableField` applies the index. The server owns
 * 年度 and 認証申請期間日数 and reports them read-only, so they arrive locked
 * without being declared so here.
 *
 * 年度開始日 / 年度終了日 / 年度内日数 are deliberately not shown. The sheet
 * carries them (columns V–X) because Excel needs them for its own formulas, but
 * they are the same calendar for every project in every year — three more
 * columns of noise across 38 rows that nobody reads and nobody can change. They
 * are still stored and still returned by the API.
 */
const emissionReductionYearColumns: FieldConfig['columns'] = [
  { name: 'year', label: '年度', type: 'number', required: false, width: '5rem' },
  {
    name: 'newMembersTotal',
    label: '新規会員数（件）',
    type: 'number',
    width: '8rem',
  },
  {
    name: 'cumulativeMembersTotal',
    label: '認証対象となる累計会員数（件）',
    type: 'number',
    width: '10rem',
  },
  {
    name: 'membersTotal',
    label: '延べ会員数（件）',
    type: 'number',
    required: false,
    width: '8rem',
  },
  {
    name: 'baselineEmissions',
    label: 'ベースライン排出量',
    type: 'number',
    required: false,
    width: '10rem',
  },
  {
    name: 'afterImplEmissions',
    label: 'プロジェクト実施後排出量',
    type: 'number',
    required: false,
    width: '10rem',
  },
  {
    name: 'certApplicationDays',
    label: '年度内の認証申請期間日数',
    type: 'number',
    required: false,
    width: '8rem',
  },
];

export const emissionReduction: FieldConfig<FormValues>[] = [
  {
    name: 'emissionReduction.unitEmission',
    label: '個々の削減活動の排出量の単位',
    type: 'select',
    options: ABSOLUTE_EMISSION_UNITS,
  },
  {
    name: 'emissionReduction.certTargetPeriodStart',
    label: '認証対象期間（開始）',
    type: 'date',
    required: false,
  },
  {
    name: 'emissionReduction.certTargetPeriodEnd',
    label: '認証対象期間（終了）',
    type: 'date',
    required: false,
  },
  {
    name: 'emissionReduction.certTargetPeriodYears',
    label: '認証対象期間（年）',
    type: 'number',
    required: false,
  },
  {
    name: 'emissionReduction.years',
    label: '4.1 プログラム型プロジェクトの全体計画',
    type: 'table',
    // The 38 年度 are the scheme's, not this project's. Adding or removing one
    // would be discarded by the server on the next read.
    fixedRows: true,
    columns: emissionReductionYearColumns,
  },
  // The 合計 row under the table. Computed from the rows on every save, so
  // these are shown for confirmation and never typed.
  {
    name: 'emissionReduction.baselineEmissionsTotal',
    label: '合計／ベースライン排出量（t-CO2）',
    type: 'number',
    required: false,
    disabled: true,
  },
  {
    name: 'emissionReduction.afterImplEmissionsUnitTotal',
    label: '合計／プロジェクト実施後排出量（t-CO2）',
    type: 'number',
    required: false,
    disabled: true,
  },
  {
    name: 'emissionReduction.emissionReductionTotal',
    label: '排出削減量の総量（t-CO2）',
    type: 'number',
    required: false,
    disabled: true,
  },
  {
    name: 'emissionReduction.emissionReductionAnnualAverage',
    label: '排出削減量の年平均（t-CO2）',
    type: 'number',
    required: false,
    disabled: true,
  },
];
