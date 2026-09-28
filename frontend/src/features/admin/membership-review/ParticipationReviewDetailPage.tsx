import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from 'react-router-dom';
import {
  approveParticipationReview,
  downloadAdminParticipationMembershipPdf,
  getAdminProjectDetail,
  getOpenParticipationReviews,
  getParticipationReviewDetail,
  participationReviewChecklistFields,
  rejectParticipationReview,
  type ParticipationReviewChecklist,
  type ParticipationReviewChecklistField,
  type ParticipationReviewDetail,
  type ParticipationReviewStatus,
} from '@/api/admin';
import { ApiError } from '@/api/_shared/errors';
import { FormActions, FormPageContent } from '@/components/forms/form-layout';
import {
  floatingPageBackButtonClassName,
  PageBackButton,
} from '@/components/navigation/PageBackButton';
import { Button } from '@/components/ui/button';
import { useToast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';
import { downloadFromUrl } from '@/utils/download';
import { isForbiddenApiError } from '@/api/_shared/errors';
import { PermissionDeniedNotice } from '@/features/admin/permissions/PermissionDeniedNotice';
import { resolveAdminProjectNavigationState } from '@/features/admin/navigation/adminProjectNavigation';
import {
  ParticipationReviewDecisionDialog,
  type ParticipationReviewDecision,
  type ParticipationReviewDecisionErrors,
} from './ParticipationReviewDecisionDialog';
import { ParticipationReviewPdfCard } from './ParticipationReviewPdfCard';
import { isParticipationReviewApproved } from './participationReviewDisplay';
import { ParticipationReviewSections } from './ParticipationReviewSections';
import { isParticipationReviewForProject } from './participationReviewProjectMatch';

const EMPTY_FIELD_ERRORS: ParticipationReviewDecisionErrors = {
  reviewerName: null,
  reviewerInkan: null,
  rejectReason: null,
  checklist: null,
};

const REVIEW_STATUSES: ParticipationReviewStatus[] = [
  'OPEN',
  'APPROVED',
  'REJECTED',
];

/** Reads a supported membership status without trusting arbitrary query values. */
function readReviewStatus(value: string | null): ParticipationReviewStatus | null {
  return REVIEW_STATUSES.includes(value as ParticipationReviewStatus)
    ? (value as ParticipationReviewStatus)
    : null;
}

/** Returns the read-only notice that corresponds to the membership status. */
function getReadOnlyReviewMessage(status: ParticipationReviewStatus): string {
  if (status === 'APPROVED') {
    return 'この入会届は承認済みです。申請内容とPDFを閲覧できます。';
  }
  if (status === 'REJECTED') {
    return 'この入会届は却下されています。申請内容のみ閲覧できます。';
  }
  return 'この入会届は現在あなたのレビュー対象ではないため、閲覧のみ可能です。';
}

/** Returns an API error message while preserving a readable fallback. */
function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

/** Creates a fresh unchecked checklist for the current reviewer's decision. */
function createEmptyChecklist(): ParticipationReviewChecklist {
  return participationReviewChecklistFields.reduce<ParticipationReviewChecklist>(
    (checklist, fieldName) => {
      checklist[fieldName] = false;
      return checklist;
    },
    {} as ParticipationReviewChecklist,
  );
}

/** Maps backend decision errors to the matching fields inside the dialog. */
function readBackendErrors(
  error: unknown,
): ParticipationReviewDecisionErrors {
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
    reviewerName: findError('reviewerName'),
    reviewerInkan: findError('reviewerInkan'),
    rejectReason: findError('rejectReason'),
    checklist:
      participationReviewChecklistFields
        .map(findError)
        .find((message): message is string => Boolean(message)) ?? null,
  };
}

