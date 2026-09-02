import { OPERATOR_PERMISSION_CODES } from './operatorPermissionSelection';

export type OperatorPermissionFilterValue = {
  permissionCodes: string[];
};

/** Creates the default filter that keeps every operator visible. */
export function createAllOperatorPermissionFilterValue(): OperatorPermissionFilterValue {
  return {
    permissionCodes: [...OPERATOR_PERMISSION_CODES],
  };
}

/** Returns whether every available permission is selected, which means no filtering. */
export function isAllOperatorPermissionFilterSelected(
  value: OperatorPermissionFilterValue,
): boolean {
  const selectedPermissionSet = new Set(value.permissionCodes);
  return OPERATOR_PERMISSION_CODES.every((permissionCode) =>
    selectedPermissionSet.has(permissionCode),
  );
}

/** Matches permissionless operators when empty, otherwise uses AND semantics. */
export function matchesOperatorPermissionFilter(
  operatorPermissions: readonly string[],
  value: OperatorPermissionFilterValue,
): boolean {
  if (isAllOperatorPermissionFilterSelected(value)) return true;
  if (value.permissionCodes.length === 0) {
    return operatorPermissions.length === 0;
  }

  const operatorPermissionSet = new Set(operatorPermissions);
  return value.permissionCodes.every((permissionCode) =>
    operatorPermissionSet.has(permissionCode),
  );
}
