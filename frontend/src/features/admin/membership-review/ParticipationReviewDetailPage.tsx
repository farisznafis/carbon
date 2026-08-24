import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  approveParticipationReview,
  getAdminProjectDetail,
  getMyParticipationReviews,
  getParticipationReviewDetail,
  participationReviewChecklistFields,
  rejectParticipationReview,
  type ParticipationReviewChecklist,
  type ParticipationReviewChecklistField,
  type ParticipationReviewDetail,
} from '@/api/admin';
import { ApiError } from '@/api/_shared/errors';
import { FormActions, FormPageContent } from '@/components/forms/form-layout';
import {
  floatingPageBackButtonClassName,
  PageBackButton,
} from '@/components/navigation/PageBackButton';
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';
import { ParticipationReviewChecklistCard } from './ParticipationReviewChecklistCard';
import { ParticipationReviewSections } from './ParticipationReviewSections';
import { isParticipationReviewForProject } from './participationReviewProjectMatch';

type Decision = 'approve' | 'reject';

type FieldErrors = {
  reviewerInkan: string | null;
  rejectReason: string | null;
  checklist: string | null;
};

const EMPTY_FIELD_ERRORS: FieldErrors = {
  reviewerInkan: null,
  rejectReason: null,
  checklist: null,
};

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

function checklistFromReview(
  review: ParticipationReviewDetail,
): ParticipationReviewChecklist {
  return participationReviewChecklistFields.reduce<ParticipationReviewChecklist>(
    (checklist, fieldName) => {
      checklist[fieldName] = review.review[fieldName];
      return checklist;
    },
    {} as ParticipationReviewChecklist,
  );
}

function readBackendErrors(error: unknown): FieldErrors {
  if (!(error instanceof ApiError)) return EMPTY_FIELD_ERRORS;
  const errors = error.payload?.errors;
  if (!errors || typeof errors !== 'object' || Array.isArray(errors)) {
    return EMPTY_FIELD_ERRORS;
  }

  const errorEntries = Object.entries(errors).filter(
    (entry): entry is [string, string] => typeof entry[1] === 'string',
  );
  const findError = (fieldName: string) =>
    errorEntries.find(
      ([key]) =>
        key === fieldName ||
        key.endsWith(`.${fieldName}`) ||
        key.endsWith(`::${fieldName}`),
    )?.[1] ?? null;

  return {
    reviewerInkan: findError('reviewerInkan'),
    rejectReason: findError('rejectReason'),
    checklist:
      participationReviewChecklistFields
        .map(findError)
        .find((message): message is string => Boolean(message)) ?? null,
  };
}

