// //モニタリング・算定方法

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import {
  IS_STANDARD_MONITORS,
  IS_USE_SAMPLING,
} from '@/lib/constants/select_types';

export const monitoringCalc: FieldConfig<FormValues>[] = [
  {
    name: 'monitoringCalc.isStandardizeEmissionAct',
    label: '全削減活動においてモニタリング・算定を実施する排出活動を統一可否',
    type: 'checkbox',
  },
  {
    name: 'monitoringCalc.actMonitoringItems',
    label: '活動量のモニタリング項目',
    type: 'text',
  },
  {
    name: 'monitoringCalc.isStandardMonitor',
    label: '全削減活動においてモニタリング・算定を実施する排出活動の確認',
    type: 'select',
    options: IS_STANDARD_MONITORS,
  },
  // if isStandardMonitor is その他（以下に確認方法をご記入ください。）, show otherMethod field
  {
    name: 'monitoringCalc.otherMethod',
    label: 'その他（モニタリング・算定を実施する排出活動の確認方法）',
    type: 'textarea',
    showWhen: {
      field: 'monitoringCalc.isStandardMonitor',
      value: 'その他（以下に確認方法をご記入ください。）',
    },
  },
  {
    name: 'monitoringCalc.isUseSampling',
    label: 'サンプリングの利用有無',
    type: 'select',
    options: IS_USE_SAMPLING,
  },
  // if isUseSampling is サンプリングを利用しない, hide samplingMethod and below field
  {
    name: 'monitoringCalc.samplingMethod',
    label: '適用するサンプル抽出法',
    type: 'textarea',
    hideWhen: {
      field: 'monitoringCalc.isUseSampling',
      value: 'サンプリングを利用しない',
    },
  },
  {
    name: 'monitoringCalc.samplingPurpose',
    label: 'サンプリングの目的',
    type: 'textarea',
    hideWhen: {
      field: 'monitoringCalc.isUseSampling',
      value: 'サンプリングを利用しない',
    },
  },
  {
    name: 'monitoringCalc.collectDataAndMeasurement',
    label: '収集データ及び実測対象',
    type: 'textarea',
    hideWhen: {
      field: 'monitoringCalc.isUseSampling',
      value: 'サンプリングを利用しない',
    },
  },
  {
    name: 'monitoringCalc.populationComposition',
    label: '母集団の構成・リスト及びその特性',
    type: 'textarea',
    hideWhen: {
      field: 'monitoringCalc.isUseSampling',
      value: 'サンプリングを利用しない',
    },
  },
  {
    name: 'monitoringCalc.explOfRationality',
    label:
      '適用したサンプル抽出法と当該抽出法の利用が妥当であることの合理的な説明',
    type: 'textarea',
    hideWhen: {
      field: 'monitoringCalc.isUseSampling',
      value: 'サンプリングを利用しない',
    },
  },
  {
    name: 'monitoringCalc.sampleSizeAndFormula',
    label: 'サンプル数（評価式を含む）',
    type: 'textarea',
    hideWhen: {
      field: 'monitoringCalc.isUseSampling',
      value: 'サンプリングを利用しない',
    },
  },
  {
    name: 'monitoringCalc.sampleSizeAndFormulaImg',
    label: 'サンプル数（画像データ）',
    type: 'file',
    hideWhen: {
      field: 'monitoringCalc.isUseSampling',
      value: 'サンプリングを利用しない',
    },
  },
  {
    name: 'monitoringCalc.dataAnalysis',
    label: 'データ収集・管理・分析方法及び非標本誤差への対応方法',
    type: 'textarea',
    hideWhen: {
      field: 'monitoringCalc.isUseSampling',
      value: 'サンプリングを利用しない',
    },
  },
  {
    name: 'monitoringCalc.implMethodSched',
    label: '実施方法（スケジュールと対応者等）',
    type: 'textarea',
    hideWhen: {
      field: 'monitoringCalc.isUseSampling',
      value: 'サンプリングを利用しない',
    },
  },
  {
    name: 'monitoringCalc.implMethodSchedImg',
    label: '実施方法（画像データ）',
    type: 'file',
    hideWhen: {
      field: 'monitoringCalc.isUseSampling',
      value: 'サンプリングを利用しない',
    },
  },
];
