import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import {
  IS_DESIGNATED_EMITTERS,
  SELECTS_REQUIRED,
} from '@/lib/constants/select_types';

// parent name; change here
const PARENT_NAME = 'otherProjectOperator';

// field name builder
// const field = (name: string) => `${PARENT_NAME}.${name}` as const;
const field = <K extends string>(name: K): `${typeof PARENT_NAME}.${K}` =>
  `${PARENT_NAME}.${name}`;

export const otherProjectOperator: FieldConfig<FormValues>[] = [
  {
    name: field('projectOperatorNameJa'),
    label: 'プログラム型運営・管理者（法人名・日本語）',
    type: 'text',
  },
  {
    // name: "otherProjectOperator.projectOperatorNameKana",
    name: field('projectOperatorNameKana'),
    label: 'プログラム型運営・管理者（法人名・フリガナ）',
    type: 'text',
  },
  { name: field('otherCorpNumber'), label: '法人番号', type: 'text' },
  {
    name: field('prefecture'),
    label: '本社所在地（都道府県）',
    type: 'text',
  },
  { name: field('city'), label: '本社所在地（市区町村）', type: 'text' },
  {
    name: field('address'),
    label: '本社所在地（市区町村以降の住所）',
    type: 'text',
  },

  {
    name: field('representativePosition'),
    label: '代表者役職',
    type: 'text',
  },

  {
    name: field('isChangeRepresentativePosition'),
    label: '代表者役職 変更有無',
    type: 'select',
    options: SELECTS_REQUIRED,
  },

  {
    name: field('representativeName'),
    label: '代表者氏名',
    type: 'text',
  },

  {
    name: field('isChangeRepresentativeName'),
    label: '代表者氏名 変更有無',
    type: 'select',
    options: SELECTS_REQUIRED,
  },
  {
    name: field('picPosition'),
    label: '担当者部署・役職',
    type: 'text',
  },

  {
    name: field('isChangePicPosition'),
    label: '担当者部署・役職 変更有無',
    type: 'select',
    options: SELECTS_REQUIRED,
  },

  { name: field('picName'), label: '担当者氏名', type: 'text' },

  {
    name: field('isChangePicName'),
    label: '担当者氏名 変更有無',
    type: 'select',
    options: SELECTS_REQUIRED,
  },
  { name: field('picEmail'), label: '担当者E-mail', type: 'text' },

  {
    name: field('isChangePicEmail'),
    label: '担当者E-mail 変更有無',
    type: 'select',
    options: SELECTS_REQUIRED,
  },

  {
    name: field('picPhoneNumber'),
    label: '担当者電話番号',
    type: 'text',
  },

  {
    name: field('isChangePicPhoneNumber'),
    label: '担当者電話番号 変更有無',
    type: 'select',
    options: SELECTS_REQUIRED,
  },
  //change disable
  {
    name: field('isDesignatedEmitter'),
    label: '特定排出者である',
    type: 'select',
    options: IS_DESIGNATED_EMITTERS,
  },

  {
    name: field('emitterCode'),
    label: '特定排出者コード',
    type: 'text',
    showWhen: {
      field: 'otherProjectOperator.isDesignatedEmitter',
      value: true,
    },
  },

  {
    name: field('hasEnergyActReport'),
    label: '省エネ法 定期報告の有無',
    type: 'select',
    options: SELECTS_REQUIRED,
  },

  {
    name: field('energyActOpeNumber'),
    label: '省エネ法特定事業者番号／特定連鎖化事業者番号',
    type: 'text',
  },
  {
    name: field('energyManageFactoryNumber'),
    label: 'エネルギー管理指定工場番号',
    type: 'text',
  },
];
