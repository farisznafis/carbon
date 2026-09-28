import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import type { ProjectPlanConditionalField } from '@/api/admin/types';
import { ProfileDetailField } from '@/components/data-display/ProfileDetailField';
import { PlanFileValue } from './PlanFileValue';
import { PlanValueTable } from './PlanValueTable';
import {
  appliesUnderRules,
  formatPlanValue,
  isShownFor,
  readPath,
} from './planViewValues';

type PlanPageValuesProps = {
  projectId: string;
  fields: ReadonlyArray<FieldConfig>;
  /** Values keyed the way the field configs name them (`projectOverview.x`). */
  values: Record<string, unknown>;
  conditionalFields?: ReadonlyArray<ProjectPlanConditionalField>;
  labelOverrides?: Readonly<Record<string, string>>;
};

/**
 * Every field of one plan page, read-only, laid out like the other admin
 * project detail pages.
 *
 * Driven by the same field configs as the wizard, so a page gains its view the
 * moment it gains its form.
 */
export function PlanPageValues({
  projectId,
  fields,
  values,
  conditionalFields = [],
  labelOverrides = {},
}: Readonly<PlanPageValuesProps>) {
  const read = (path: string) => readPath(values, path);
  const visible = fields.filter(
    (field) =>
      isShownFor(field, read) &&
      appliesUnderRules(String(field.name), conditionalFields, read),
  );

  return (
    <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
      {visible.map((field) => {
        const name = String(field.name);
        const label = labelOverrides[name] ?? field.label;
        const value = read(name);

        if (field.type === 'table') {
          return (
            <div key={name} className="min-w-0 space-y-2 md:col-span-2">
              <dt className="text-sm font-medium text-muted-foreground">{label}</dt>
              <dd>
                <PlanValueTable
                  projectId={projectId}
                  field={field}
                  rows={value}
                  conditionalFields={conditionalFields}
                />
              </dd>
            </div>
          );
        }

        const isWide = field.type === 'textarea' || field.type === 'file';
        const display =
          field.type === 'file' && typeof value === 'string' && value ? (
            <PlanFileValue projectId={projectId} s3Key={value} />
          ) : (
            <span className="whitespace-pre-wrap">{formatPlanValue(field, value)}</span>
          );

        return (
          <div key={name} className={isWide ? 'md:col-span-2' : undefined}>
            <ProfileDetailField label={label} value={display} />
          </div>
        );
      })}
    </dl>
  );
}
