import type { AdminOperator } from '@/api/admin/operators';

const OPERATOR_ROLE_LABELS: Readonly<Record<string, string>> = {
  admin: '管理者',
  operator: '運用者',
};

/** Normalizes backend role values before role-sensitive UI decisions. */
export function normalizeOperatorRole(role: string | null | undefined): string {
  return role?.trim().toLocaleLowerCase('en-US') ?? '';
}

/** Returns whether a backend role represents an administrator account. */
export function isAdminOperatorRole(role: string | null | undefined): boolean {
  return normalizeOperatorRole(role) === 'admin';
}

/** Converts a backend role into its Japanese profile label. */
export function getOperatorRoleLabel(role: string | null | undefined): string {
  const normalizedRole = normalizeOperatorRole(role);
  return OPERATOR_ROLE_LABELS[normalizedRole] ?? (role?.trim() || '-');
}

/** Prefers the list role for one operator while retaining detail data for direct access. */
export function resolveOperatorRole(
  operator: AdminOperator | undefined,
  operators: readonly AdminOperator[],
): string {
  if (!operator) return '';

  return (
    operators.find((candidate) => candidate.id === operator.id)?.role ||
    operator.role
  );
}
