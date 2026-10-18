import { useId, useState } from 'react';
import { ChevronDown } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import {
  getOperatorPermissionLabel,
  groupOperatorPermissionOptions,
} from './operatorPermissions';
import {
  getPermissionSelectionState,
  OPERATOR_PERMISSION_CODES,
  type PermissionSelectionState,
  updatePermissionScope,
} from './operatorPermissionSelection';
import {
  createAllOperatorPermissionFilterValue,
  isAllOperatorPermissionFilterSelected,
  type OperatorPermissionFilterValue,
} from './operatorPermissionFilterModel';

type PermissionFilterCheckboxProps = {
  id: string;
  label: string;
  checked: PermissionSelectionState;
  onCheckedChange: (checked: boolean) => void;
  ariaLabel?: string;
};

type OperatorPermissionFilterProps = {
  value: OperatorPermissionFilterValue;
  onChange: (value: OperatorPermissionFilterValue) => void;
};

/** Formats the compact trigger label from the current multi-select state. */
function getPermissionFilterTriggerLabel(
  value: OperatorPermissionFilterValue,
): string {
  if (isAllOperatorPermissionFilterSelected(value)) return 'すべての権限';

  const selectedOptionCount = value.permissionCodes.length;
  if (selectedOptionCount === 0) return '権限なし';
  if (value.permissionCodes.length === 1) {
    return getOperatorPermissionLabel(value.permissionCodes[0]);
  }
  return `${selectedOptionCount}件選択`;
}

/** Renders one accessible checkbox row used by filter-level and group-level controls. */
function PermissionFilterCheckbox({
  id,
  label,
  checked,
  onCheckedChange,
  ariaLabel,
}: Readonly<PermissionFilterCheckboxProps>) {
  return (
    <div className="flex min-h-9 items-center gap-3">
      <Checkbox
        id={id}
        checked={checked}
        aria-label={ariaLabel}
        className="h-5 w-5"
        onCheckedChange={(nextChecked) => onCheckedChange(nextChecked === true)}
      />
      <label
        htmlFor={id}
        className="min-w-0 cursor-pointer text-sm text-foreground"
      >
        {label}
      </label>
    </div>
  );
}

/** Renders the grouped permission multi-select used by the operator list filter. */
export function OperatorPermissionFilter({
  value,
  onChange,
}: Readonly<OperatorPermissionFilterProps>) {
  const filterId = useId();
  const [open, setOpen] = useState(false);
  const permissionGroups = groupOperatorPermissionOptions();
  const selectedPermissionSet = new Set(value.permissionCodes);
  const globalSelectionState = getPermissionSelectionState(
    selectedPermissionSet,
    OPERATOR_PERMISSION_CODES,
  );
  const selectedPermissionCount = OPERATOR_PERMISSION_CODES.filter(
    (permissionCode) => selectedPermissionSet.has(permissionCode),
  ).length;

  /** Selects every permission or clears all to show permissionless operators. */
  const handleGlobalSelectionChange = (checked: boolean) => {
    onChange(
      checked
        ? createAllOperatorPermissionFilterValue()
        : { permissionCodes: [] },
    );
  };

  /** Selects or clears every permission within one business group. */
  const handleGroupSelectionChange = (
    permissionCodes: readonly string[],
    checked: boolean,
  ) => {
    onChange({
      ...value,
      permissionCodes: updatePermissionScope(
        value.permissionCodes,
        permissionCodes,
        checked,
      ),
    });
  };

  /** Selects or clears one permission in the active filter. */
  const handlePermissionChange = (permissionCode: string, checked: boolean) => {
    onChange({
      ...value,
      permissionCodes: updatePermissionScope(
        value.permissionCodes,
        [permissionCode],
        checked,
      ),
    });
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          size="md"
          role="combobox"
          aria-label="権限で絞り込む"
          aria-expanded={open}
          className="w-fit min-w-0 justify-between gap-3 bg-card font-normal rounded-md"
        >
          <span className="truncate">{getPermissionFilterTriggerLabel(value)}</span>
          <ChevronDown className="h-4 w-4 shrink-0 opacity-50" aria-hidden="true" />
        </Button>
      </PopoverTrigger>
      <PopoverContent
        align="start"
        className="w-[min(26rem,calc(100vw-2rem))] p-0"
      >
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border p-4">
          <p className="text-sm font-bold text-primary" aria-live="polite">
            選択済み {selectedPermissionCount} / {OPERATOR_PERMISSION_CODES.length}
          </p>
          <PermissionFilterCheckbox
            id={`${filterId}-all`}
            label="すべての権限"
            checked={globalSelectionState}
            ariaLabel="すべての権限を選択"
            onCheckedChange={handleGlobalSelectionChange}
          />
        </div>

        <div className="max-h-96 sm:max-h-72 overflow-y-auto p-3">
          <div className="space-y-5">
            {permissionGroups.map((group, groupIndex) => {
              const groupPermissionCodes = group.permissions.map(
                (permission) => permission.code,
              );
              const groupSelectionState = getPermissionSelectionState(
                selectedPermissionSet,
                groupPermissionCodes,
              );

              return (
                <section key={group.category} className="space-y-2">
                  <div className="flex items-center justify-between gap-4 border-b border-border pb-2">
                    <h3 className="text-sm font-bold text-foreground">
                      {group.category}
                    </h3>
                    <PermissionFilterCheckbox
                      id={`${filterId}-group-${groupIndex}`}
                      label="すべて選択"
                      checked={groupSelectionState}
                      ariaLabel={`${group.category}をすべて選択`}
                      onCheckedChange={(checked) =>
                        handleGroupSelectionChange(groupPermissionCodes, checked)
                      }
                    />
                  </div>
                  <div className="space-y-1">
                    {group.permissions.map((permission) => (
                      <PermissionFilterCheckbox
                        key={permission.code}
                        id={`${filterId}-${permission.code}`}
                        label={permission.label}
                        checked={selectedPermissionSet.has(permission.code)}
                        onCheckedChange={(checked) =>
                          handlePermissionChange(permission.code, checked)
                        }
                      />
                    ))}
                  </div>
                </section>
              );
            })}
          </div>
        </div>
      </PopoverContent>
    </Popover>
  );
}
