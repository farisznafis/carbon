const USER_ROLE_LABELS: Readonly<Record<string, string>> = {
  admin: '管理者',
  master: 'マスター',
  operator: '運用者',
  user: '社員',
};

/** Converts an API role value into the Japanese label used by account UI. */
export function formatUserRole(
  role: string | null | undefined,
  fallback = 'ユーザー',
): string {
  const normalizedRole = role?.trim().toLocaleLowerCase('en-US') ?? '';
  return USER_ROLE_LABELS[normalizedRole] ?? (role?.trim() || fallback);
}
