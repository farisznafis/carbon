import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { LoaderCircle, X } from 'lucide-react';
import {
  getSiteEquipmentHistory,
  type SiteEquipmentHistory,
} from '@/api/site';
import {
  DataGridTable,
  type DataGridTableColumn,
} from '@/components/data-display/DataGridTable';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { EquipmentDetails } from './EquipmentDetails';
import { EquipmentDocuments } from './EquipmentDocuments';
import { ActionButton } from '@/components/actions';

type EquipmentHistoryDialogProps = {
  siteId: string;
  equipmentId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

function formatDate(value: string | null) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date);
}

function formatDateTime(value: string | null) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
}

function compareHistoryDesc(
  first: SiteEquipmentHistory,
  second: SiteEquipmentHistory,
) {
  const firstDate = Date.parse(first.changeDate ?? first.registeredAt ?? '');
  const secondDate = Date.parse(second.changeDate ?? second.registeredAt ?? '');
  const dateDifference =
    (Number.isNaN(secondDate) ? 0 : secondDate) -
    (Number.isNaN(firstDate) ? 0 : firstDate);

  if (dateDifference !== 0) return dateDifference;

  const firstRegisteredAt = Date.parse(first.registeredAt ?? '');
  const secondRegisteredAt = Date.parse(second.registeredAt ?? '');
  return (
    (Number.isNaN(secondRegisteredAt) ? 0 : secondRegisteredAt) -
    (Number.isNaN(firstRegisteredAt) ? 0 : firstRegisteredAt)
  );
}

function HistoryValue({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="min-w-0">
      <dt className="text-small text-foreground">{label}</dt>
      <dd className="mt-1 break-words font-medium text-foreground">
        {children}
      </dd>
    </div>
  );
}

function getHistoryErrorMessage(error: unknown) {
  return error instanceof Error && error.message
    ? error.message
    : '設備変更履歴の取得に失敗しました。';
}

function HistoryDetail({
  history,
  onBack,
}: {
  history: SiteEquipmentHistory;
  onBack: () => void;
}) {
  return (
    <div className="animate-slide-up space-y-5">
      <ActionButton kind="back" onClick={onBack}>
        一覧へ戻る
      </ActionButton>

      <section className="rounded-xl border border-border bg-card p-5">
        <h3 className="mb-4 text-large text-foreground">履歴情報</h3>
        <dl className="grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2 lg:grid-cols-3">
          <HistoryValue label="履歴ID">{history.id ?? '-'}</HistoryValue>
          <HistoryValue label="変更日">
            {formatDate(history.changeDate)}
          </HistoryValue>
          <HistoryValue label="変更前設備ID">
            {history.previousEquipmentId ?? '-'}
          </HistoryValue>
          <HistoryValue label="モニタリング開始日">
            {formatDate(history.monitoringStartDate)}
          </HistoryValue>
          <HistoryValue label="モニタリング終了日">
            {formatDate(history.monitoringEndDate)}
          </HistoryValue>
          <HistoryValue label="状態">
            <StatusBadge tone={history.isActive ? 'success' : 'neutral'}>
              {history.isActive === null
                ? '-'
                : history.isActive
                  ? '有効'
                  : '終了'}
            </StatusBadge>
          </HistoryValue>
          <HistoryValue label="登録者">
            {history.registeredBy || '-'}
          </HistoryValue>
          <HistoryValue label="登録日時">
            {formatDateTime(history.registeredAt)}
          </HistoryValue>
        </dl>
      </section>

      <EquipmentDetails equipment={history.equipment} />
      <EquipmentDocuments equipment={history.equipment} />
    </div>
  );
}

