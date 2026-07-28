//プログラム型プロジェクトの運営・管理_方法論

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

export const programMethodology: FieldConfig<FormValues>[] = [
  {
    name: 'programMethodology.opeManageStructure',
    label: '運営・管理体制',
    type: 'textarea',
  },
  {
    name: 'programMethodology.structureDiagram',
    label: '構成図',
    type: 'file',
  },
  {
    name: 'programMethodology.methodContentOfAggreement',
    label: '協定内容の方法論',
    type: 'textarea',
  },
  {
    name: 'programMethodology.infoCollManageMethod',
    label: '情報収集・管理方法',
    type: 'textarea',
  },
  {
    name: 'programMethodology.domesticImplementation',
    label: '国内実施の有無',
    type: 'textarea',
  },
  {
    name: 'programMethodology.registrationInput',
    label: '登録入力',
    type: 'textarea',
  },
  {
    name: 'programMethodology.certExtensionInput',
    label: '認証延長入力',
    type: 'textarea',
  },
  {
    name: 'programMethodology.mustImplAfterTwoYears',
    label: '2年後に必ず実施',
    type: 'textarea',
  },
  {
    name: 'programMethodology.additionality',
    label: '追加性',
    type: 'textarea',
  },
  {
    name: 'programMethodology.implByApprovedMethod',
    label: '承認された方法による実施',
    type: 'textarea',
  },
  {
    name: 'programMethodology.otherProjectJcredit',
    label: '他のプロジェクト・J-クレジット制度',
    type: 'textarea',
  },
  {
    name: 'programMethodology.otherSimilarSystem',
    label: '他の類似システム',
    type: 'textarea',
  },
  {
    name: 'programMethodology.methodIsCommonInfo',
    label: '方法論は一般的な情報です',
    type: 'textarea',
  },
  {
    name: 'programMethodology.moniItemIsCommonInfo',
    label: '監視項目は一般的な情報です',
    type: 'textarea',
  },
];
