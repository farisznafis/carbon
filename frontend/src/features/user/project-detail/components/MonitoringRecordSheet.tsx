import type { ReactNode } from 'react';
import type { MonitoringRecordListItem } from '@/api/monitoring';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet';
import {
  canRequestCancel,
  formatNumber,
  formatRecordDate,
  formatRecordDateTime,
  type MonitoringRecordStatus,
} from './monitoringRecordDisplay';

export function MonitoringRecordStatusBadge({ status }: Readonly<{ status: MonitoringRecordStatus }>) {
  if (status === 'requested') return <StatusBadge tone='warning'>取消申請中</StatusBadge>;
  if (status === 'cancelled') return <StatusBadge>取消済み</StatusBadge>;
  return <StatusBadge tone='success'>有効</StatusBadge>;
}

function DetailSection({ title, children }: Readonly<{ title: string; children: ReactNode }>) {
  return (
    <section className='space-y-3'>
      <h3 className='text-sm font-bold text-foreground'>{title}</h3>
      <dl className='divide-y divide-border rounded-lg border border-border'>{children}</dl>
    </section>
  );
}

function DetailRow({ label, children }: Readonly<{ label: string; children: ReactNode }>) {
  return (
    <div className='flex items-center justify-between gap-4 px-4 py-3'>
      <dt className='text-small text-muted-foreground'>{label}</dt>
      <dd className='text-right text-sm font-medium tabular-nums text-foreground'>{children}</dd>
    </div>
  );
}

/** A number with its unit beside it, or a dash alone when there is no value. */
function withUnit(value: number | null, unit: string) {
  const formatted = formatNumber(value);
  return formatted === '-' ? formatted : `${formatted} ${unit}`;
}

type MonitoringRecordSheetProps = {
  record: MonitoringRecordListItem | null;
  status: MonitoringRecordStatus;
  onOpenChange: (open: boolean) => void;
  onRequestCancel: (record: MonitoringRecordListItem) => void;
};

/**
 * Everything one record holds, beside the list so it stays in view. The
 * 取消申請 lives here so a record is looked at before it is withdrawn.
 */
export function MonitoringRecordSheet({
  record,
  status,
  onOpenChange,
  onRequestCancel,
}: Readonly<MonitoringRecordSheetProps>) {
  return (
    <Sheet open={record !== null} onOpenChange={onOpenChange}>
      <SheetContent className='flex w-full flex-col gap-0 p-0 sm:max-w-md'>
        {record && (
          <>
            <SheetHeader className='space-y-2 border-b border-border px-6 py-5 text-left'>
              <SheetTitle>{formatRecordDate(record.dateRecorded)} のモニタリングデータ</SheetTitle>
              <SheetDescription asChild>
                <div>
                  <MonitoringRecordStatusBadge status={status} />
                </div>
              </SheetDescription>
            </SheetHeader>

            <div className='flex-1 space-y-6 overflow-y-auto px-6 py-5'>
              <DetailSection title='電力量'>
                <DetailRow label='発電量'>{withUnit(record.elPv, 'kWh')}</DetailRow>
                <DetailRow label='自家消費量'>{withUnit(record.elPvr, 'kWh')}</DetailRow>
                <DetailRow label='蓄電池の交換'>{record.changedBattery ? 'あり' : 'なし'}</DetailRow>
                {record.changedBattery && (
                  <DetailRow label='蓄電池放電量'>{withUnit(record.elpjBatteryValue, 'kWh')}</DetailRow>
                )}
              </DetailSection>

              <DetailSection title='制御機器'>
                <DetailRow label='稼働時間'>{withUnit(record.elpjControlValue, '時間')}</DetailRow>
                <DetailRow label='測定精度'>{withUnit(record.elpjAccuracy, '%')}</DetailRow>
              </DetailSection>

              <DetailSection title='登録情報'>
                <DetailRow label='登録日時'>{formatRecordDateTime(record.timeUpdated)}</DetailRow>
                <DetailRow label='集計'>
                  {record.processedAt
                    ? `${formatRecordDateTime(record.processedAt, false)} 集計済み`
                    : '未集計'}
                </DetailRow>
                <DetailRow label='方法論'>{record.methodologyType || '-'}</DetailRow>
              </DetailSection>
            </div>

            <SheetFooter className='border-t border-border px-6 py-4 sm:flex-col sm:space-x-0'>
              {canRequestCancel(record, status) ? (
                <Button
                  type='button'
                  variant='outline'
                  className='w-full border-destructive/40 text-destructive hover:bg-destructive/5 hover:text-destructive'
                  onClick={() => onRequestCancel(record)}
                >
                  取消申請
                </Button>
              ) : (
                <p className='text-small text-muted-foreground'>
                  {status === 'requested'
                    ? '取消申請を送信済みです。運営の確認後に取り消されます。'
                    : 'このモニタリングデータは取り消されています。'}
                </p>
              )}
            </SheetFooter>
          </>
        )}
      </SheetContent>
    </Sheet>
  );
}
