import { useState } from 'react';
import type { SiteProjectEquipment } from '@/api/site';
import { RowActionButton } from '@/components/data-display/RowActionButton';
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
import { Eye, History, PencilLine, Wrench } from 'lucide-react';
import {
  equipmentSummaryFields,
} from '../equipmentDisplay';
import { projectDetailCardClassName } from '../projectDetailStyles';
import { EquipmentDetails, EquipmentField } from './EquipmentDetails';
import { EquipmentChangeStartDialog } from './EquipmentChangeStartDialog';
import { EquipmentDocuments } from './EquipmentDocuments';
import { EquipmentHistoryDialog } from './EquipmentHistoryDialog';

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
      <DialogContent className='max-h-[90vh] w-[calc(100vw-2rem)] max-w-4xl overflow-y-auto rounded-lg'>
        <DialogHeader>
          <DialogTitle>設備詳細</DialogTitle>
          <DialogDescription>
            選択した設備の登録情報を表示します。
          </DialogDescription>
        </DialogHeader>

        {equipment ? (
          <div className='space-y-5'>
            <EquipmentDetails equipment={equipment} />
            <EquipmentDocuments equipment={equipment} />
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
  onShowHistory,
  onStartChange,
}: {
  equipment: SiteProjectEquipment;
  index: number;
  onShowDetails: (equipment: SiteProjectEquipment) => void;
  onShowHistory: (equipmentId: number) => void;
  onStartChange: (equipment: SiteProjectEquipment) => void;
}) {
  const deviceNo = equipment.deviceNo?.trim();
  const pcsManufacturer = equipment.pcsManufacturer?.trim();
  const pcsModel = equipment.pcsModel?.trim();
  const pcsIdentity = [pcsManufacturer, pcsModel].filter(Boolean).join(' / ');
  const equipmentTitle =
    deviceNo || pcsModel || pcsManufacturer || `設備 ${index + 1}`;
  const equipmentDescription = deviceNo ? pcsIdentity : '';

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
              {equipmentDescription && (
                <CardDescription className='text-table mt-1 break-words'>
                  {equipmentDescription}
                </CardDescription>
              )}
            </div>
          </div>
          <div className='flex flex-wrap gap-2 self-start sm:self-auto'>
            {/* Keeps its icon so it matches the icon+label actions beside it. */}
            <RowActionButton onClick={() => onShowDetails(equipment)}>
              <Eye className='h-4 w-4' />
              詳細
            </RowActionButton>
            {typeof equipment.equipmentId === 'number' && (
              <>
                <Button
                  type='button'
                  variant='outline'
                  size='sm'
                  onClick={() =>
                    onShowHistory(equipment.equipmentId as number)
                  }
                >
                  <History className='h-4 w-4' />
                  変更履歴
                </Button>
                <Button
                  type='button'
                  variant='outline'
                  size='sm'
                  onClick={() => onStartChange(equipment)}
                >
                  <PencilLine className='h-4 w-4' />
                  設備を変更する
                </Button>
              </>
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
  siteId,
  equipments,
  onEditEquipment,
}: {
  siteId: string;
  equipments: SiteProjectEquipment[];
  onEditEquipment: (equipmentId: number) => void;
}) {
  const [selectedEquipment, setSelectedEquipment] =
    useState<SiteProjectEquipment | null>(null);
  const [historyEquipmentId, setHistoryEquipmentId] = useState<number | null>(
    null,
  );
  const [equipmentToChange, setEquipmentToChange] =
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
            onShowHistory={setHistoryEquipmentId}
            onStartChange={setEquipmentToChange}
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

      <EquipmentHistoryDialog
        siteId={siteId}
        equipmentId={historyEquipmentId}
        open={historyEquipmentId !== null}
        onOpenChange={(open) => {
          if (!open) setHistoryEquipmentId(null);
        }}
      />

      <EquipmentChangeStartDialog
        equipment={equipmentToChange}
        onOpenChange={(open) => {
          if (!open) setEquipmentToChange(null);
        }}
        onConfirm={onEditEquipment}
      />
    </>
  );
}
