import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { PREFECTURES } from '@/lib/constants/prefectures';
import { REGIONALS } from '@/lib/constants/regionals';
import { METHODS } from '@/lib/constants/methodology';
import { REVIEWER_NAMES } from '@/lib/constants/reviewer_names';
import {
  IS_DESIGNATED_EMITTERS,
  SELECTS_REQUIRED,
} from '@/lib/constants/select_types';

export const projectApplication: FieldConfig<FormValues>[] = [
  // if 認証権限保有主体更新 selected, all below form is disable
  {
    name: 'projectApplication.applicationCategory',
    label: '申請区分',
    type: 'select',
    options: [
      { label: 'プロジェクト登録', value: '登録' },
      { label: 'プロジェクト計画変更', value: '計画更新' },
      { label: '認証権限保有主体変更', value: '認証権限保有主体更新' },
    ],
  },
  {
    name: 'projectApplication.isUsingCostSupport',
    label: '審査費用支援の活用',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.projectOperatorKana',
    label: 'プログラム型運営・管理者（法人名）（フリガナ）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.projectOperatorJa',
    label: 'プログラム型運営・管理者（法人名）（日本語表記）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.projectOperatorEn',
    label: 'プログラム型運営・管理者（法人名）（英語表記）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.corpNumber',
    label: '法人番号（13桁の数字）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.corpPosition',
    label: '法人代表者の役職',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.corpName',
    label: '法人代表者の氏名',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },

  {
    name: 'projectApplication.prefecture',
    label: '都道府県',
    type: 'select',
    options: PREFECTURES,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },

  {
    name: 'projectApplication.city',
    label: '市区町村',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.address',
    label: '市区町村以降の住所',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.isDesignatedEmitter',
    label: '特定排出者に該当するか',
    type: 'select',
    options: IS_DESIGNATED_EMITTERS,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  // if isdesignatedemitter false, disable
  {
    name: 'projectApplication.emitterCode',
    label: '特定排出者コード',
    type: 'text',
    showWhen: {
      field: 'projectApplication.isDesignatedEmitter',
      value: true,
    },

    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.hasEnergyActPeriodReport',
    label: 'エネルギ ーの使用の合理化等に関する法律に基づく定期報告有無',
    type: 'select',
    options: [
      { label: '対象である', value: true },
      { label: '対象でない', value: false },
    ],
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  // if hasEnergyActPeriodReport false, disable
  {
    name: 'projectApplication.energyActOpeNumber',
    label: '（対象の場合）省エネ法特定事業者番号又は特定連鎖化事業者番号',
    type: 'text',
    showWhen: {
      field: 'projectApplication.hasEnergyActPeriodReport',
      value: true,
    },
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  // if hasEnergyActPeriodReport false, disable
  {
    name: 'projectApplication.energyManageFactoryNumber',
    label: '（対象の場合）エネルギー管理指定工場番号',
    type: 'text',
    showWhen: {
      field: 'projectApplication.hasEnergyActPeriodReport',
      value: true,
    },
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.representativePosition',
    label: '代表者の役職',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.isChangeRepresentativePosition',
    label: '代表者の役職に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.representativeName',
    label: '代表者の氏名',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.isChangeRepresentativeName',
    label: '代表者の氏名に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.picPosition',
    label: '申請担当者の役職',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.isChangePicPosition',
    label: '申請担当者の役職に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.picName',
    label: '申請担当者の氏名',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.isChangePicName',
    label: '申請担当者の氏名に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.picEmail',
    label: '申請担当者のメールアドレス',
    type: 'email',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.isChangePicEmail',
    label: '申請担当者のメールアドレスに変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.picPhone',
    label: '申請担当者の電話番号',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.isChangePicPhone',
    label: '申請担当者の電話番号に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.otherContactBusinessName',
    label: '（その他連絡先）事業者名',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.isChangeOtherContactBusinessName',
    label: '（その他連絡先）事業者名に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.otherContactPicPosition',
    label: '（その他連絡先）担当者役職',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.isChangeOtherContactPicPosition',
    label: '（その他連絡先）担当者役職に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.otherContactPicName',
    label: '（その他連絡先）担当者氏名',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.isChangeOtherContactPicName',
    label: '（その他連絡先）担当者氏名に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.otherContactPicEmail',
    label: '（その他連絡先）担当者メールアドレス',
    type: 'email',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.isChangeOtherContactPicEmail',
    label: '（その他連絡先）担当者メールアドレスに変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.otherContactPicPhone',
    label: '（その他連絡先）担当者電話番号',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.isChangeOtherContactPicPhone',
    label: '（その他連絡先）担当者電話番号に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.projectNumber',
    label: 'プロジェクト番号',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.projectName',
    label: 'プロジェクトの名称',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.methodologyCategory',
    label: '適用する方法論の区分',
    type: 'select',
    options: METHODS,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.methodologyNumber',
    label: '適用方法論番号',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.methodology',
    label: '適用方法論',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.location',
    label: 'プロジェクト実施場所の分類',
    type: 'select',
    options: [
      { label: '家庭', value: '家庭' },
      { label: '事業所', value: '事業所' },
      { label: '家庭および事業所', value: '家庭および事業所' },
      { label: '水田', value: '水田' },
      { label: '農家', value: '農家' },
      { label: '公共施設', value: '公共施設' },
      { label: '建築物', value: '建築物' },
    ],
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  // not sure
  {
    name: 'projectApplication.SaaS',
    label: 'プロジェクト実施場所の分類（英語表記）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  // not sure
  {
    name: 'projectApplication.projectOutline',
    label: 'プロジェクト概要（英語表記）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.regionCategory',
    label: '地域区分',
    type: 'select',
    options: REGIONALS,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.groupName',
    label: '取りまとめる削減活動（会員）全体を指す団体、組織、活動等の名称',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.groupNameFurigana',
    label:
      '取りまとめる削減活動（会員）全体を指す団体、組織、活動等の名称（フリガナ）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.isParticipatingCnAct',
    label: '会員のカーボンニュートラル行動計画参加有無',
    type: 'select',
    options: [
      {
        label:
          'カーボンニュートラル行動計画に参加する削減活動の実施者による削減活動を含む',
        value: true,
      },
      {
        label:
          'カーボンニュートラル行動計画に参加する削減活動の実施者による削減活動を含まない',
        value: false,
      },
    ],
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.validationCheckReviewerName',
    label: '妥当性確認審査機関名',
    type: 'select',
    options: REVIEWER_NAMES,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.applicationDate',
    label: '妥当性確認申請日（西暦年月日）',
    type: 'date',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.registrationDate',
    label: '登録申請日（西暦年月日）',
    type: 'date',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.reviewerName',
    label: '審査機関名',
    type: 'select',
    options: REVIEWER_NAMES,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  {
    name: 'projectApplication.applicationChangeDate',
    label: '計画変更日（西暦年月日）',
    type: 'date',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体更新',
    },
  },
  //
  // {
  //   name: "projectApplication.projectNumber",
  //   label: "（記載内容の確認）プロジェクト番号",
  //   type: "date",
  // },
];