export default function ParticipationReviewDetailPage() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const { projectId = '', participationId = '' } = useParams();
  const numericParticipationId = Number(participationId);

  const [review, setReview] = useState<ParticipationReviewDetail | null>(null);
  const [isReviewable, setIsReviewable] = useState(false);
  const [checklist, setChecklist] = useState<ParticipationReviewChecklist | null>(
    null,
  );
  const [reviewerInkan, setReviewerInkan] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [decision, setDecision] = useState<Decision | null>(null);
  const [fieldErrors, setFieldErrors] =
    useState<FieldErrors>(EMPTY_FIELD_ERRORS);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const returnToList = useCallback(() => {
    navigate(paths.adminProjectMembership(projectId));
  }, [navigate, projectId]);

  const loadReview = useCallback(async () => {
    if (
      !projectId ||
      !Number.isInteger(numericParticipationId) ||
      numericParticipationId <= 0
    ) {
      setReview(null);
      setLoadError(null);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setLoadError(null);

    try {
      const [project, detail, myReviews] = await Promise.all([
        getAdminProjectDetail(projectId),
        getParticipationReviewDetail(numericParticipationId),
        getMyParticipationReviews(),
      ]);

      if (!isParticipationReviewForProject(detail, projectId, project)) {
        setReview(null);
        setLoadError(
          'この入会届は指定されたプロジェクトに属していません。',
        );
        return;
      }

      setReview(detail);
      setChecklist(checklistFromReview(detail));
      setIsReviewable(
        myReviews.some(
          (candidate) =>
            candidate.participationId === numericParticipationId &&
            isParticipationReviewForProject(candidate, projectId, project),
        ),
      );
    } catch (error) {
      setLoadError(
        getErrorMessage(error, '入会届の詳細取得に失敗しました。'),
      );
    } finally {
      setIsLoading(false);
    }
  }, [numericParticipationId, projectId]);

  useEffect(() => {
    void loadReview();
  }, [loadReview]);

  const allChecklistItemsChecked = useMemo(
    () =>
      checklist !== null &&
      participationReviewChecklistFields.every(
        (fieldName) => checklist[fieldName],
      ),
    [checklist],
  );
  const hasReviewerInkan = Boolean(reviewerInkan.trim());
  const canApprove =
    isReviewable &&
    hasReviewerInkan &&
    allChecklistItemsChecked &&
    !isSubmitting;
  const canReject = isReviewable && hasReviewerInkan && !isSubmitting;

  const updateChecklist = (
    fieldName: ParticipationReviewChecklistField,
    checked: boolean,
  ) => {
    setChecklist((current) =>
      current ? { ...current, [fieldName]: checked } : current,
    );
    setFieldErrors((current) => ({ ...current, checklist: null }));
    setSubmitError(null);
  };

  const openDecision = (nextDecision: Decision) => {
    setDecision(nextDecision);
    setRejectReason('');
    setFieldErrors(EMPTY_FIELD_ERRORS);
    setSubmitError(null);
  };

  const closeDecision = (open: boolean) => {
    if (open || isSubmitting) return;
    setDecision(null);
    setRejectReason('');
    setFieldErrors(EMPTY_FIELD_ERRORS);
    setSubmitError(null);
  };

  const submitDecision = async () => {
    if (!review || !checklist || !decision || isSubmitting) return;

    const trimmedInkan = reviewerInkan.trim();
    const trimmedRejectReason = rejectReason.trim();

    if (!trimmedInkan) {
      setFieldErrors((current) => ({
        ...current,
        reviewerInkan: 'レビュー担当者印を入力してください。',
      }));
      return;
    }

    if (decision === 'approve' && !allChecklistItemsChecked) {
      setFieldErrors((current) => ({
        ...current,
        checklist: 'すべてのチェック項目を確認してください。',
      }));
      return;
    }

    if (decision === 'reject' && !trimmedRejectReason) {
      setFieldErrors((current) => ({
        ...current,
        rejectReason: '却下理由を入力してください。',
      }));
      return;
    }

    setFieldErrors(EMPTY_FIELD_ERRORS);
    setSubmitError(null);
    setIsSubmitting(true);

    try {
      const commonPayload = {
        reviewerInkan: trimmedInkan,
        ...checklist,
      };

      if (decision === 'approve') {
        await approveParticipationReview(
          review.participationId,
          commonPayload,
        );
      } else {
        await rejectParticipationReview(review.participationId, {
          ...commonPayload,
          rejectReason: trimmedRejectReason,
        });
      }

      toast({
        title:
          decision === 'approve'
            ? '入会届を承認しました'
            : '入会届を却下しました',
        variant: decision === 'approve' ? 'success' : 'warning',
      });
      returnToList();
    } catch (error) {
      const backendErrors = readBackendErrors(error);
      const message = getErrorMessage(
        error,
        decision === 'approve'
          ? '入会届の承認に失敗しました。'
          : '入会届の却下に失敗しました。',
      );
      setFieldErrors(backendErrors);
      setSubmitError(message);
      toast({
        title: '入会届を処理できませんでした',
        description: message,
        variant: 'destructive',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-6 text-small text-foreground">
        入会届の詳細を読み込んでいます...
      </main>
    );
  }

  if (loadError || !review || !checklist) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background px-6">
        <div className="space-y-4 text-center">
          <p
            role={loadError ? 'alert' : undefined}
            className={
              loadError
                ? 'text-small text-destructive'
                : 'text-small text-foreground'
            }
          >
            {loadError || '入会届が見つかりませんでした。'}
          </p>
          <div className="flex justify-center gap-3">
            <Button type="button" variant="outline" onClick={returnToList}>
              一覧へ戻る
            </Button>
            {loadError && (
              <Button type="button" onClick={() => void loadReview()}>
                再試行
              </Button>
            )}
          </div>
        </div>
      </main>
    );
  }

  const isRejection = decision === 'reject';

  return (
    <main className="relative min-h-screen bg-background px-4 py-10 sm:px-6 lg:px-8">
      <PageBackButton
        onClick={returnToList}
        label=""
        aria-label="入会届一覧へ戻る"
        className={floatingPageBackButtonClassName}
      />

      <FormPageContent className="max-w-[960px] space-y-6 pt-16 sm:pt-0">
        <header className="space-y-2">
          <h1>入会届の詳細</h1>
          <p className="text-small text-foreground">
            申請内容と確認資料を確認してください。
          </p>
        </header>

        {!isReviewable && (
          <div
            role="status"
            className="rounded-xl border border-border bg-muted/30 px-5 py-4 text-small text-foreground"
          >
            この入会届は現在あなたのレビュー対象ではないため、閲覧のみ可能です。
          </div>
        )}

        <ParticipationReviewSections review={review} />

        <ParticipationReviewChecklistCard
          checklist={checklist}
          reviewerInkan={reviewerInkan}
          readOnly={!isReviewable}
          reviewerInkanError={fieldErrors.reviewerInkan}
          checklistError={fieldErrors.checklist}
          disabled={isSubmitting}
          onChecklistChange={updateChecklist}
          onReviewerInkanChange={(value) => {
            setReviewerInkan(value);
            setFieldErrors((current) => ({
              ...current,
              reviewerInkan: null,
            }));
            setSubmitError(null);
          }}
        />

        {isReviewable && (
          <FormActions
            actions={[
              {
                id: 'reject',
                label: '却下',
                variant: 'destructive',
                disabled: !canReject,
                onClick: () => openDecision('reject'),
              },
              {
                id: 'approve',
                label: '承認',
                variant: 'success',
                disabled: !canApprove,
                onClick: () => openDecision('approve'),
              },
            ]}
          />
        )}
      </FormPageContent>

      <AlertDialog open={Boolean(decision)} onOpenChange={closeDecision}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              入会届を{isRejection ? '却下' : '承認'}しますか？
            </AlertDialogTitle>
            <AlertDialogDescription>
              入会届ID: {review.participationId}
            </AlertDialogDescription>
          </AlertDialogHeader>

          {isRejection && (
            <div className="space-y-2">
              <Label htmlFor="reject-reason">却下理由（必須）</Label>
              <Textarea
                id="reject-reason"
                value={rejectReason}
                onChange={(event) => {
                  setRejectReason(event.target.value);
                  setFieldErrors((current) => ({
                    ...current,
                    rejectReason: null,
                  }));
                  setSubmitError(null);
                }}
                placeholder="却下理由を入力してください"
                disabled={isSubmitting}
                aria-invalid={Boolean(fieldErrors.rejectReason)}
                aria-describedby={
                  fieldErrors.rejectReason ? 'reject-reason-error' : undefined
                }
              />
              {fieldErrors.rejectReason && (
                <p
                  id="reject-reason-error"
                  role="alert"
                  className="text-small text-destructive"
                >
                  {fieldErrors.rejectReason}
                </p>
              )}
            </div>
          )}

          {submitError && (
            <p role="alert" className="text-small text-destructive">
              {submitError}
            </p>
          )}

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
                  onClick: () => closeDecision(false),
                },
                {
                  id: 'confirm',
                  label: isRejection ? '却下する' : '承認する',
                  pendingLabel: '処理中...',
                  variant: isRejection ? 'destructive' : 'success',
                  disabled: isRejection
                    ? !rejectReason.trim()
                    : !allChecklistItemsChecked,
                  onClick: () => void submitDecision(),
                },
              ]}
            />
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </main>
  );
}
