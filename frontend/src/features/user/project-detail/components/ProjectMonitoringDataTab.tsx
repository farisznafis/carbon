import { useMemo, useState } from 'react';
import { ClipboardList } from 'lucide-react';
import {
  createMonitoringCancelRequest,
  type MonitoringRecordListItem,
} from '@/api/monitoring';
import type { SiteProjectEquipment } from '@/api/site';
import { ActionButton } from '@/components/actions';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { SkeletonCard } from '@/components/feedback/loading';
import { FormActions } from '@/components/forms/form-layout';
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Textarea } from '@/components/ui/textarea';
import {
  useCancelRequestedRecords,
  useMonitoringRecordsQuery,
} from '@/hooks/monitoring/useMonitoringRecordsQuery';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';
import { projectDetailCardClassName } from '../projectDetailStyles';

const MONITORING_RECORDS_PAGE_SIZE = 10;

type ProjectMonitoringDataTabProps = {
  siteId: string;
  projectId: string;
  /** The project's equipment, used to name the equipment each record belongs to. */
  equipments: readonly SiteProjectEquipment[];
};

function formatDate(value: string) {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date);
}

function formatMetric(value: number | null, unit: string) {
  if (value === null || value === undefined || Number.isNaN(value)) return '-';
  return `${new Intl.NumberFormat('ja-JP').format(value)} ${unit}`;
}

function getRecordKey(record: MonitoringRecordListItem) {
  return `${record.id}:${record.timeUpdated}`;
}

/** Names equipment the way the 設備一覧 cards do, so both tabs refer to it alike. */
function getEquipmentLabel(equipment: SiteProjectEquipment) {
  return (
    equipment.deviceNo?.trim() ||
    equipment.pcsModel?.trim() ||
    equipment.pcsManufacturer?.trim() ||
    `設備ID: ${equipment.equipmentId}`
  );
}

/**
 * The project's monitoring records for this site. A record cannot be edited
 * once registered, so a wrong one is withdrawn by sending a 取消申請 for review.
 */
