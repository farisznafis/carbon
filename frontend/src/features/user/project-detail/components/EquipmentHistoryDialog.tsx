import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ArrowLeft, ExternalLink, LoaderCircle, X } from 'lucide-react';
import {
  getSiteEquipmentHistory,
  getSiteProjectEquipmentPresignedUrl,
  type SiteEquipmentHistory,
  type SiteProjectEquipmentFile,
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
import { useToast } from '@/hooks/use-toast';
import {
  formatEquipmentFileType,
  getEquipmentFileDisplayName,
} from '@/features/user/equipment/equipmentFileDisplay';
import { EquipmentDetails } from './EquipmentDetails';

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

function openInNewTab(url: string) {
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.target = '_blank';
  anchor.rel = 'noopener noreferrer';
  anchor.click();
}

function HistoryDocuments({ history }: { history: SiteEquipmentHistory }) {
  const { toast } = useToast();
  const [openingFileId, setOpeningFileId] = useState<string | null>(null);
  const files = history.equipment.files ?? [];
  const evidenceDocuments = history.equipment.evidenceDocuments ?? [];

  const showFile = async (file: SiteProjectEquipmentFile) => {
    if (!file.s3Key || openingFileId) return;
    const fileKey = file.fileId || file.s3Key;
    setOpeningFileId(fileKey);

    try {
      openInNewTab(await getSiteProjectEquipmentPresignedUrl(file.s3Key));
    } catch (error) {
      toast({
        title: 'ファイルを表示できませんでした',
        description:
          error instanceof Error
            ? error.message
            : 'ファイルの取得に失敗しました。',
        variant: 'destructive',
      });
    } finally {
      setOpeningFileId(null);
    }
  };

  return (
    <section className="space-y-4 rounded-xl border border-border bg-card p-5">
      <h3 className="text-large text-foreground">確認資料</h3>

      {files.length === 0 && evidenceDocuments.length === 0 ? (
        <p className="text-small text-muted-foreground">登録された資料はありません。</p>
      ) : (
        <div className="space-y-3">
          {files.map((file, index) => {
            const fileKey = file.fileId || file.s3Key || String(index);
            return (
              <div
                key={fileKey}
                className="flex flex-col gap-3 rounded-lg border border-border p-4 sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="min-w-0">
                  <p className="font-medium text-foreground">
                    {formatEquipmentFileType(file.fileType)}
                  </p>
                  <p className="mt-1 break-all text-small text-muted-foreground">
                    {getEquipmentFileDisplayName(file)}
                  </p>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={!file.s3Key || openingFileId !== null}
                  onClick={() => void showFile(file)}
                >
                  {openingFileId === fileKey ? (
                    <LoaderCircle className="animate-spin" />
                  ) : (
                    <ExternalLink />
                  )}
                  表示
                </Button>
              </div>
            );
          })}

          {evidenceDocuments.map((document, index) => (
            <p
              key={`${document}-${index}`}
              className="break-all rounded-lg border border-border p-4 text-small text-foreground"
            >
              {document || '-'}
            </p>
          ))}
        </div>
      )}
    </section>
  );
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
      <Button type="button" variant="ghost" onClick={onBack}>
        <ArrowLeft />
        一覧へ戻る
      </Button>

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
      <HistoryDocuments history={history} />
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
      {
        id: 'actions',
        label: '',
        className: 'text-right',
        render: (history) => (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => setSelectedHistory(history)}
          >
            詳細
          </Button>
        ),
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
              <Button
                type="button"
                variant="outline"
                onClick={() => void loadHistory(true)}
              >
                再試行
              </Button>
            </div>
          ) : (
            <DataGridTable
              rows={histories}
              columns={columns}
              gridTemplateColumns="140px minmax(230px,1.4fr) 100px minmax(130px,1fr) 190px 80px"
              minWidth={940}
              getRowKey={(history) =>
                String(
                  history.id ??
                    `${history.changeDate}-${history.registeredAt}-${history.previousEquipmentId}`,
                )
              }
              emptyMessage="設備変更履歴はありません。"
            />
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
