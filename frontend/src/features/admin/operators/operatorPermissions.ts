import type { AdminOperatorPermissionCode } from '@/api/admin/operators';

export type OperatorPermissionOption = {
  code: AdminOperatorPermissionCode;
  label: string;
  category: string;
};

export const OPERATOR_PERMISSION_OPTIONS: readonly OperatorPermissionOption[] = [
  { code: 'PROJECT_CREATE', label: 'プロジェクト登録', category: 'プロジェクト管理' },
  { code: 'PROJECT_READ', label: 'プロジェクト閲覧', category: 'プロジェクト管理' },
  { code: 'PROJECT_UPDATE', label: 'プロジェクト更新', category: 'プロジェクト管理' },
  { code: 'PROJECT_STATUS_UPDATE', label: 'プロジェクトステータス更新', category: 'プロジェクト管理' },
  { code: 'PROJECT_APPLICATION_OUTPUT', label: 'プロジェクト申請書出力', category: 'プロジェクト管理' },
  { code: 'PROJECT_VIEW', label: 'プロジェクト詳細閲覧', category: 'プロジェクト管理' },
  { code: 'PARTICIPATING_COMPANY_READ', label: '参加企業閲覧', category: '企業管理' },
  { code: 'PARTICIPATING_COMPANY_APPLICATION_REVIEW', label: '参加企業申請審査', category: '企業管理' },
  { code: 'PARTICIPATING_COMPANY_SUSPEND', label: '参加企業利用停止', category: '企業管理' },
  { code: 'MASTER_USER_PASSWORD_RESET', label: 'マスターユーザーパスワード再設定', category: '企業管理' },
  { code: 'OPERATOR_USER_READ', label: '運用者閲覧', category: '運営管理' },
  { code: 'OPERATOR_USER_CREATE', label: '運用者登録', category: '運営管理' },
  { code: 'OPERATOR_USER_UPDATE', label: '運用者更新', category: '運営管理' },
  { code: 'METHODOLOGY_READ', label: '方法論閲覧', category: '方法論' },
  { code: 'MONITORING_READ', label: 'モニタリング閲覧', category: 'モニタリング管理' },
  { code: 'MONITORING_DELETE', label: 'モニタリング削除', category: 'モニタリング管理' },
  { code: 'MONITORING_CANCEL', label: 'モニタリング取消', category: 'モニタリング管理' },
  { code: 'MONITORING_BATCH_HISTORY_READ', label: 'バッチ履歴閲覧', category: 'モニタリング管理' },
  { code: 'MONITORING_BATCH_ERROR_READ', label: 'バッチエラー閲覧', category: 'モニタリング管理' },
  { code: 'APPLICATION_FORM_READ', label: '申請書閲覧', category: '申請管理' },
] as const;

const PERMISSION_CATEGORY_BY_CODE = new Map<string, string>(
  OPERATOR_PERMISSION_OPTIONS.map((permission) => [
    permission.code,
    permission.category,
  ]),
);

const PERMISSION_LABEL_BY_CODE = new Map<string, string>(
  OPERATOR_PERMISSION_OPTIONS.map((permission) => [
    permission.code,
    permission.label,
  ]),
);

/** Returns the Japanese permission label and safely falls back to an unknown API value. */
export function getOperatorPermissionLabel(permission: string): string {
  return PERMISSION_LABEL_BY_CODE.get(permission) ?? permission;
}

/** Returns the primary permission category used by the compact list summary. */
export function getOperatorPermissionCategory(permission: string): string {
  return PERMISSION_CATEGORY_BY_CODE.get(permission) ?? permission;
}
