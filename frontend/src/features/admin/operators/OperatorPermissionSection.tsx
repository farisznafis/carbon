import { useId } from 'react';
import { Checkbox } from '@/components/ui/checkbox';
import { FormSection } from '@/components/forms/form-layout';
import {
  groupOperatorPermissionOptions,
  OPERATOR_PERMISSION_OPTIONS,
} from './operatorPermissions';

type OperatorPermissionSectionProps = {
  selectedPermissions: string[];
  onChange: (permissions: string[]) => void;
  error?: string | null;
  sectionNumber?: number;
  sectionId?: string;
};

export const OPERATOR_PERMISSION_SECTION_ID = 'permissions';

const ALL_PERMISSION_CODES = OPERATOR_PERMISSION_OPTIONS.map(
  (permission) => permission.code,
);

type PermissionSelectionState = boolean | 'indeterminate';

/** Returns the checkbox state for a permission scope, including partial selection. */
function getPermissionSelectionState(
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

/** Updates only the requested permission scope while preserving unknown backend permissions. */
function updatePermissionScope(
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

type PermissionSelectAllProps = {
  checked: PermissionSelectionState;
  ariaLabel: string;
  onCheckedChange: (checked: boolean) => void;
};

/** Renders the shared select-all control used globally and within each group. */
function PermissionSelectAll({
  checked,
  ariaLabel,
  onCheckedChange,
}: Readonly<PermissionSelectAllProps>) {
  return (
    <label className="flex cursor-pointer items-center gap-3 whitespace-nowrap text-sm text-muted-foreground">
      <Checkbox
        checked={checked}
        aria-label={ariaLabel}
        className="h-5 w-5"
        onCheckedChange={(nextChecked) => onCheckedChange(nextChecked === true)}
      />
      <span>すべて選択</span>
    </label>
  );
}

/** Renders the permission-code selection reused by the admin operator form. */
export function OperatorPermissionSection({
  selectedPermissions,
  onChange,
  error,
  sectionNumber = 4,
  sectionId,
}: Readonly<OperatorPermissionSectionProps>) {
  const permissionSectionId = useId();
  const groupedPermissions = groupOperatorPermissionOptions();
  const selectedPermissionSet = new Set(selectedPermissions);
  const selectedPermissionCount = ALL_PERMISSION_CODES.filter((permissionCode) =>
    selectedPermissionSet.has(permissionCode),
  ).length;
  const allPermissionSelectionState = getPermissionSelectionState(
    selectedPermissionSet,
    ALL_PERMISSION_CODES,
  );

  /** Adds or removes one permission code without mutating the current selection. */
  const handlePermissionChange = (permissionCode: string, checked: boolean) => {
    onChange(
      updatePermissionScope(selectedPermissions, [permissionCode], checked),
    );
  };

  /** Selects or clears every known permission while retaining unknown API values. */
  const handleAllPermissionsChange = (checked: boolean) => {
    onChange(
      updatePermissionScope(
        selectedPermissions,
        ALL_PERMISSION_CODES,
        checked,
      ),
    );
  };

  /** Selects or clears every permission within one business group. */
  const handlePermissionGroupChange = (
    permissionCodes: readonly string[],
    checked: boolean,
  ) => {
    onChange(updatePermissionScope(selectedPermissions, permissionCodes, checked));
  };

  return (
    <FormSection
      id={sectionId}
      number={sectionNumber}
      title="利用権限"
      description="権限は登録後でも追加・変更できます。"
      actions={
        <div className="flex flex-wrap items-center gap-x-5 gap-y-3">
          <p className="text-sm font-semibold text-primary" aria-live="polite">
            選択済み {selectedPermissionCount} / {ALL_PERMISSION_CODES.length}
          </p>
          <PermissionSelectAll
            checked={allPermissionSelectionState}
            ariaLabel="すべての権限を選択"
            onCheckedChange={handleAllPermissionsChange}
          />
        </div>
      }
      tabIndex={sectionId ? -1 : undefined}
      className={sectionId ? 'scroll-mt-24' : undefined}
    >
      <div className="-space-y-2">
        {groupedPermissions.map((group, groupIndex) => {
          const groupPermissionCodes = group.permissions.map(
            (permission) => permission.code,
          );
          const groupTitleId = `${permissionSectionId}-group-${groupIndex}`;
          const groupSelectionState = getPermissionSelectionState(
            selectedPermissionSet,
            groupPermissionCodes,
          );

          return (
            <section
              key={group.category}
              aria-labelledby={groupTitleId}
              className="py-5 first:pt-0 last:pb-0"
            >
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border pb-3">
                <h4 id={groupTitleId} className="font-bold text-foreground">
                  {group.category}
                </h4>
                <PermissionSelectAll
                  checked={groupSelectionState}
                  ariaLabel={`${group.category}をすべて選択`}
                  onCheckedChange={(checked) =>
                    handlePermissionGroupChange(groupPermissionCodes, checked)
                  }
                />
              </div>
              <div className="grid gap-x-10 gap-y-2 pt-4 sm:grid-cols-2">
                {group.permissions.map((permission) => {
                  const checked = selectedPermissionSet.has(permission.code);
                  return (
                    <label
                      key={permission.code}
                      className="flex min-h-10 cursor-pointer items-center gap-4 text-sm text-foreground"
                    >
                      <Checkbox
                        checked={checked}
                        className="h-5 w-5"
                        onCheckedChange={(value) =>
                          handlePermissionChange(
                            permission.code,
                            value === true,
                          )
                        }
                      />
                      <span>{permission.label}</span>
                    </label>
                  );
                })}
              </div>
            </section>
          );
        })}
      </div>
      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}
    </FormSection>
  );
}