/** Displays an application review and coordinates its approve or reject decision. */
export default function ParticipationReviewDetailPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { toast } = useToast();
  const { projectId = '', participationId = '' } = useParams();
  const [searchParams] = useSearchParams();
  const navigationState = resolveAdminProjectNavigationState(location.state);
  const requestedStatus = readReviewStatus(searchParams.get('status'));
  const numericParticipationId = Number(participationId);

  const [review, setReview] = useState<ParticipationReviewDetail | null>(null);
  const [isReviewable, setIsReviewable] = useState(false);
  const [checklist, setChecklist] = useState<ParticipationReviewChecklist | null>(
    null,
  );
  const [reviewerName, setReviewerName] = useState('');
  const [reviewerInkan, setReviewerInkan] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [decision, setDecision] =
    useState<ParticipationReviewDecision | null>(null);
  const [fieldErrors, setFieldErrors] =
    useState<ParticipationReviewDecisionErrors>(EMPTY_FIELD_ERRORS);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [hasPermissionError, setHasPermissionError] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);

  const returnToList = useCallback(() => {
    navigate(paths.adminProjectMembership(projectId, requestedStatus ?? 'OPEN'), {
      state: navigationState,
    });
  }, [navigate, navigationState, projectId, requestedStatus]);

  /** Fetches a fresh membership PDF URL and opens it without leaving the review page. */
  const handleViewMembershipPdf = async () => {
    if (!review || isDownloadingPdf) return;

    setIsDownloadingPdf(true);
    try {
      const { pdfUrl } = await downloadAdminParticipationMembershipPdf(
        projectId,
        review.participationId,
      );
      downloadFromUrl(pdfUrl);
    } catch (error) {
      toast({
        title: 'PDFを表示できませんでした',
        description:
          error instanceof Error && error.message
            ? error.message
            : 'しばらくしてからもう一度お試しください。',
        variant: 'destructive',
      });
    } finally {
      setIsDownloadingPdf(false);
    }
  };

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
    setHasPermissionError(false);

    try {
      const openReviewsRequest =
        requestedStatus === 'APPROVED' || requestedStatus === 'REJECTED'
          ? Promise.resolve([])
          : getOpenParticipationReviews();
      const [project, detail, openReviews] = await Promise.all([
        getAdminProjectDetail(projectId),
        getParticipationReviewDetail(numericParticipationId),
        openReviewsRequest,
      ]);

      if (!isParticipationReviewForProject(detail, projectId, project)) {
        setReview(null);
        setLoadError(
          'この入会届は指定されたプロジェクトに属していません。',
        );
        return;
      }

      const isCurrentUserReviewable = openReviews.some(
        (candidate) =>
          candidate.participationId === numericParticipationId &&
          isParticipationReviewForProject(candidate, projectId, project),
      );

      setReview(detail);
      setIsReviewable(
        (requestedStatus === null || requestedStatus === 'OPEN') &&
          isCurrentUserReviewable,
      );
      setChecklist(createEmptyChecklist());
      setReviewerName('');
      setReviewerInkan('');
      setRejectReason('');
      setDecision(null);
      setFieldErrors(EMPTY_FIELD_ERRORS);
      setSubmitError(null);
    } catch (error) {
      setHasPermissionError(isForbiddenApiError(error));
      setLoadError(
        getErrorMessage(error, '入会届の詳細取得に失敗しました。'),
      );
    } finally {
      setIsLoading(false);
    }
  }, [numericParticipationId, projectId, requestedStatus]);

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
  const hasReviewerName = Boolean(reviewerName.trim());
  const hasReviewerInkan = Boolean(reviewerInkan.trim());
  const canSubmitDecision =
    hasReviewerName &&
    hasReviewerInkan &&
    allChecklistItemsChecked &&
    (decision !== 'reject' || Boolean(rejectReason.trim())) &&
    !isSubmitting;

  /** Updates one checklist item and clears errors that it resolves. */
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

  /** Opens the selected decision form while retaining the current review draft. */
  const openDecision = (nextDecision: ParticipationReviewDecision) => {
    setDecision(nextDecision);
    setFieldErrors(EMPTY_FIELD_ERRORS);
    setSubmitError(null);
  };

  /** Closes the dialog without discarding fields entered for this participation. */
  const closeDecision = (open: boolean) => {
    if (open || isSubmitting) return;
    setDecision(null);
    setFieldErrors(EMPTY_FIELD_ERRORS);
    setSubmitError(null);
  };

  /** Sends the validated checklist to the selected approve or reject endpoint. */
  const submitDecision = async () => {
    if (!review || !checklist || !decision || isSubmitting) return;

    const trimmedReviewerName = reviewerName.trim();
    const trimmedInkan = reviewerInkan.trim();
    const trimmedRejectReason = rejectReason.trim();

    if (!trimmedReviewerName) {
      setFieldErrors((current) => ({
        ...current,
        reviewerName: 'レビュー担当者名を入力してください。',
      }));
      return;
    }

    if (!trimmedInkan) {
      setFieldErrors((current) => ({
        ...current,
        reviewerInkan: 'レビュー担当者印を入力してください。',
      }));
      return;
    }

    if (!allChecklistItemsChecked) {
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
        reviewerName: trimmedReviewerName,
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

  if (hasPermissionError) {
    return (
      <main className="mx-auto w-full max-w-[960px] space-y-6 px-4 py-10 sm:px-6">
        <header><h1>入会届の詳細</h1><p className="mt-2 text-small text-foreground">申請内容と確認資料を確認します。</p></header>
        <PermissionDeniedNotice permission="PARTICIPATING_COMPANY_APPLICATION_REVIEW" />
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

  const reviewStatus =
    requestedStatus ??
    (isParticipationReviewApproved(review) ? 'APPROVED' : 'OPEN');

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
            {getReadOnlyReviewMessage(reviewStatus)}
          </div>
        )}

        {reviewStatus === 'APPROVED' && (
          <ParticipationReviewPdfCard
            isLoading={isDownloadingPdf}
            onView={() => void handleViewMembershipPdf()}
          />
        )}

        <ParticipationReviewSections review={review} />

        {isReviewable && (
          <FormActions
            actions={[
              {
                id: 'reject',
                label: '却下',
                variant: 'destructive',
                disabled: isSubmitting,
                onClick: () => openDecision('reject'),
              },
              {
                id: 'approve',
                label: '承認',
                variant: 'success',
                disabled: isSubmitting,
                onClick: () => openDecision('approve'),
              },
            ]}
          />
        )}
      </FormPageContent>

      {isReviewable && (
        <ParticipationReviewDecisionDialog
          open={Boolean(decision)}
          decision={decision}
          review={review}
          checklist={checklist}
          reviewerName={reviewerName}
          reviewerInkan={reviewerInkan}
          rejectReason={rejectReason}
          fieldErrors={fieldErrors}
          submitError={submitError}
          isSubmitting={isSubmitting}
          canSubmit={canSubmitDecision}
          onOpenChange={closeDecision}
          onChecklistChange={updateChecklist}
          onReviewerNameChange={(value) => {
            setReviewerName(value);
            setFieldErrors((current) => ({
              ...current,
              reviewerName: null,
            }));
            setSubmitError(null);
          }}
          onReviewerInkanChange={(value) => {
            setReviewerInkan(value);
            setFieldErrors((current) => ({
              ...current,
              reviewerInkan: null,
            }));
            setSubmitError(null);
          }}
          onRejectReasonChange={(value) => {
            setRejectReason(value);
            setFieldErrors((current) => ({
              ...current,
              rejectReason: null,
            }));
            setSubmitError(null);
          }}
          onSubmit={() => void submitDecision()}
        />
      )}
    </main>
  );
}
