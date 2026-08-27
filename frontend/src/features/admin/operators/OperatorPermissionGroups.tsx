import {
  getOperatorPermissionLabel,
  groupOperatorPermissions,
} from './operatorPermissions';

type OperatorPermissionGroupsProps = {
  permissions: readonly string[];
};

/** Displays operator permissions as Figma-style category groups and compact chips. */
export function OperatorPermissionGroups({
  permissions,
}: Readonly<OperatorPermissionGroupsProps>) {
  if (permissions.length === 0) {
    return <p className="text-sm text-muted-foreground">権限なし</p>;
  }

  const permissionGroups = groupOperatorPermissions(permissions);

  return (
    <div className="space-y-5">
      {permissionGroups.map((group) => (
        <section key={group.category} className="space-y-2.5">
          <div className="flex items-baseline gap-2">
            <h3 className="text-sm font-bold text-foreground">
              {group.category}
            </h3>
            <span className="text-xs text-muted-foreground">
              {group.permissions.length}件
            </span>
          </div>
          <ul className="flex flex-wrap gap-2">
            {group.permissions.map((permission) => (
              <li
                key={permission}
                className="rounded-full border border-primary/25 bg-primary/5 px-3 py-1.5 text-xs text-primary"
              >
                {getOperatorPermissionLabel(permission)}
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
