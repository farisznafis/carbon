import { createContext, useContext } from 'react';
import type { AdminOperatorPermissionCode } from '@/api/admin/operators';

export type AdminPermissionContextValue = {
  operatorId: string | null;
  role: 'admin' | 'operator';
  permissions: ReadonlySet<AdminOperatorPermissionCode>;
  isAdmin: boolean;
  hasAnyPermission: boolean;
  isLoading: boolean;
  isError: boolean;
  hasPermission: (permission: AdminOperatorPermissionCode) => boolean;
  guardAction: (permission: AdminOperatorPermissionCode, action: () => void) => boolean;
  retry: () => Promise<unknown>;
};

export const AdminPermissionContext = createContext<AdminPermissionContextValue | null>(null);

/** Reads the required admin permission provider. */
export function useAdminPermissions(): AdminPermissionContextValue {
  const context = useContext(AdminPermissionContext);
  if (!context) throw new Error('useAdminPermissions must be used within AdminPermissionProvider');
  return context;
}
