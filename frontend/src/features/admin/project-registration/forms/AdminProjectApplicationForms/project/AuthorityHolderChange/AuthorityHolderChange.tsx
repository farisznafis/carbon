// 認証権限保有主体変更届（所有者を変更する方のみ提出）

import { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

/**
 * Filed only when the project's owner changes.
 *
 * The blank workbook keeps this sheet hidden; はじめに rows 44–51 say to unhide
 * it, fill it, and then pick 認証権限保有主体変更 as the 申請区分 on 登録申請書.
 * The backend registers the page `optional`, so a plan that never files it can
 * still read complete.
 */
export const authorityHolderChange: FieldConfig<FormValues>[] = [
  { name: 'authorityHolderChange.submissionDate', label: '届出日', type: 'date' },

  // １．プロジェクト登録情報 — both filled from the project record.
  {
    name: 'authorityHolderChange.projectNumber',
    label: 'プロジェクト番号',
    type: 'text',
  },
  {
    name: 'authorityHolderChange.projectOperatorName',
    label: 'プロジェクト実施者名（プログラム型運営・管理者名）',
    type: 'text',
  },

  // ２．所有者変更内容 — the incoming owner.
  {
    name: 'authorityHolderChange.newAccountNumber',
    label: '変更後の口座番号',
    type: 'text',
    placeholder: 'JP-100-20000-00001-____-00',
  },
  {
    name: 'authorityHolderChange.newCorpName',
    label: '変更後の法人名',
    type: 'text',
  },
  {
    name: 'authorityHolderChange.newPicPositionAndName',
    label: '変更後の法人の担当者所属・氏名',
    type: 'text',
  },
  {
    name: 'authorityHolderChange.newPicEmail',
    label: '変更後の法人の担当者連絡先（メールアドレス）',
    type: 'email',
  },
];
