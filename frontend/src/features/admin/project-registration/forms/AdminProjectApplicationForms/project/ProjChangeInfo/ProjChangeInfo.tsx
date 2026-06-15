import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import { REVIEWER_NAMES } from '@/lib/constants/reviewer_names';
import { IS_NEED_REVALIDATIONS } from '@/lib/constants/select_types';

export const projChangeInfo: FieldConfig<FormValues>[] = [
  { name: 'projChangeInfo.numberOfChanges', label: '変更回数', type: 'number' },
  {
    name: 'projChangeInfo.applicationChangeDate',
    label: '変更申請日（西暦年月日）',
    type: 'date',
  },
  {
    name: 'projChangeInfo.effectiveDate',
    label: '変更内容の適用開始日（西暦年月日）',
    type: 'date',
  },
  // プログラム型運営・管理者の社名の変更（特定した排出源の変更等を伴いプロジェクトの実態に影響を与えるものを除く）	
  {
    name: 'projChangeInfo.changeTypeOperatorName',
    label: '変更種別：プログラム型運営・管理者の社名の変更',
    type: 'checkbox',
  },
  // クレジット取得予定者の変更
  {
    name: 'projChangeInfo.changeTypeCreditHolder',
    label: '変更種別：クレジット取得予定者の変更',
    type: 'checkbox',
  },
  // プログラム型プロジェクトにおける認証対象期間の延長（実施要綱1.6の規程に基づくもの。個々の削減活動ではなく、プロジェクト全体の認証対象期間の延長を指す）
  {
    name: 'projChangeInfo.changeTypeExtension',
    label: '変更種別：プログラム型プロジェクトにおける認証対象期間の延長',
    type: 'checkbox',
  },
  {
    name: 'projChangeInfo.changeTypeOther',
    label: '変更種別：形式的な変更以外',
    type: 'checkbox',
  },
  { name: 'projChangeInfo.overview', label: '変更概要', type: 'textarea' },
  // 再妥当性確認の要否	
  {
    name: 'projChangeInfo.isNeedRevalidation',
    label: '再妥当性確認要否',
    type: 'select',
    options: IS_NEED_REVALIDATIONS,
  },
  {
    name: 'projChangeInfo.reviewerName',
    label: '審査機関名',
    type: 'select',
    options: REVIEWER_NAMES,
  },
  {
    name: 'projChangeInfo.historyNumberOfChanges',
    label: '[変更申請履歴]変更回数',
    type: 'number',
  },
  {
    name: 'projChangeInfo.historyApplicationChangeDate',
    label: '[変更申請履歴]変更申請日（西暦年月日）',
    type: 'date',
  },
  {
    name: 'projChangeInfo.historyChangeTypeOperatorName',
    label: '[変更申請履歴]変更種別：プログラム型運営・管理者の社名の変更',
    type: 'checkbox',
  },
  {
    name: 'projChangeInfo.historyChangeTypeCreditHolder',
    label: '[変更申請履歴]変更種別：クレジット取得予定者の変更',
    type: 'checkbox',
  },
  {
    name: 'projChangeInfo.historyChangeTypeExtension',
    label:
      '[変更申請履歴]変更種別：プログラム型プロジェクトにおける認証対象期間の延長',
    type: 'checkbox',
  },
  {
    name: 'projChangeInfo.historyChangeTypeOther',
    label: '[変更申請履歴]変更種別：形式的な変更以外',
    type: 'checkbox',
  },
  {
    name: 'projChangeInfo.historyOverview',
    label: '[変更申請履歴]変更概要',
    type: 'textarea',
  },
];
