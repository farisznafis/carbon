import { OPERATOR_PERMISSION_OPTIONS } from './operatorPermissions';

export const OPERATOR_PERMISSION_CODES = OPERATOR_PERMISSION_OPTIONS.map(
  (permission) => permission.code,
);

export type PermissionSelectionState = boolean | 'indeterminate';

/** Returns the checkbox state for a permission scope, including partial selection. */
export function getPermissionSelectionState(
  selectedPermissions: ReadonlySet<string>,
  permissionCodes: readonly string[],
): PermissionSelectionState {
  const selectedCount = permissionCodes.filter((permissionCode) =>
    selectedPermissions.has(permissionCode),
  ).length;

  if (selectedCount === 0) return false;
  if (selectedCount === permissionCodes.length) return true;
  return 'indeterminate';
}

/** Updates only the requested permission scope while preserving values outside that scope. */
export function updatePermissionScope(
  selectedPermissions: readonly string[],
  permissionCodes: readonly string[],
  checked: boolean,
): string[] {
  const permissionCodeSet = new Set(permissionCodes);

  if (checked) {
    return Array.from(new Set([...selectedPermissions, ...permissionCodes]));
  }

  return selectedPermissions.filter(
    (permissionCode) => !permissionCodeSet.has(permissionCode),
  );
}
