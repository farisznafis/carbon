import type { AdminOperatorPermissionCode } from '@/api/admin/operators';

export type OperatorPermissionOption = {
  code: AdminOperatorPermissionCode;
  label: string;
  category: string;
};

export type OperatorPermissionGroup = {
  category: string;
  permissions: string[];
};

export type OperatorPermissionOptionGroup = {
  category: string;
  permissions: readonly OperatorPermissionOption[];
};

const FALLBACK_PERMISSION_CATEGORY = 'その他';

const PERMISSION_CATEGORY_ORDER = [
  'プロジェクト管理',
  '参加企業・申請管理',
  'モニタリング管理',
  '運用者管理',
  '方法論・帳票',
  FALLBACK_PERMISSION_CATEGORY,
] as const;

export const OPERATOR_PERMISSION_OPTIONS: readonly OperatorPermissionOption[] = [
  { code: 'PROJECT_CREATE', label: 'プロジェクト作成', category: 'プロジェクト管理' },
  { code: 'PROJECT_READ', label: 'プロジェクト参照', category: 'プロジェクト管理' },
  { code: 'PROJECT_UPDATE', label: 'プロジェクト更新', category: 'プロジェクト管理' },
  { code: 'PROJECT_STATUS_UPDATE', label: 'プロジェクトステータス変更', category: 'プロジェクト管理' },
  { code: 'PROJECT_APPLICATION_OUTPUT', label: '申請書出力', category: 'プロジェクト管理' },
  { code: 'PARTICIPATING_COMPANY_READ', label: '参加企業参照', category: '参加企業・申請管理' },
  { code: 'PARTICIPATING_COMPANY_APPLICATION_REVIEW', label: '参加申請処理', category: '参加企業・申請管理' },
  { code: 'PARTICIPATING_COMPANY_SUSPEND', label: '参加企業一時停止', category: '参加企業・申請管理' },
  { code: 'MASTER_USER_PASSWORD_RESET', label: 'マスターユーザーパスワードリセット', category: '参加企業・申請管理' },
  { code: 'OPERATOR_USER_READ', label: '運用者一覧参照', category: '運用者管理' },
  { code: 'OPERATOR_USER_CREATE', label: '運用者作成', category: '運用者管理' },
  { code: 'OPERATOR_USER_UPDATE', label: '運用者更新', category: '運用者管理' },
  { code: 'METHODOLOGY_READ', label: '方法論参照', category: '方法論・帳票' },
  { code: 'MONITORING_READ', label: 'モニタリングデータ参照', category: 'モニタリング管理' },
  { code: 'MONITORING_DELETE', label: 'モニタリングデータ削除', category: 'モニタリング管理' },
  { code: 'MONITORING_CANCEL', label: 'モニタリングデータ取消', category: 'モニタリング管理' },
  { code: 'MONITORING_BATCH_HISTORY_READ', label: 'バッチ履歴参照', category: 'モニタリング管理' },
  { code: 'MONITORING_BATCH_ERROR_READ', label: 'バッチエラー参照', category: 'モニタリング管理' },
  { code: 'APPLICATION_FORM_READ', label: '申請書様式参照', category: '方法論・帳票' },
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

const PERMISSION_ORDER_BY_CODE = new Map<string, number>(
  OPERATOR_PERMISSION_OPTIONS.map((permission, index) => [permission.code, index]),
);

/** Returns the Japanese permission label and safely falls back to an unknown API value. */
export function getOperatorPermissionLabel(permission: string): string {
  return PERMISSION_LABEL_BY_CODE.get(permission) ?? permission;
}

/** Returns the primary permission category used by the compact list summary. */
export function getOperatorPermissionCategory(permission: string): string {
  return PERMISSION_CATEGORY_BY_CODE.get(permission) ?? FALLBACK_PERMISSION_CATEGORY;
}

/** Groups permission options in the shared business order used by operator forms. */
export function groupOperatorPermissionOptions(): OperatorPermissionOptionGroup[] {
  return PERMISSION_CATEGORY_ORDER
    .filter((category) => category !== FALLBACK_PERMISSION_CATEGORY)
    .map((category) => ({
      category,
      permissions: OPERATOR_PERMISSION_OPTIONS.filter(
        (permission) => permission.category === category,
      ),
    }))
    .filter((group) => group.permissions.length > 0);
}

/** Groups API permission codes in the stable category order used by profile and filters. */
export function groupOperatorPermissions(
  permissions: readonly string[],
): OperatorPermissionGroup[] {
  const groupedPermissions = new Map<string, string[]>();

  permissions.forEach((permission) => {
    const category = getOperatorPermissionCategory(permission);
    const categoryPermissions = groupedPermissions.get(category) ?? [];
    categoryPermissions.push(permission);
    groupedPermissions.set(category, categoryPermissions);
  });

  return Array.from(groupedPermissions, ([category, categoryPermissions]) => ({
    category,
    permissions: categoryPermissions.sort(
      (leftPermission, rightPermission) =>
        (PERMISSION_ORDER_BY_CODE.get(leftPermission) ?? Number.MAX_SAFE_INTEGER) -
        (PERMISSION_ORDER_BY_CODE.get(rightPermission) ?? Number.MAX_SAFE_INTEGER),
    ),
  })).sort(
    (leftGroup, rightGroup) =>
      PERMISSION_CATEGORY_ORDER.indexOf(
        leftGroup.category as (typeof PERMISSION_CATEGORY_ORDER)[number],
      ) -
      PERMISSION_CATEGORY_ORDER.indexOf(
        rightGroup.category as (typeof PERMISSION_CATEGORY_ORDER)[number],
      ),
  );
}
