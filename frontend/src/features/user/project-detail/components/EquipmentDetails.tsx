import type { SiteProjectEquipment } from '@/api/site';
import {
  equipmentDetailSections,
  formatEquipmentField,
  type EquipmentDisplayField,
} from '../equipmentDisplay';

export function EquipmentField({
  equipment,
  field,
}: {
  equipment: SiteProjectEquipment;
  field: EquipmentDisplayField;
}) {
  return (
    <div className="min-w-0">
      <dt className="text-small text-muted-foreground">{field.label}</dt>
      <dd className="mt-1 break-words font-medium text-foreground">
        {formatEquipmentField(equipment, field)}
      </dd>
    </div>
  );
}

export function EquipmentDetails({
  equipment,
}: {
  equipment: SiteProjectEquipment;
}) {
  return (
    <div className="space-y-5 pb-2">
      {equipmentDetailSections.map((section) => {
        const visibleFields = section.fields.filter(
          (field) => !field.condition || field.condition(equipment),
        );

        return (
          <section
            key={section.title}
            className="rounded-xl border border-border bg-card p-5"
          >
            <h3 className="mb-4 text-large text-foreground">
              {section.title}
            </h3>
            <dl className="grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2 lg:grid-cols-3">
              {visibleFields.map((field) => (
                <EquipmentField
                  key={`${section.title}-${field.key}`}
                  equipment={equipment}
                  field={field}
                />
              ))}
            </dl>
          </section>
        );
      })}
    </div>
  );
}
