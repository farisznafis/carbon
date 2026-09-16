// データ管理・特記事項_方法論
import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { IS_SIMILAR, IS_INDEPENDENT_OF_LEGALS, SELECTS_REQUIRED } from '@/lib/constants/select_types';

export const dataManagement: FieldConfig<FormValues>[] = [
  {
    name: 'dataManagement.dataManagementPic',
    label: 'データ管理責任者',
    type: 'text',
  },
  {
    name: 'dataManagement.monitoringPic',
    label: 'モニタリング担当者',
    type: 'text',
  },
  {
    name: 'dataManagement.moniDataAcquiring',
    label: '各削減活動におけるモニタリングデータの取得方法',
    type: 'textarea',
  },
  {
    name: 'dataManagement.moniDataCollection',
    label: '各削減活動のモニタリングデータの収集方法',
    type: 'textarea',
  },
  {
    name: 'dataManagement.moniDataRec',
    label: 'モニタリングデータの記録・保管方法',
    type: 'textarea',
  },
  {
    name: 'dataManagement.dataStoring',
    label: 'データ保存期間（認証対象期間終了後何年間）',
    type: 'number',
  },
  {
    name: 'dataManagement.isRisksForEmission',
    label: '排出量の削減に影響を与える可能性のあるリスク有無',
    type: 'select',
    options: SELECTS_REQUIRED,
  },
  // if isRisksForEmission is 無, hide riskFactors field
  {
    name: 'dataManagement.riskFactors',
    label: '（リスク有の場合）リスク要因',
    type: 'textarea',
    hideWhen: {
      field: 'dataManagement.isRisksForEmission',
      value: false,
    },
  },
  {
    name: 'dataManagement.isSimilar',
    label: '類似制度へプロジェクトを登録有無',
    type: 'select',
    options: IS_SIMILAR,
  },
  // if isSimilar is 登録していない, hide similarSystemName, similarSystemPeriodStart, similarSystemPeriodEnd fields
  {
    name: 'dataManagement.similarSystemName',
    label: '（登録有の場合）類似制度名',
    type: 'text',
    hideWhen: {
      field: 'dataManagement.isSimilar',
      value: '登録していない',
    },
  },
  {
    name: 'dataManagement.similarSystemPeriodStart',
    label: '（登録有の場合）類似制度での認証予定期間（開始）西暦年月日',
    type: 'date',
    hideWhen: {
      field: 'dataManagement.isSimilar',
      value: '登録していない',
    },
  },
  {
    name: 'dataManagement.similarSystemPeriodEnd',
    label: '（登録有の場合）類似制度での認証予定期間（終了）西暦年月日',
    type: 'date',
    hideWhen: {
      field: 'dataManagement.isSimilar',
      value: '登録していない',
    },
  },
  {
    name: 'dataManagement.isIndependentofLegal',
    label: 'プロジェクトの実施は、法令等の義務履行有無',
    type: 'select',
    options: IS_INDEPENDENT_OF_LEGALS,
  },
];