export function ProjectMonitoringDataTab({
  siteId,
  projectId,
  equipments,
}: Readonly<ProjectMonitoringDataTabProps>) {
  const { toast } = useToast();
  const [currentPage, setCurrentPage] = useState(1);
  const recordsQuery = useMonitoringRecordsQuery(
    siteId,
    projectId,
    currentPage,
    MONITORING_RECORDS_PAGE_SIZE,
  );
  const records = recordsQuery.data?.items ?? [];
  const totalItems = recordsQuery.data?.pagination.totalItems ?? 0;
  const totalPages = Math.max(recordsQuery.data?.pagination.totalPages ?? 1, 1);
  const loadError = recordsQuery.isError
    ? recordsQuery.error instanceof Error
      ? recordsQuery.error.message
      : 'モニタリングデータの取得に失敗しました。'
    : null;
  const { requestedKeys, markRequested } = useCancelRequestedRecords(siteId, projectId);
  const [selectedRecord, setSelectedRecord] =
    useState<MonitoringRecordListItem | null>(null);
  const [reasonText, setReasonText] = useState('');
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const equipmentLabels = useMemo(
    () =>
      new Map(
        equipments
          .filter((equipment) => typeof equipment.equipmentId === 'number')
          .map((equipment) => [equipment.equipmentId as number, getEquipmentLabel(equipment)]),
      ),
    [equipments],
  );

  const closeCancelConfirmation = () => {
    if (isSubmitting) return;
    setSelectedRecord(null);
    setReasonText('');
    setSubmitError(null);
  };

  const handleCancelRequest = async () => {
    const trimmedReason = reasonText.trim();
    if (!selectedRecord || !trimmedReason || isSubmitting) return;

    try {
      setIsSubmitting(true);
      setSubmitError(null);
      await createMonitoringCancelRequest({
        siteId: selectedRecord.siteId || siteId,
        recordPK: selectedRecord.id,
        recordSK: selectedRecord.timeUpdated,
        reasonText: trimmedReason,
      });

      markRequested(getRecordKey(selectedRecord));
      setSelectedRecord(null);
      setReasonText('');

      toast({
        title: '取消申請を送信しました',
        description: '運営による確認後、モニタリングデータが取り消されます。',
      });
    } catch (error) {
      setSubmitError(
        error instanceof Error
          ? error.message
          : '取消申請の送信に失敗しました。',
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <section className='flex items-center gap-3'>
        <span className='flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-secondary text-primary'>
          <ClipboardList className='h-5 w-5' aria-hidden='true' />
        </span>
        <div>
          <h2 className='text-xl font-bold text-foreground'>モニタリングデータ</h2>
          <p className='mt-1 max-w-3xl text-body leading-6 text-muted-foreground'>
            この拠点で登録したモニタリングデータです。
            誤りがある場合は、取消申請を送信してください。
          </p>
        </div>
      </section>

      {recordsQuery.isPending ? (
        <SkeletonCard className='min-h-40 rounded-2xl' />
      ) : loadError ? (
        <Card className={projectDetailCardClassName}>
          <CardContent className='flex flex-col items-center gap-3 p-6 text-center'>
            <p className='text-destructive' role='alert'>
              {loadError}
            </p>
            <ActionButton kind='retry' onClick={() => void recordsQuery.refetch()} />
          </CardContent>
        </Card>
      ) : records.length === 0 && currentPage === 1 ? (
        <Card className={projectDetailCardClassName}>
          <CardContent className='p-10 text-center text-muted-foreground'>
            モニタリングデータはまだ登録されていません。
          </CardContent>
        </Card>
      ) : (
        <div className='space-y-4'>
          <Card
            className={cn(
              projectDetailCardClassName,
              'overflow-hidden transition-opacity',
              // Dims the previous page while the next one loads.
              recordsQuery.isPlaceholderData && 'opacity-60',
            )}
            aria-busy={recordsQuery.isPlaceholderData}
          >
            <Table className='min-w-[1040px]'>
              <TableHeader>
                <TableRow>
                  <TableHead className='whitespace-nowrap'>記録日</TableHead>
                  <TableHead className='whitespace-nowrap'>設備</TableHead>
                  <TableHead className='whitespace-nowrap text-right'>発電量</TableHead>
                  <TableHead className='whitespace-nowrap text-right'>自家消費量</TableHead>
                  <TableHead className='whitespace-nowrap text-right'>蓄電池放電量</TableHead>
                  <TableHead className='whitespace-nowrap text-right'>制御機器稼働時間</TableHead>
                  <TableHead className='whitespace-nowrap text-right'>測定精度</TableHead>
                  <TableHead className='whitespace-nowrap'>状態</TableHead>
                  <TableHead className='whitespace-nowrap text-right'>
                    <span className='sr-only'>操作</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {records.map((record) => {
                  const recordKey = getRecordKey(record);
                  const hasSubmitted = requestedKeys.includes(recordKey);
                  const canCancel =
                    record.isActive &&
                    Boolean(record.id) &&
                    Boolean(record.timeUpdated) &&
                    !hasSubmitted;
                  const batteryValue = record.changedBattery
                    ? record.elpjBatteryValue
                    : 0;
                  const equipmentLabel =
                    record.equipmentId === null
                      ? '-'
                      : equipmentLabels.get(record.equipmentId) ??
                        `設備ID: ${record.equipmentId}`;

                  return (
                    <TableRow key={recordKey}>
                      <TableCell className='whitespace-nowrap'>
                        {formatDate(record.dateRecorded)}
                      </TableCell>
                      <TableCell className='max-w-48 truncate' title={equipmentLabel}>
                        {equipmentLabel}
                      </TableCell>
                      <TableCell className='whitespace-nowrap text-right tabular-nums'>
                        {formatMetric(record.elPv, 'kWh')}
                      </TableCell>
                      <TableCell className='whitespace-nowrap text-right tabular-nums'>
                        {formatMetric(record.elPvr, 'kWh')}
                      </TableCell>
                      <TableCell className='whitespace-nowrap text-right tabular-nums'>
                        {formatMetric(batteryValue, 'kWh')}
                      </TableCell>
                      <TableCell className='whitespace-nowrap text-right tabular-nums'>
                        {formatMetric(record.elpjControlValue, '時間')}
                      </TableCell>
                      <TableCell className='whitespace-nowrap text-right tabular-nums'>
                        {formatMetric(record.elpjAccuracy, '%')}
                      </TableCell>
                      <TableCell>
                        {hasSubmitted ? (
                          <StatusBadge tone='warning'>取消申請中</StatusBadge>
                        ) : record.isActive ? (
                          <StatusBadge tone='success'>有効</StatusBadge>
                        ) : (
                          <StatusBadge>取消済み</StatusBadge>
                        )}
                      </TableCell>
                      <TableCell className='text-right'>
                        {canCancel && (
                          <Button
                            type='button'
                            variant='outline'
                            size='sm'
                            aria-label={`${formatDate(record.dateRecorded)}のモニタリングデータを取消申請`}
                            onClick={() => {
                              setSelectedRecord(record);
                              setReasonText('');
                              setSubmitError(null);
                            }}
                          >
                            取消申請
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </Card>

          <PaginationControls
            currentPage={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            onPageChange={setCurrentPage}
          />
        </div>
      )}

      <AlertDialog
        open={selectedRecord !== null}
        onOpenChange={(nextOpen) => {
          if (!nextOpen) closeCancelConfirmation();
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>取消申請を送信しますか？</AlertDialogTitle>
            <AlertDialogDescription>
              対象レコードの記録日: {formatDate(selectedRecord?.dateRecorded ?? '')}
            </AlertDialogDescription>
          </AlertDialogHeader>

          <div className='space-y-2'>
            <label htmlFor='monitoring-cancel-reason' className='text-sm font-medium'>
              理由 <span className='text-destructive'>*</span>
            </label>
            <Textarea
              id='monitoring-cancel-reason'
              value={reasonText}
              onChange={(event) => setReasonText(event.target.value)}
              placeholder='取消理由を入力してください'
              rows={4}
              disabled={isSubmitting}
            />
            {submitError ? (
              <p className='text-sm text-destructive' role='alert'>
                {submitError}
              </p>
            ) : null}
          </div>

          <AlertDialogFooter>
            <FormActions
              context='compact'
              className='w-full'
              disabled={isSubmitting}
              pendingActionId={isSubmitting ? 'confirm' : null}
              actions={[
                {
                  id: 'cancel',
                  label: 'キャンセル',
                  variant: 'outline',
                  onClick: closeCancelConfirmation,
                },
                {
                  id: 'confirm',
                  label: '取消申請を送信',
                  pendingLabel: '送信中...',
                  variant: 'destructive',
                  disabled: !reasonText.trim(),
                  onClick: handleCancelRequest,
                },
              ]}
            />
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
