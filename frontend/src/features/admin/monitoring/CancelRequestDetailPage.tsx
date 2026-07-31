import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  decideMonitoringCancelRequest,
  getMonitoringCancelRequests,
  type MonitoringCancelDecision,
  type MonitoringCancelRequestListItem,
} from '@/api/monitoring';
import { PageBackButton } from '@/components/navigation/PageBackButton';
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
import { CancelRequestStatusBadge } from '@/features/admin/monitoring/CancelRequestStatusBadge';
import { formatCancelRequestDateTime } from '@/features/admin/monitoring/cancelRequestFormatters';
import { useToast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';

type DetailFieldProps = {
  label: string;
  children: React.ReactNode;
  fullWidth?: boolean;
};

type CancelRequestLocationState = {
  cancelRequest?: MonitoringCancelRequestListItem;
};

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

function DetailField({ label, children, fullWidth }: DetailFieldProps) {
  return (
    <div className={fullWidth ? 'space-y-2 md:col-span-2' : 'space-y-2'}>
      <dt className="text-small font-bold text-foreground">{label}</dt>
      <dd className="break-words text-body font-bold text-foreground">
        {children}
      </dd>
    </div>
  );
}

export default function CancelRequestDetailPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { cancelRequestId } = useParams();
  const { toast } = useToast();
  const requestId = Number(cancelRequestId);
  const locationRequest = (
    location.state as CancelRequestLocationState | null
  )?.cancelRequest;
  const initialRequest =
    locationRequest?.id === requestId ? locationRequest : null;

  const [cancelRequest, setCancelRequest] =
    useState<MonitoringCancelRequestListItem | null>(initialRequest);
  const [isLoading, setIsLoading] = useState(!initialRequest);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [decision, setDecision] = useState<MonitoringCancelDecision | null>(
    null,
  );
  const [managerRemarks, setManagerRemarks] = useState('');
  const [remarksError, setRemarksError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const returnToList = useCallback(() => {
    navigate(`${paths.adminDashboard}?tab=monitoring`);
  }, [navigate]);

  const loadCancelRequest = useCallback(async () => {
    if (!Number.isInteger(requestId) || requestId <= 0) {
      setCancelRequest(null);
      setLoadError(null);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setLoadError(null);

    try {
      const requests = await getMonitoringCancelRequests();
      setCancelRequest(
        requests.find((request) => request.id === requestId) ?? null,
      );
    } catch (error) {
      setLoadError(
        getErrorMessage(error, '取消申請情報の取得に失敗しました。'),
      );
    } finally {
      setIsLoading(false);
    }
  }, [requestId]);

  useEffect(() => {
    void loadCancelRequest();
  }, [loadCancelRequest]);

  const openDecisionDialog = (nextDecision: MonitoringCancelDecision) => {
    setDecision(nextDecision);
    setManagerRemarks('');
    setRemarksError(null);
    setSubmitError(null);
  };

  const handleDialogOpenChange = (open: boolean) => {
    if (open || isSubmitting) return;

    setDecision(null);
    setManagerRemarks('');
    setRemarksError(null);
    setSubmitError(null);
  };

  const handleDecisionSubmit = async () => {
    if (!cancelRequest || !decision || isSubmitting) return;

    const trimmedRemarks = managerRemarks.trim();
    if (decision === 'REJECTED' && !trimmedRemarks) {
      setRemarksError('却下理由を入力してください。');
      return;
    }

    setRemarksError(null);
    setSubmitError(null);
    setIsSubmitting(true);

    try {
      await decideMonitoringCancelRequest(cancelRequest.id, {
        decision,
        ...(trimmedRemarks ? { managerRemarks: trimmedRemarks } : {}),
      });

      const isApproval = decision === 'APPROVE';
      toast({
        title: isApproval ? '取消申請を承認しました' : '取消申請を却下しました',
        variant: isApproval ? 'success' : 'warning',
      });
      returnToList();
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

  if (isLoading && !cancelRequest) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-6 text-small text-foreground">
        取消申請情報を読み込んでいます...
      </main>
    );
  }

  if (loadError) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-6">
        <div className="space-y-4 text-center">
          <p role="alert" className="text-small text-destructive">
            {loadError}
          </p>
          <div className="flex justify-center gap-3">
            <Button type="button" variant="outline" onClick={returnToList}>
              一覧へ戻る
            </Button>
            <Button type="button" onClick={() => void loadCancelRequest()}>
              再試行
            </Button>
          </div>
        </div>
      </main>
    );
  }

  if (!cancelRequest) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-6">
        <div className="space-y-4 text-center">
          <p className="text-small text-foreground">
            取消申請情報が見つかりませんでした。
          </p>
          <Button type="button" variant="outline" onClick={returnToList}>
            一覧へ戻る
          </Button>
        </div>
      </main>
    );
  }

  const isPending = cancelRequest.status.toUpperCase() === 'PENDING';
  const isRejection = decision === 'REJECTED';

  return (
    <main className="relative min-h-screen bg-background px-4 py-10 sm:px-6">
      <PageBackButton
        onClick={returnToList}
        label=""
        aria-label="取消一覧へ戻る"
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />

      <div className="mx-auto w-full max-w-[960px] space-y-10">
        <h1 className="text-foreground">取消申請内容の詳細</h1>

        <section className="rounded-xl border border-border bg-card p-8 shadow-app-sm md:p-12">
          <dl className="grid grid-cols-1 gap-x-16 gap-y-8 md:grid-cols-2">
            <DetailField label="申請ID">{cancelRequest.id}</DetailField>
            <DetailField label="ステータス">
              <CancelRequestStatusBadge status={cancelRequest.status} />
            </DetailField>
            <DetailField label="拠点名">
              {cancelRequest.siteName || '-'}
            </DetailField>
            <DetailField label="申請者">
              {cancelRequest.requestedBy || '-'}
            </DetailField>
            <DetailField label="申請日時">
              {formatCancelRequestDateTime(cancelRequest.requestedAt)}
            </DetailField>
            <DetailField label="決定日時">
              {formatCancelRequestDateTime(cancelRequest.decidedAt)}
            </DetailField>
            <DetailField label="決定者">
              {cancelRequest.decidedBy || '-'}
            </DetailField>
            <DetailField label="取消理由" fullWidth>
              <span className="whitespace-pre-wrap">
                {cancelRequest.reason || '-'}
              </span>
            </DetailField>
          </dl>

          {isPending && (
            <div className="mt-12 flex justify-end gap-3">
              <Button
                type="button"
                variant="destructive"
                size="md"
                onClick={() => openDecisionDialog('REJECTED')}
              >
                却下
              </Button>
              <Button
                type="button"
                size="md"
                className="bg-success text-success-foreground hover:bg-success/90"
                onClick={() => openDecisionDialog('APPROVE')}
              >
                承認
              </Button>
            </div>
          )}
        </section>
      </div>

      <AlertDialog
        open={Boolean(decision)}
        onOpenChange={handleDialogOpenChange}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              取消申請を{isRejection ? '却下' : '承認'}しますか？
            </AlertDialogTitle>
            <AlertDialogDescription>
              申請ID: {cancelRequest.id}
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
              <p role="alert" className="text-sm text-destructive">
                {submitError}
              </p>
            )}
          </div>

          <AlertDialogFooter>
            <AlertDialogCancel disabled={isSubmitting}>
              キャンセル
            </AlertDialogCancel>
            <Button
              type="button"
              variant={isRejection ? 'destructive' : 'default'}
              className={
                isRejection
                  ? undefined
                  : 'bg-success text-success-foreground hover:bg-success/90'
              }
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
    </main>
  );
}
