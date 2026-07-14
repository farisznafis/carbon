import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  decideMonitoringCancelRequest,
  getMonitoringCancelRequests,
  type MonitoringCancelDecision,
  type MonitoringCancelRequestListItem,
} from '@/api/monitoring';
import {
  type Column,
  ProTable,
} from '@/components/data-display/ProTable/ProTable';
import { PaginationControls } from '@/components/data-display/Pagination/PaginationControls';
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/hooks/use-toast';
import { usePagination } from '@/hooks/usePagination';
import { cn } from '@/lib/utils';

const INITIAL_ITEMS_PER_PAGE = 10;

const STATUS_PRESENTATION: Record<
  string,
  { label: string; className: string }
> = {
  PENDING: {
    label: '承認待ち',
    className: 'border-amber-200 bg-amber-50 text-amber-800',
  },
  APPROVED: {
    label: '承認済み',
    className: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  },
  REJECTED: {
    label: '却下',
    className: 'border-red-200 bg-red-50 text-red-800',
  },
};

type CancelRequestTableRow = Omit<MonitoringCancelRequestListItem, 'id'> & {
  id: string;
  cancelRequestId: number;
};

type DecisionContext = {
  request: CancelRequestTableRow;
  decision: MonitoringCancelDecision;
};

function formatDateTime(value: string | null) {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ja-JP', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

function StatusBadge({ status }: { status: string }) {
  const normalizedStatus = status.toUpperCase();
  const presentation = STATUS_PRESENTATION[normalizedStatus];

  return (
    <span
      className={cn(
        'inline-flex rounded-full border px-2.5 py-1 text-xs font-medium',
        presentation?.className ??
          'border-border bg-muted text-muted-foreground',
      )}
    >
      {presentation?.label ?? status}
    </span>
  );
}

export function CancelRequestHistoryTable() {
  const { toast } = useToast();
  const [requests, setRequests] = useState<CancelRequestTableRow[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [decisionContext, setDecisionContext] =
    useState<DecisionContext | null>(null);
  const [managerRemarks, setManagerRemarks] = useState('');
  const [remarksError, setRemarksError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const reloadRequests = useCallback(async () => {
    setIsLoading(true);
    setLoadError(null);

    try {
      const responseItems = await getMonitoringCancelRequests();
      setRequests(
        responseItems.map((request) => ({
          ...request,
          id: String(request.id),
          cancelRequestId: request.id,
        })),
      );
    } catch (error) {
      setLoadError(
        getErrorMessage(error, '取消申請一覧の取得に失敗しました。'),
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void reloadRequests();
  }, [reloadRequests]);

  const {
    currentPage,
    totalPages,
    totalItems,
    paginatedItems,
    goToPage,
    itemsPerPage,
    setItemsPerPage,
  } = usePagination(requests, { itemsPerPage: INITIAL_ITEMS_PER_PAGE });

  const openDecisionDialog = useCallback(
    (request: CancelRequestTableRow, decision: MonitoringCancelDecision) => {
      setDecisionContext({ request, decision });
      setManagerRemarks('');
      setRemarksError(null);
      setSubmitError(null);
    },
    [],
  );

  const handleDialogOpenChange = (open: boolean) => {
    if (open || isSubmitting) return;

    setDecisionContext(null);
    setManagerRemarks('');
    setRemarksError(null);
    setSubmitError(null);
  };

  const handleDecisionSubmit = async () => {
    if (!decisionContext || isSubmitting) return;

    const trimmedRemarks = managerRemarks.trim();
    if (decisionContext.decision === 'REJECTED' && !trimmedRemarks) {
      setRemarksError('却下理由を入力してください。');
      return;
    }

    setRemarksError(null);
    setSubmitError(null);
    setIsSubmitting(true);

    try {
      await decideMonitoringCancelRequest(
        decisionContext.request.cancelRequestId,
        {
          decision: decisionContext.decision,
          ...(trimmedRemarks ? { managerRemarks: trimmedRemarks } : {}),
        },
      );

      const isApproval = decisionContext.decision === 'APPROVE';
      setDecisionContext(null);
      setManagerRemarks('');
      toast({
        title: isApproval ? '取消申請を承認しました' : '取消申請を却下しました',
      });
      await reloadRequests();
    } catch (error) {
      const message = getErrorMessage(
        error,
        '取消申請の処理に失敗しました。',
      );
      setSubmitError(message);
      toast({
        title: '取消申請を処理できませんでした',
        description: message,
        variant: 'destructive',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const columns = useMemo<Column<CancelRequestTableRow>[]>(
    () => [
      {
        key: 'id',
        label: '申請ID',
        sortable: true,
        className: 'whitespace-nowrap',
      },
      {
        key: 'siteName',
        label: '拠点名',
        sortable: true,
        truncate: true,
        maxWidth: 'max-w-[180px]',
      },
      {
        key: 'requestedBy',
        label: '申請者',
        sortable: true,
        truncate: true,
        maxWidth: 'max-w-[180px]',
      },
      {
        key: 'requestedAt',
        label: '申請日時',
        sortable: true,
        className: 'whitespace-nowrap',
        render: (value) => formatDateTime(String(value)),
      },
      {
        key: 'reason',
        label: '取消理由',
        truncate: true,
        maxWidth: 'max-w-[260px]',
      },
      {
        key: 'status',
        label: 'ステータス',
        sortable: true,
        className: 'whitespace-nowrap',
        render: (value) => <StatusBadge status={String(value)} />,
      },
      {
        key: 'decidedAt',
        label: '決定日時',
        sortable: true,
        className: 'whitespace-nowrap',
        render: (value) => formatDateTime(value ? String(value) : null),
      },
      {
        key: 'decidedBy',
        label: '決定者',
        truncate: true,
        maxWidth: 'max-w-[180px]',
        render: (value) => (value ? String(value) : '-'),
      },
      {
        key: 'id',
        id: 'actions',
        label: '操作',
        className: 'whitespace-nowrap text-right',
        render: (_, request) =>
          request.status.toUpperCase() === 'PENDING' ? (
            <div className="flex justify-end gap-2">
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() => openDecisionDialog(request, 'APPROVE')}
              >
                承認
              </Button>
              <Button
                type="button"
                size="sm"
                variant="destructive"
                onClick={() => openDecisionDialog(request, 'REJECTED')}
              >
                却下
              </Button>
            </div>
          ) : (
            '-'
          ),
      },
    ],
    [openDecisionDialog],
  );

  if (isLoading) {
    return (
      <div className="rounded-lg border border-border py-12 text-center text-sm text-muted-foreground">
        取消申請一覧を読み込んでいます...
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="space-y-3 rounded-lg border border-destructive/30 bg-destructive/5 p-6 text-center">
        <p className="text-sm text-destructive">{loadError}</p>
        <Button type="button" variant="outline" onClick={reloadRequests}>
          再試行
        </Button>
      </div>
    );
  }

  const isRejection = decisionContext?.decision === 'REJECTED';

  return (
    <>
      <div className="space-y-4 overflow-x-auto">
        <div className="min-w-[1100px]">
          <ProTable
            data={paginatedItems}
            columns={columns}
            clickable={false}
            height="max-h-[520px]"
            emptyState={
              <p className="text-muted-foreground">取消申請はありません。</p>
            }
          />
        </div>
        <PaginationControls
          currentPage={currentPage}
          totalPages={totalPages}
          totalItems={totalItems}
          itemsPerPage={itemsPerPage}
          onPageChange={goToPage}
          onItemsPerPageChange={setItemsPerPage}
        />
      </div>

      <AlertDialog
        open={Boolean(decisionContext)}
        onOpenChange={handleDialogOpenChange}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              取消申請を{isRejection ? '却下' : '承認'}しますか？
            </AlertDialogTitle>
            <AlertDialogDescription>
              申請ID: {decisionContext?.request.id ?? '-'}
            </AlertDialogDescription>
          </AlertDialogHeader>

          <div className="space-y-2">
            <Label htmlFor="manager-remarks">
              管理者コメント{isRejection ? '（必須）' : '（任意）'}
            </Label>
            <Textarea
              id="manager-remarks"
              value={managerRemarks}
              onChange={(event) => {
                setManagerRemarks(event.target.value);
                if (remarksError) setRemarksError(null);
              }}
              placeholder={
                isRejection
                  ? '却下理由を入力してください'
                  : '必要に応じてコメントを入力してください'
              }
              disabled={isSubmitting}
              aria-invalid={Boolean(remarksError)}
              aria-describedby={remarksError ? 'manager-remarks-error' : undefined}
            />
            {remarksError && (
              <p id="manager-remarks-error" className="text-sm text-destructive">
                {remarksError}
              </p>
            )}
            {submitError && (
              <p className="text-sm text-destructive">{submitError}</p>
            )}
          </div>

          <AlertDialogFooter>
            <AlertDialogCancel disabled={isSubmitting}>
              キャンセル
            </AlertDialogCancel>
            <Button
              type="button"
              variant={isRejection ? 'destructive' : 'default'}
              disabled={isSubmitting}
              onClick={handleDecisionSubmit}
            >
              {isSubmitting
                ? '処理中...'
                : isRejection
                  ? '却下する'
                  : '承認する'}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
