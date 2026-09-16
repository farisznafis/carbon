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
  // if 認証権限保有主体変更 selected, all below form is disable
  {
    name: 'projectApplication.applicationCategory',
    label: '申請区分',
    type: 'select',
    options: [
      { label: 'プロジェクト登録', value: '登録' },
      { label: 'プロジェクト計画変更', value: '計画変更' },
      { label: '認証権限保有主体変更', value: '認証権限保有主体変更' },
    ],
  },
  {
    name: 'projectApplication.isUsingCostSupport',
    label: '審査費用支援の活用',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.projectOperatorKana',
    label: 'プログラム型運営・管理者（法人名）（フリガナ）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.projectOperatorJa',
    label: 'プログラム型運営・管理者（法人名）（日本語表記）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.projectOperatorEn',
    label: 'プログラム型運営・管理者（法人名）（英語表記）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.corpNumber',
    label: '法人番号（13桁の数字）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.corpPosition',
    label: '法人代表者の役職',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.corpName',
    label: '法人代表者の氏名',
    type: 'personName',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },

  {
    name: 'projectApplication.prefecture',
    label: '都道府県',
    type: 'select',
    options: PREFECTURES,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },

  {
    name: 'projectApplication.city',
    label: '市区町村',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.address',
    label: '市区町村以降の住所',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.isDesignatedEmitter',
    label: '特定排出者に該当するか',
    type: 'select',
    options: IS_DESIGNATED_EMITTERS,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
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
      value: '認証権限保有主体変更',
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
      value: '認証権限保有主体変更',
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
      value: '認証権限保有主体変更',
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
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.representativePosition',
    label: '代表者の役職',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.isChangeRepresentativePosition',
    label: '代表者の役職に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.representativeName',
    label: '代表者の氏名',
    type: 'personName',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.isChangeRepresentativeName',
    label: '代表者の氏名に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.picPosition',
    label: '申請責任者の役職',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.isChangePicPosition',
    label: '申請責任者の役職に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.picName',
    label: '申請責任者の氏名',
    type: 'personName',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.isChangePicName',
    label: '申請責任者の氏名に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.picEmail',
    label: '申請責任者のメールアドレス',
    type: 'email',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.isChangePicEmail',
    label: '申請責任者のメールアドレスに変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.picPhone',
    label: '申請責任者の電話番号',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.isChangePicPhoneNumber',
    label: '申請責任者の電話番号に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.otherContactBusinessName',
    label: '（その他連絡先）事業者名',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.isChangeOtherContactBusinessName',
    label: '（その他連絡先）事業者名に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.otherContactPicPosition',
    label: '（その他連絡先）責任者役職',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.isChangeOtherContactPicPosition',
    label: '（その他連絡先）責任者役職に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.otherContactsPicName',
    label: '（その他連絡先）責任者氏名',
    type: 'personName',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.isChangeOtherContactsPicName',
    label: '（その他連絡先）責任者氏名に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.otherContactsPicEmail',
    label: '（その他連絡先）責任者メールアドレス',
    type: 'email',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.isChangeOtherContactsPicEmail',
    label: '（その他連絡先）責任者メールアドレスに変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.otherContactsPicPhoneNumber',
    label: '（その他連絡先）責任者電話番号',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.isChangeOtherContactsPicPhoneNumber',
    label: '（その他連絡先）責任者電話番号に変更があるか',
    type: 'select',
    options: SELECTS_REQUIRED,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.projectNumber',
    label: 'プロジェクト番号',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.projectName',
    label: 'プロジェクトの名称',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.methodologyCategory',
    label: '適用する方法論の区分',
    type: 'select',
    options: METHODS,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.methodologyNumber',
    label: '適用方法論番号',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.methodology',
    label: '適用方法論',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
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
      value: '認証権限保有主体変更',
    },
  },
  // not sure
  {
    name: 'projectApplication.SaaS',
    label: 'プロジェクト実施場所の分類（英語表記）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  // not sure
  {
    name: 'projectApplication.projectOutline',
    label: 'プロジェクト概要（英語表記）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.regionCategory',
    label: '地域区分',
    type: 'select',
    options: REGIONALS,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.groupName',
    label: '取りまとめる削減活動（社員）全体を指す団体、組織、活動等の名称',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.groupNameFurigana',
    label:
      '取りまとめる削減活動（社員）全体を指す団体、組織、活動等の名称（フリガナ）',
    type: 'text',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.isParticipatingCnAct',
    label: '社員のカーボンニュートラル行動計画参加有無',
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
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.validationCheckReviewerName',
    label: '妥当性確認審査機関名',
    type: 'select',
    options: REVIEWER_NAMES,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.applicationDate',
    label: '妥当性確認申請日（西暦年月日）',
    type: 'date',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.registrationDate',
    label: '登録申請日（西暦年月日）',
    type: 'date',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.reviewerName',
    label: '審査機関名',
    type: 'select',
    options: REVIEWER_NAMES,
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  {
    name: 'projectApplication.applicationChangeDate',
    label: '計画変更日（西暦年月日）',
    type: 'date',
    hideWhen: {
      field: 'projectApplication.applicationCategory',
      value: '認証権限保有主体変更',
    },
  },
  //
  // {
  //   name: "projectApplication.projectNumber",
  //   label: "（記載内容の確認）プロジェクト番号",
  //   type: "date",
  // },
];
