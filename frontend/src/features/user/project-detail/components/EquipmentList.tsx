import { useState } from 'react';
import type { SiteProjectEquipment } from '@/api/site';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Eye, PencilLine, Wrench } from 'lucide-react';
import {
  equipmentDetailSections,
  equipmentSummaryFields,
  formatEquipmentField,
  type EquipmentDisplayField,
} from '../equipmentDisplay';
import { projectDetailCardClassName } from '../projectDetailStyles';

function EquipmentField({
  equipment,
  field,
}: {
  equipment: SiteProjectEquipment;
  field: EquipmentDisplayField;
}) {
  return (
    <div className='min-w-0'>
      <dt className='text-small text-foreground'>{field.label}</dt>
      <dd className='mt-1 break-words font-medium text-foreground'>
        {formatEquipmentField(equipment, field)}
      </dd>
    </div>
  );
}

function EquipmentDetailDialog({
  equipment,
  open,
  onOpenChange,
}: {
  equipment: SiteProjectEquipment | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className='max-h-[90vh] w-[calc(100vw-2rem)] max-w-4xl overflow-y-auto rounded-xl'>
        <DialogHeader>
          <DialogTitle>設備詳細</DialogTitle>
          <DialogDescription>
            選択した設備の登録情報を表示します。
          </DialogDescription>
        </DialogHeader>

        {equipment ? (
          <div className='space-y-5 pb-2'>
            {equipmentDetailSections.map((section) => {
              const visibleFields = section.fields.filter(
                (field) => !field.condition || field.condition(equipment),
              );

              return (
                <section
                  key={section.title}
                  className='rounded-xl border border-border bg-card p-5'
                >
                  <h3 className='mb-4 text-large text-foreground'>
                    {section.title}
                  </h3>
                  <dl className='grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2 lg:grid-cols-3'>
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
        ) : null}
      </DialogContent>
    </Dialog>
  );
}

function EquipmentSummaryCard({
  equipment,
  index,
  onShowDetails,
  onEdit,
}: {
  equipment: SiteProjectEquipment;
  index: number;
  onShowDetails: (equipment: SiteProjectEquipment) => void;
  onEdit: (equipmentId: number) => void;
}) {
  const equipmentTitle = equipment.equipmentType || `設備 ${index + 1}`;

  return (
    <Card className={projectDetailCardClassName}>
      <CardHeader className='px-6 pb-4 pt-6'>
        <div className='flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between'>
          <div className='flex min-w-0 items-center gap-3'>
            <span className='flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary text-primary'>
              <Wrench className='h-5 w-5' />
            </span>
            <div className='min-w-0'>
              <CardTitle className='break-words text-foreground'>
                {equipmentTitle}
              </CardTitle>
              <CardDescription className='text-table mt-1'>
                設備ID: {equipment.equipmentId ?? '-'}
              </CardDescription>
            </div>
          </div>
          <div className='flex flex-wrap gap-2 self-start sm:self-auto'>
            <Button
              type='button'
              variant='outline'
              size='sm'
              onClick={() => onShowDetails(equipment)}
            >
              <Eye className='h-4 w-4' />
              詳細
            </Button>
            {typeof equipment.equipmentId === 'number' && (
              <Button
                type='button'
                variant='outline'
                size='sm'
                onClick={() => onEdit(equipment.equipmentId as number)}
              >
                <PencilLine className='h-4 w-4' />
                設備変更
              </Button>
            )}
          </div>
        </div>
      </CardHeader>
      <CardContent className='px-6 pb-6'>
        <dl className='grid grid-cols-1 gap-x-8 gap-y-5 sm:grid-cols-2 lg:grid-cols-4'>
          {equipmentSummaryFields.map((field) => (
            <EquipmentField
              key={field.key}
              equipment={equipment}
              field={field}
            />
          ))}
        </dl>
      </CardContent>
    </Card>
  );
}

export function EquipmentList({
  equipments,
  onEditEquipment,
}: {
  equipments: SiteProjectEquipment[];
  onEditEquipment: (equipmentId: number) => void;
}) {
  const [selectedEquipment, setSelectedEquipment] =
    useState<SiteProjectEquipment | null>(null);

  return (
    <>
      <div className='space-y-4'>
        {equipments.map((equipment, index) => (
          <EquipmentSummaryCard
            key={`${equipment.equipmentId ?? equipment.tempId ?? index}`}
            equipment={equipment}
            index={index}
            onShowDetails={setSelectedEquipment}
            onEdit={onEditEquipment}
          />
        ))}
      </div>

      <EquipmentDetailDialog
        equipment={selectedEquipment}
        open={selectedEquipment !== null}
        onOpenChange={(open) => {
          if (!open) setSelectedEquipment(null);
        }}
      />
    </>
  );
}
