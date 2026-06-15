import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { PREFECTURES } from '@/lib/constants/prefectures';
import { REDUCTION_ACTIVITY_INCLUDED } from '@/lib/constants/select_types';

export const projectOverviewFields: FieldConfig<FormValues>[] = [
  {
    name: 'projectOverview.projectOperatorNameKana',
    label: 'プログラム型運営・管理者（法人名・フリガナ）',
    type: 'text',
  },
  {
    name: 'projectOverview.projectOperatorNameJa',
    label: 'プログラム型運営・管理者（法人名・日本語）',
    type: 'text',
  },
  { name: 'projectOverview.corpNumber', label: '法人番号', type: 'text' },
  {
    name: 'projectOverview.prefecture',
    label: '本社所在地（都道府県）',
    type: 'select',
    options: PREFECTURES,
  },
  {
    name: 'projectOverview.city',
    label: '本社所在地（市区町村）',
    type: 'text',
  },
  {
    name: 'projectOverview.address',
    label: '本社所在地（市区町村以降の住所）',
    type: 'text',
  },
  { name: 'projectOverview.overview', label: '事業者概要', type: 'textarea' },
  {
    name: 'projectOverview.companyName',
    label: '実施者名（本社所在地）',
    type: 'text',
  },
  {
    name: 'projectOverview.reductionActivityNameKana',
    label: '団体、組織、活動等の名称（フリガナ）',
    type: 'text',
  },
  {
    name: 'projectOverview.reductionActivityName',
    label: '団体、組織、活動等の名称',
    type: 'text',
  },
  {
    name: 'projectOverview.reductionActivityIncluded',
    label: '会員のカーボンニュートラル参加の有無実施者削減活動有無',
    type: 'select',
    options: REDUCTION_ACTIVITY_INCLUDED,
  },
  // if reductionActivityIncluded is false, disable
  {
    name: 'projectOverview.cnActParticipationDetails',
    label: 'カーボンニュートラル行動計画への参加状況の確認方法',
    type: 'textarea',
    showWhen: {
      field: 'projectOverview.reductionActivityIncluded',
      value: 'カーボンニュートラル行動計画に参加する削減活動の実施者による削減活動を含まない',
    }
  },
  {
    name: 'projectOverview.holderAccountNameKana',
    label: 'Ｊ－クレジット保有者 口座法人名（フリガナ）',
    type: 'text',
  },
  {
    name: 'projectOverview.holderAccountName',
    label: 'Ｊ－クレジット保有者 口座法人名',
    type: 'text',
  },
  {
    name: 'projectOverview.holderCorpNumber',
    label: 'Ｊ－クレジット保有者 口座法人名',
    type: 'text',
  },
  {
    name: 'projectOverview.holderPrefecture',
    label: 'Ｊ－クレジット保有者 住所／都道府県',
    type: 'select',
    options: PREFECTURES,
  },
  {
    name: 'projectOverview.holderCity',
    label: 'Ｊ－クレジット保有者 住所／市区町村',
    type: 'text',
  },
  {
    name: 'projectOverview.holderAddress',
    label: 'Ｊ－クレジット保有者 住所／市区町村以外の住所',
    type: 'text',
  },
  {
    name: 'projectOverview.isHolderUndecided',
    label: 'Ｊ－クレジット保有予定者決定有無',
    type: 'checkbox',
  },
  {
    name: 'projectOverview.location',
    label: 'プロジェクト実施場所の分類',
    type: 'text',
  },
  {
    name: 'projectOverview.projectLocationClassificationEn',
    label: 'Project Location Classification (EN)',
    type: 'text',
  },
  { name: 'projectOverview.regionalCategory', label: '地域区分', type: 'text' },
  { name: 'projectOverview.targetArea', label: '対象地域', type: 'text' },
  {
    name: 'projectOverview.creditUtilizationPlan',
    label: 'クレジット利用計画',
    type: 'textarea',
  },
  {
    name: 'projectOverview.projectConceptDiagram',
    label: 'プロジェクト概念図',
    type: 'file',
  },
  // 認証対象期間が適用されない方法論を用いたプロジェクト
  // if isMethodologyValid is true, hide certTargetPeriodStart, certTargetPeriodEnd, certTargetPeriodYears fields
  {
    name: 'projectOverview.isMethodologyValid',
    label: '認証対象期間が適用されない方法論を用いたプロジェクト',
    type: 'checkbox',
  },
  {
    name: 'projectOverview.certTargetPeriodStart',
    label: '認証対象期間開始日（西暦年月日）',
    type: 'date',
    hideWhen: {
      field: 'projectOverview.isMethodologyValid',
      value: true,
    },
  },
  {
    name: 'projectOverview.certTargetPeriodEnd',
    label: '認証対象期間終了日（西暦年月日）',
    type: 'date',
    hideWhen: {
      field: 'projectOverview.isMethodologyValid',
      value: true,
    },
  },
  {
    name: 'projectOverview.certTargetPeriodYears',
    label: '認証対象期間（年数）',
    type: 'number',
    hideWhen: {
      field: 'projectOverview.isMethodologyValid',
      value: true,
    },
  },
];
