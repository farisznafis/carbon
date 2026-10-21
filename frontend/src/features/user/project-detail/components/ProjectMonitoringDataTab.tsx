import { useMemo, useState } from 'react';
import { ClipboardList } from 'lucide-react';
import {
  createMonitoringCancelRequest,
  type MonitoringRecordListItem,
} from '@/api/monitoring';
import { ActionButton } from '@/components/actions';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
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
import { Card, CardContent } from '@/components/ui/card';
import { Textarea } from '@/components/ui/textarea';
import {
  useCancelRequestedRecords,
  useMonitoringRecordsQuery,
} from '@/hooks/monitoring/useMonitoringRecordsQuery';
import { useToast } from '@/hooks/use-toast';
import { cn } from '@/lib/utils';
import { projectDetailCardClassName } from '../projectDetailStyles';
import {
  MonitoringRecordSheet,
  MonitoringRecordStatusBadge,
} from './MonitoringRecordSheet';
import {
  formatNumber,
  formatRecordDate,
  getRecordKey,
  getRecordStatus,
} from './monitoringRecordDisplay';

const MONITORING_RECORDS_PAGE_SIZE = 10;
const RECORD_GRID_COLUMNS =
  'minmax(110px, 0.8fr) minmax(110px, 1fr) minmax(110px, 1fr) minmax(150px, 1fr)';

type ProjectMonitoringDataTabProps = {
  siteId: string;
  projectId: string;
};

/**
 * The project's monitoring records for this site. The list keeps to what tells
 * records apart; the rest, and the 取消申請, open in a side panel per record.
 */
export function ProjectMonitoringDataTab({
  siteId,
  projectId,
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
  const [viewedRecord, setViewedRecord] = useState<MonitoringRecordListItem | null>(null);
  const [cancelTarget, setCancelTarget] = useState<MonitoringRecordListItem | null>(null);
  const [reasonText, setReasonText] = useState('');
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const columns = useMemo<DataGridTableColumn<MonitoringRecordListItem>[]>(
    () => [
      {
        id: 'dateRecorded',
        label: '記録日',
        render: (record) => (
          <span className='font-bold tabular-nums text-foreground'>
            {formatRecordDate(record.dateRecorded)}
          </span>
        ),
      },
      {
        id: 'elPv',
        label: '発電量 (kWh)',
        render: (record) => (
          <span className='tabular-nums text-foreground'>
            {formatNumber(record.elPv)}
          </span>
        ),
      },
      {
        id: 'elPvr',
        label: '自家消費量 (kWh)',
        render: (record) => (
          <span className='tabular-nums text-foreground'>
            {formatNumber(record.elPvr)}
          </span>
        ),
      },
      {
        id: 'status',
        label: '状態',
        render: (record) => (
          <span className='flex flex-wrap items-center gap-1.5'>
            <MonitoringRecordStatusBadge status={getRecordStatus(record, requestedKeys)} />
            {record.processedAt && <StatusBadge>集計済み</StatusBadge>}
          </span>
        ),
      },
    ],
    [requestedKeys],
  );

  const closeCancelConfirmation = () => {
    if (isSubmitting) return;
    setCancelTarget(null);
    setReasonText('');
    setSubmitError(null);
  };

  const handleCancelRequest = async () => {
    const trimmedReason = reasonText.trim();
    if (!cancelTarget || !trimmedReason || isSubmitting) return;

    try {
      setIsSubmitting(true);
      setSubmitError(null);
      await createMonitoringCancelRequest({
        siteId: cancelTarget.siteId || siteId,
        recordPK: cancelTarget.id,
        recordSK: cancelTarget.timeUpdated,
        reasonText: trimmedReason,
      });

      markRequested(getRecordKey(cancelTarget));
      setCancelTarget(null);
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
        <span className='flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-secondary text-primary-strong'>
          <ClipboardList className='h-5 w-5' aria-hidden='true' />
        </span>
        <div>
          <h2 className='text-xl font-bold text-foreground'>モニタリングデータ</h2>
          <p className='mt-1 max-w-3xl text-body leading-6 text-muted-foreground'>
            この拠点で登録したモニタリングデータです。
            行を選ぶと詳細を確認でき、誤りがある場合は取消申請を送信できます。
          </p>
        </div>
      </section>

      {recordsQuery.isPending ? (
        <SkeletonCard className='min-h-40 rounded-lg' />
      ) : loadError ? (
        <Card className={projectDetailCardClassName}>
          <CardContent className='flex flex-col items-center gap-3 p-6 text-center'>
            <p className='text-destructive-strong' role='alert'>
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
          <div
            className={cn(
              'transition-opacity',
              // Dims the previous page while the next one loads.
              recordsQuery.isPlaceholderData && 'opacity-60',
            )}
            aria-busy={recordsQuery.isPlaceholderData}
          >
            <DataGridTable
              rows={records}
              columns={columns}
              gridTemplateColumns={RECORD_GRID_COLUMNS}
              minWidth={560}
              getRowKey={getRecordKey}
              emptyMessage='このページにモニタリングデータはありません。'
              onRowClick={(record) => setViewedRecord(record)}
              getRowActionLabel={(record) =>
                `${formatRecordDate(record.dateRecorded)}のモニタリングデータの詳細を開く`
              }
            />
          </div>

          <PaginationControls
            currentPage={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            onPageChange={setCurrentPage}
          />
        </div>
      )}

      <MonitoringRecordSheet
        record={viewedRecord}
        status={viewedRecord ? getRecordStatus(viewedRecord, requestedKeys) : 'active'}
        onOpenChange={(open) => {
          if (!open) setViewedRecord(null);
        }}
        onRequestCancel={(record) => {
          setCancelTarget(record);
          setReasonText('');
          setSubmitError(null);
        }}
      />

      <AlertDialog
        open={cancelTarget !== null}
        onOpenChange={(nextOpen) => {
          if (!nextOpen) closeCancelConfirmation();
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>取消申請を送信しますか？</AlertDialogTitle>
            <AlertDialogDescription>
              対象レコードの記録日: {formatRecordDate(cancelTarget?.dateRecorded ?? '')}
            </AlertDialogDescription>
          </AlertDialogHeader>

          <div className='space-y-2'>
            <label htmlFor='monitoring-cancel-reason' className='text-sm font-medium'>
              理由 <span className='text-destructive-strong'>*</span>
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
              <p className='text-sm text-destructive-strong' role='alert'>
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
