//適用する方法論

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { PROJECT_TYPES } from '@/lib/constants/project_types';

export const applicableMethodology: FieldConfig<FormValues>[] = [
  {
    name: 'applicableMethodology.methodologyCategory',
    label: '方法論カテゴリー',
    type: 'text',
  },
  {
    name: 'applicableMethodology.methodologyNumber',
    label: '方法論番号',
    type: 'text',
  },
  {
    name: 'applicableMethodology.methodologyVersion',
    label: '方法論バージョン',
    type: 'text',
  },
  {
    name: 'applicableMethodology.projectType',
    label: 'プロジェクトタイプ',
    type: 'select',
    options: PROJECT_TYPES,
  },

  // hide if projectType is "更新プロジェクトと新設プロジェクトの双方を対象とする"
  {
    name: 'applicableMethodology.approachDetails',
    label: 'アプローチの詳細',
    type: 'textarea',
    hideWhen: [{
      field: 'applicableMethodology.projectType',
      value: '更新プロジェクトのみを対象とする',
    }]
  },
  {
    name: 'applicableMethodology.reductionMethodSummary',
    label: '削減方法の概要',
    type: 'textarea',
  },
  // プロジェクト実施前の燃料種：
  {
    name: 'applicableMethodology.fuelTypeBeforeImpl',
    label: '導入前の燃料タイプ',
    type: 'text',
  },
  // プロジェクト実施前の対象設備：
  {
    name: 'applicableMethodology.targetEquipmentBeforeImpl',
    label: '導入前の対象設備',
    type: 'text',
  },
  {
    name: 'applicableMethodology.overviewDiagramBeforeAct',
    label: '導入前の概要図',
    type: 'file',
  },
  // プロジェクト実施後の燃料種：
  {
    name: 'applicableMethodology.fuelTypeAfterImpl',
    label: '導入後の燃料タイプ',
    type: 'text',
  },
  // プロジェクト実施後の対象設備：
  {
    name: 'applicableMethodology.targetEquipmentAfterImpl',
    label: '導入後の対象設備',
    type: 'text',
  },
  {
    name: 'applicableMethodology.overviewDiagramAfterAct',
    label: '導入後の概要図',
    type: 'file',
  },
  // 方法論の適用条件について適合している理由を下記に記入してください。
  // can be add more rows
  {
    name: 'applicableMethodology.reductionActivityMethodCondition',
    label: '削減活動方法の条件',
    type: 'textarea',
  },
];
