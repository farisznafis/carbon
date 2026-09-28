import { useCallback, useMemo, type ReactNode } from 'react';
import type { AdminOperatorPermissionCode } from '@/api/admin/operators';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { useCurrentAdminOperatorProfileQuery } from '@/hooks/admin/useCurrentAdminOperatorProfileQuery';
import { normalizeOperatorRole } from '@/features/admin/operators/operatorRoles';
import { OPERATOR_PERMISSION_OPTIONS } from '@/features/admin/operators/operatorPermissions';
import { AdminPermissionContext, type AdminPermissionContextValue } from './useAdminPermissions';

const KNOWN_PERMISSION_CODES = new Set<AdminOperatorPermissionCode>(
  OPERATOR_PERMISSION_OPTIONS.map((permission) => permission.code),
);

/** Returns whether an API permission belongs to the supported 19-code contract. */
function isKnownPermissionCode(permission: string): permission is AdminOperatorPermissionCode {
  return KNOWN_PERMISSION_CODES.has(permission as AdminOperatorPermissionCode);
}

/** Provides the signed-in admin portal account's normalized permission state. */
export function AdminPermissionProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const { toast } = useToast();
  const profileQuery = useCurrentAdminOperatorProfileQuery(user);
  const operator = profileQuery.operator;
  const role = normalizeOperatorRole(operator?.role) === 'admin' ? 'admin' : 'operator';
  const isAdmin = role === 'admin';
  const permissions = useMemo(
    () => new Set((operator?.permissions ?? []).filter(isKnownPermissionCode)),
    [operator?.permissions],
  );

  /** Checks one permission while allowing administrator accounts to bypass assignments. */
  const hasPermission = useCallback(
    (permission: AdminOperatorPermissionCode) => isAdmin || permissions.has(permission),
    [isAdmin, permissions],
  );

  /** Runs an authorized UI action or reports the missing permission without a request. */
  const guardAction = useCallback(
    (permission: AdminOperatorPermissionCode, action: () => void) => {
      if (hasPermission(permission)) {
        action();
        return true;
      }

      toast({
        title: '権限がありません',
        description: 'この操作を行う権限がありません。必要な場合は管理者にお問い合わせください。',
        variant: 'destructive',
      });
      return false;
    },
    [hasPermission, toast],
  );

  const contextValue = useMemo<AdminPermissionContextValue>(
    () => ({
      operatorId: operator?.id ?? profileQuery.resolvedOperatorId ?? null,
      role,
      permissions,
      isAdmin,
      hasAnyPermission: isAdmin || permissions.size > 0,
      isLoading: profileQuery.isLoading,
      isError: profileQuery.isError || (!profileQuery.isLoading && !operator),
      hasPermission,
      guardAction,
      retry: profileQuery.refetch,
    }),
    [guardAction, hasPermission, isAdmin, operator, permissions, profileQuery.isError, profileQuery.isLoading, profileQuery.refetch, profileQuery.resolvedOperatorId, role],
  );

  return <AdminPermissionContext.Provider value={contextValue}>{children}</AdminPermissionContext.Provider>;
}