export function EquipmentHistoryDialog({
  siteId,
  equipmentId,
  open,
  onOpenChange,
}: EquipmentHistoryDialogProps) {
  const historyCache = useRef(new Map<number, SiteEquipmentHistory[]>());
  const requestSequence = useRef(0);
  const [histories, setHistories] = useState<SiteEquipmentHistory[]>([]);
  const [selectedHistory, setSelectedHistory] =
    useState<SiteEquipmentHistory | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const loadHistory = useCallback(
    async (force = false) => {
      if (equipmentId === null) return;

      const cachedHistory = historyCache.current.get(equipmentId);
      if (!force && cachedHistory) {
        setHistories(cachedHistory);
        setErrorMessage('');
        return;
      }

      const requestId = ++requestSequence.current;
      setIsLoading(true);
      setErrorMessage('');

      try {
        const response = await getSiteEquipmentHistory(siteId, equipmentId);
        if (requestId !== requestSequence.current) return;
        const sortedHistory = [...response].sort(compareHistoryDesc);
        historyCache.current.set(equipmentId, sortedHistory);
        setHistories(sortedHistory);
      } catch (error) {
        if (requestId !== requestSequence.current) return;
        setHistories([]);
        setErrorMessage(getHistoryErrorMessage(error));
      } finally {
        if (requestId === requestSequence.current) setIsLoading(false);
      }
    },
    [equipmentId, siteId],
  );

  useEffect(() => {
    if (!open || equipmentId === null) return;
    setSelectedHistory(null);
    void loadHistory();
  }, [equipmentId, loadHistory, open]);

  const columns = useMemo<DataGridTableColumn<SiteEquipmentHistory>[]>(
    () => [
      {
        id: 'changeDate',
        label: '変更日',
        sortable: true,
        sortValue: (history) => history.changeDate,
        render: (history) => formatDate(history.changeDate),
      },
      {
        id: 'monitoringPeriod',
        label: 'モニタリング期間',
        render: (history) =>
          `${formatDate(history.monitoringStartDate)} 〜 ${formatDate(history.monitoringEndDate)}`,
      },
      {
        id: 'status',
        label: '状態',
        render: (history) => (
          <StatusBadge tone={history.isActive ? 'success' : 'neutral'}>
            {history.isActive === null
              ? '-'
              : history.isActive
                ? '有効'
                : '終了'}
          </StatusBadge>
        ),
      },
      {
        id: 'registeredBy',
        label: '登録者',
        render: (history) => history.registeredBy || '-',
      },
      {
        id: 'registeredAt',
        label: '登録日時',
        sortable: true,
        sortValue: (history) => history.registeredAt,
        render: (history) => formatDateTime(history.registeredAt),
      },
    ],
    [],
  );

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!nextOpen) setSelectedHistory(null);
        onOpenChange(nextOpen);
      }}
    >
      <DialogContent
        hideCloseButton={selectedHistory !== null}
        className="flex max-h-[90vh] w-[calc(100vw-2rem)] max-w-6xl flex-col overflow-hidden rounded-xl"
      >
        {selectedHistory && (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="absolute right-4 top-4 z-10 h-8 w-8"
            aria-label="設備変更履歴一覧へ戻る"
            onClick={() => setSelectedHistory(null)}
          >
            <X className="h-4 w-4" />
          </Button>
        )}
        <DialogHeader>
          <DialogTitle>
            {selectedHistory ? '設備変更履歴詳細' : '設備変更履歴'}
          </DialogTitle>
          <DialogDescription>
            設備ID: {equipmentId ?? '-'}
          </DialogDescription>
        </DialogHeader>

        <div className="min-h-0 flex-1 overflow-y-auto pr-1">
          {selectedHistory ? (
            <HistoryDetail
              history={selectedHistory}
              onBack={() => setSelectedHistory(null)}
            />
          ) : isLoading ? (
            <div className="flex min-h-48 items-center justify-center gap-2 text-small text-foreground">
              <LoaderCircle className="animate-spin" />
              設備変更履歴を読み込んでいます...
            </div>
          ) : errorMessage ? (
            <div className="space-y-4 rounded-xl border border-destructive/30 bg-destructive/5 p-8 text-center">
              <p role="alert" className="text-small text-destructive">
                {errorMessage}
              </p>
              <ActionButton kind="retry" onClick={() => void loadHistory(true)} />
            </div>
          ) : (
            <DataGridTable
              rows={histories}
              columns={columns}
              gridTemplateColumns="140px minmax(230px,1.4fr) 100px minmax(130px,1fr) 190px"
              minWidth={940}
              getRowKey={(history) =>
                String(
                  history.id ??
                    `${history.changeDate}-${history.registeredAt}-${history.previousEquipmentId}`,
                )
              }
              emptyMessage="設備変更履歴はありません。"
              onRowClick={(history) => setSelectedHistory(history)}
            />
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
