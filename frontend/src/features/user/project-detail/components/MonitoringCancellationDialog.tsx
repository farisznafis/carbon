import { useEffect, useState } from 'react';
import {
  createMonitoringCancelRequest,
  getMonitoringRecords,
  type MonitoringRecordListItem,
} from '@/api/monitoring';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import { FormActions } from '@/components/forms/form-layout';
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/hooks/use-toast';

const MONITORING_RECORDS_PAGE_SIZE = 10;

type MonitoringCancellationDialogProps = {
  open: boolean;
  siteId: string;
  projectId: string;
  onOpenChange: (open: boolean) => void;
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

export function MonitoringCancellationDialog({
  open,
  siteId,
  projectId,
  onOpenChange,
}: MonitoringCancellationDialogProps) {
  const { toast } = useToast();
  const [records, setRecords] = useState<MonitoringRecordListItem[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [selectedRecord, setSelectedRecord] =
    useState<MonitoringRecordListItem | null>(null);
  const [reasonText, setReasonText] = useState('');
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submittedRecordKeys, setSubmittedRecordKeys] = useState<Set<string>>(
    () => new Set(),
  );

  useEffect(() => {
    if (!open || !siteId || !projectId) return;

    let ignoreResult = false;

    const loadRecords = async () => {
      try {
        setIsLoading(true);
        setLoadError(null);
        const result = await getMonitoringRecords({
          siteId,
          projectId,
          page: currentPage,
          limit: MONITORING_RECORDS_PAGE_SIZE,
        });

        if (ignoreResult) return;

        setRecords(result.items);
        setTotalItems(result.pagination.totalItems);
        setTotalPages(Math.max(result.pagination.totalPages, 1));
      } catch (error) {
        if (ignoreResult) return;

        setRecords([]);
        setTotalItems(0);
        setTotalPages(1);
        setLoadError(
          error instanceof Error
            ? error.message
            : 'モニタリングデータの取得に失敗しました。',
        );
      } finally {
        if (!ignoreResult) setIsLoading(false);
      }
    };

    void loadRecords();

    return () => {
      ignoreResult = true;
    };
  }, [currentPage, open, projectId, siteId]);

  const closeCancelConfirmation = () => {
    if (isSubmitting) return;
    setSelectedRecord(null);
    setReasonText('');
    setSubmitError(null);
  };

  const handleDialogOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      closeCancelConfirmation();
      setCurrentPage(1);
      setLoadError(null);
    }
    onOpenChange(nextOpen);
  };

  const handleCancelRequest = async () => {
    const trimmedReason = reasonText.trim();
    if (!selectedRecord || !trimmedReason || isSubmitting) return;

    try {
      setIsSubmitting(true);
      setSubmitError(null);
      await createMonitoringCancelRequest({
        siteId: selectedRecord.siteID,
        recordPK: selectedRecord.id,
        recordSK: selectedRecord.timeUpdated,
        reasonText: trimmedReason,
      });

      const submittedRecordKey = getRecordKey(selectedRecord);
      setSubmittedRecordKeys((previousKeys) => {
        const nextKeys = new Set(previousKeys);
        nextKeys.add(submittedRecordKey);
        return nextKeys;
      });
      setSelectedRecord(null);
      setReasonText('');

      toast({
        title: '取消申請を送信しました',
        description: 'モニタリングデータの取消申請を受け付けました。',
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
      <Dialog open={open} onOpenChange={handleDialogOpenChange}>
        <DialogContent className='flex max-h-[90vh] w-[calc(100vw-2rem)] max-w-[1400px] flex-col overflow-hidden rounded-xl'>
          <DialogHeader>
            <DialogTitle>取消一覧</DialogTitle>
            <DialogDescription>
              モニタリングデータを確認し、必要なレコードの取消申請を送信できます。
            </DialogDescription>
          </DialogHeader>

          <div className='min-h-0 flex-1 space-y-4 overflow-y-auto'>
            {isLoading ? (
              <div className='rounded-xl border border-border p-8 text-center text-foreground'>
                読み込み中...
              </div>
            ) : loadError ? (
              <div
                className='rounded-xl border border-destructive/40 p-5 text-destructive'
                role='alert'
              >
                {loadError}
              </div>
            ) : (
              <div className='rounded-xl border border-border'>
                <Table className='min-w-[1180px]'>
                  <TableHeader>
                    <TableRow>
                      <TableHead className='whitespace-nowrap'>記録日</TableHead>
                      <TableHead className='whitespace-nowrap'>方法論</TableHead>
                      <TableHead className='whitespace-nowrap text-right'>
                        制御機器稼働時間
                      </TableHead>
                      <TableHead className='whitespace-nowrap text-right'>
                        発電量
                      </TableHead>
                      <TableHead className='whitespace-nowrap text-right'>
                        自家消費量
                      </TableHead>
                      <TableHead className='whitespace-nowrap text-right'>
                        蓄電池放電量
                      </TableHead>
                      <TableHead className='whitespace-nowrap text-right'>
                        測定精度
                      </TableHead>
                      <TableHead className='whitespace-nowrap'>状態</TableHead>
                      <TableHead className='whitespace-nowrap text-right'>
                        操作
                      </TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {records.length === 0 ? (
                      <TableRow>
                        <TableCell
                          colSpan={9}
                          className='h-28 text-center text-foreground'
                        >
                          モニタリングデータはありません。
                        </TableCell>
                      </TableRow>
                    ) : (
                      records.map((record) => {
                        const recordKey = getRecordKey(record);
                        const hasSubmitted = submittedRecordKeys.has(recordKey);
                        const canCancel =
                          record.isActive &&
                          Boolean(record.id) &&
                          Boolean(record.timeUpdated) &&
                          !hasSubmitted;
                        const batteryValue = record.changedBattery
                          ? record.elpjBatteryValue
                          : 0;

                        return (
                          <TableRow key={recordKey}>
                            <TableCell className='whitespace-nowrap'>
                              {formatDate(record.dateRecorded)}
                            </TableCell>
                            <TableCell className='max-w-40 truncate'>
                              {record.methodologyType || '-'}
                            </TableCell>
                            <TableCell className='whitespace-nowrap text-right'>
                              {formatMetric(record.elpjControlValue, '時間')}
                            </TableCell>
                            <TableCell className='whitespace-nowrap text-right'>
                              {formatMetric(record.elPv, 'kWh')}
                            </TableCell>
                            <TableCell className='whitespace-nowrap text-right'>
                              {formatMetric(record.elPvr, 'kWh')}
                            </TableCell>
                            <TableCell className='whitespace-nowrap text-right'>
                              {formatMetric(batteryValue, 'kWh')}
                            </TableCell>
                            <TableCell className='whitespace-nowrap text-right'>
                              {formatMetric(record.elpjAccuracy, '%')}
                            </TableCell>
                            <TableCell>
                              <Badge
                                variant='outline'
                                className={
                                  record.isActive
                                    ? 'border-primary/40 text-primary'
                                    : 'border-border text-foreground'
                                }
                              >
                                {record.isActive ? '有効' : '取消済み'}
                              </Badge>
                            </TableCell>
                            <TableCell className='text-right'>
                              <Button
                                type='button'
                                variant='outline'
                                size='sm'
                                disabled={!canCancel}
                                onClick={() => {
                                  setSelectedRecord(record);
                                  setReasonText('');
                                  setSubmitError(null);
                                }}
                              >
                                {hasSubmitted
                                  ? '申請済み'
                                  : record.isActive
                                    ? '取消'
                                    : '取消済み'}
                              </Button>
                            </TableCell>
                          </TableRow>
                        );
                      })
                    )}
                  </TableBody>
                </Table>
              </div>
            )}

            {!isLoading && !loadError && (
              <PaginationControls
                currentPage={currentPage}
                totalPages={totalPages}
                totalItems={totalItems}
                onPageChange={setCurrentPage}
                className='pb-1'
              />
            )}
          </div>
        </DialogContent>
      </Dialog>

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
              context="compact"
              className="w-full"
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
