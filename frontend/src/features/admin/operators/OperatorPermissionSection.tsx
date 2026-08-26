import { Checkbox } from '@/components/ui/checkbox';
import { FormSection } from '@/components/forms/form-layout';
import { cn } from '@/lib/utils';
import { OPERATOR_PERMISSION_OPTIONS } from './operatorPermissions';

type OperatorPermissionSectionProps = {
  selectedPermissions: string[];
  onChange: (permissions: string[]) => void;
  error?: string | null;
};

/** Renders the permission-code selection reused by the admin operator form. */
export function OperatorPermissionSection({
  selectedPermissions,
  onChange,
  error,
}: Readonly<OperatorPermissionSectionProps>) {
  const groupedPermissions = OPERATOR_PERMISSION_OPTIONS.reduce(
    (groups, permission) => {
      const categoryPermissions = groups.get(permission.category) ?? [];
      groups.set(permission.category, [...categoryPermissions, permission]);
      return groups;
    },
    new Map<string, (typeof OPERATOR_PERMISSION_OPTIONS)[number][]>(),
  );

  /** Adds or removes one permission code without mutating the current selection. */
  const handlePermissionChange = (permissionCode: string, checked: boolean) => {
    onChange(
      checked
        ? Array.from(new Set([...selectedPermissions, permissionCode]))
        : selectedPermissions.filter((code) => code !== permissionCode),
    );
  };

  return (
    <FormSection number={4} title="権限">
      <p className="text-sm leading-6 text-muted-foreground">
        運用者が担当する業務に必要な権限を選択してください。
      </p>
      <div className="space-y-5">
        {Array.from(groupedPermissions.entries()).map(([category, permissions]) => (
          <fieldset key={category} className="space-y-3">
            <legend className="text-sm font-bold text-foreground">{category}</legend>
            <div className="grid gap-3 sm:grid-cols-2">
              {permissions.map((permission) => {
                const checked = selectedPermissions.includes(permission.code);
                return (
                  <label
                    key={permission.code}
                    className={cn(
                      'flex min-h-12 cursor-pointer items-center gap-3 rounded-lg border border-border px-4 py-3 text-sm transition-colors',
                      checked && 'border-primary bg-primary/5',
                    )}
                  >
                    <Checkbox
                      checked={checked}
                      onCheckedChange={(value) =>
                        handlePermissionChange(permission.code, value === true)
                      }
                    />
                    <span className="text-foreground">{permission.label}</span>
                  </label>
                );
              })}
            </div>
          </fieldset>
        ))}
      </div>
      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}
    </FormSection>
  );
}
