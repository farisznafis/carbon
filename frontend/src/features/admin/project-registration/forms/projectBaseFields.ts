import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const projectFields: FieldConfig<FormValues>[] = [
  { name: 'projectBase.projectName', label: 'プロジェクト名称', type: 'text' },

  {
    name: 'projectBase.projectStatus',
    label: 'ステータス',
    type: 'select',
    options: [
      { label: '作成済み', value: '作成済み' },
      { label: '準備中', value: '準備中' },
      { label: '変更中', value: '変更中' },
      { label: '妥当性確認処理', value: '妥当性確認処理' },
      { label: '却下', value: '却下' },
      { label: '実施中', value: '実施中' },
      { label: '検証中', value: '検証中' },
      { label: 'クレジット申請中', value: 'クレジット申請中' },
      { label: '終了', value: '終了' },
    ],
  },
  {
    name: 'projectBase.regionalCategory',
    label: '地域区分',
    type: 'select',
    options: [
      { label: '北海道', value: '北海道' },
      { label: '東北', value: '東北' },
      { label: '関東', value: '関東' },
      { label: '中部', value: '中部' },
      { label: '近畿', value: '近畿' },
      { label: '中国', value: '中国' },
      { label: '四国', value: '四国' },
      { label: '九州・沖縄', value: '九州・沖縄' },
      { label: '全国', value: '全国' },
    ],
  },

  {
    name: 'projectBase.projectNumber',
    label: 'プロジェクト番号',
    type: 'text',
  },
  { name: 'projectBase.companyName', label: '会社名', type: 'text' },
];

export const representativeFields: FieldConfig<FormValues>[] = [
  { name: 'projectBase.repLastName', label: '代表者 姓', type: 'text' },
  { name: 'projectBase.repFirstName', label: '代表者 名', type: 'text' },
  {
    name: 'projectBase.repLastNameKana',
    label: '代表者 姓（カナ）',
    type: 'text',
  },
  {
    name: 'projectBase.repFirstNameKana',
    label: '代表者 名（カナ）',
    type: 'text',
  },
  { name: 'projectBase.repEmail', label: '代表者 メール', type: 'email' },
  {
    name: 'projectBase.repPhoneNumber',
    label: '代表者 電話番号',
    type: 'text',
  },
  { name: 'projectBase.repPosition', label: '代表者 役職', type: 'text' },
];

export const picFields: FieldConfig<FormValues>[] = [
  { name: 'projectBase.picFirstName', label: '責任者名', type: 'text' },
  { name: 'projectBase.picLastName', label: '責任者姓', type: 'text' },
  {
    name: 'projectBase.picFirstNameKana',
    label: '責任者名（フリガナ）',
    type: 'text',
  },
  {
    name: 'projectBase.picLastNameKana',
    label: '責任者姓（フリガナ）',
    type: 'text',
  },
  {
    name: 'projectBase.picEmail',
    label: '責任者メールアドレス',
    type: 'email',
  },
  {
    name: 'projectBase.picPhoneNumber',
    label: '責任者電話番号',
    type: 'text',
  },
  { name: 'projectBase.picPosition', label: '担当役職', type: 'text' },
  {
    name: 'projectBase.picDataManagement',
    label: 'データ管理責任者',
    type: 'text',
  },
];

export const membersFields: FieldConfig<FormValues>[] = [
  {
    name: 'projectBase.newMembersTotal',
    label: '新規会員数（件）',
    type: 'number',
  },
  {
    name: 'projectBase.withdrawnMembers',
    label: '退会会員数（件）',
    type: 'number',
  },
  {
    name: 'projectBase.cumulativeMembers',
    label: '認証対象となる累計会員数（件）',
    type: 'number',
  },
  {
    name: 'projectBase.membersTotal',
    label: '延べ会員数（件）',
    type: 'number',
  },
];

export const methodologyFields: FieldConfig<FormValues>[] = [
  {
    name: 'projectBase.methodologyVersion',
    label: '方法論バージョン',
    type: 'text',
  },
  {
    name: 'projectBase.methodCategory',
    label: '方法論区分',
    type: 'select',
    options: [
      { label: 'EN-S', value: 'EN-S' },
      { label: 'EN-R', value: 'EN-R' },
      { label: 'IN', value: 'IN' },
      { label: 'AG', value: 'AG' },
      { label: 'WA', value: 'WA' },
    ],
  },
  {
    name: 'projectBase.methodNumber',
    label: '方法論番号',
    type: 'text',
  },
  {
    name: 'projectBase.methodName',
    label: '方法論名称',
    type: 'text',
  },
  {
    name: 'projectBase.methodMainEmission',
    label: '方法論排出源／活動',
    type: 'text',
  },
  {
    name: 'projectBase.methodCombiFeasibility',
    label: '方法論組み合わせ可否',
    type: 'select',
    options: [
      { label: '有', value: true },
      { label: '無', value: false },
    ],
  },
  {
    name: 'projectBase.methodDetails',
    label: '方法論説明',
    type: 'textarea',
  },
];

export const emissionFields: FieldConfig<FormValues>[] = [
  {
    name: 'projectBase.emissionReduction',
    label: '削減量',
    type: 'number',
  },
  {
    name: 'projectBase.baselineEmissions',
    label: 'ベースライン排出量',
    type: 'number',
  },
  {
    name: 'projectBase.postProjectEmission',
    label: 'プロジェクト後排出量',
    type: 'number',
  },
];

export const operatorInfo: FieldConfig<FormValues>[] = [
  {
    name: 'projectBase.operatorCompanyId',
    label: '運営会社ID',
    type: 'text',
  },
];

export const allProjectFields: FieldConfig<FormValues>[] = [
  ...projectFields,
  ...representativeFields,
  ...picFields,
  ...membersFields,
  ...emissionFields,
  ...methodologyFields,
  ...operatorInfo,
];
