import type {
  ParticipationReviewChecklist,
  ParticipationReviewChecklistField,
  ParticipationReviewDetail,
} from '@/api/admin';
import { participationReviewChecklistFields } from '@/api/admin';
import { FormActions } from '@/components/forms/form-layout';
import { RequiredBadge } from '@/components/forms/shared/RequiredLabel';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { participationReviewChecklistLabels } from './participationReviewDisplay';

export type ParticipationReviewDecision = 'approve' | 'reject';

export type ParticipationReviewDecisionErrors = {
  reviewerInkan: string | null;
  rejectReason: string | null;
  checklist: string | null;
};

type ParticipationReviewDecisionDialogProps = {
  open: boolean;
  decision: ParticipationReviewDecision | null;
  review: ParticipationReviewDetail;
  checklist: ParticipationReviewChecklist;
  reviewerInkan: string;
  rejectReason: string;
  fieldErrors: ParticipationReviewDecisionErrors;
  submitError: string | null;
  isSubmitting: boolean;
  canSubmit: boolean;
  onOpenChange: (open: boolean) => void;
  onChecklistChange: (
    fieldName: ParticipationReviewChecklistField,
    checked: boolean,
  ) => void;
  onReviewerInkanChange: (value: string) => void;
  onRejectReasonChange: (value: string) => void;
  onSubmit: () => void;
};

/**
 * Displays the complete approval or rejection form without interrupting review-detail reading.
 */
export function ParticipationReviewDecisionDialog({
  open,
  decision,
  review,
  checklist,
  reviewerInkan,
  rejectReason,
  fieldErrors,
  submitError,
  isSubmitting,
  canSubmit,
  onOpenChange,
  onChecklistChange,
  onReviewerInkanChange,
  onRejectReasonChange,
  onSubmit,
}: ParticipationReviewDecisionDialogProps) {
  const isRejection = decision === 'reject';
  const actionLabel = isRejection ? '却下する' : '承認する';

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[calc(100dvh-2rem)] w-[calc(100vw-2rem)] max-w-4xl flex-col gap-0 overflow-hidden rounded-xl p-0">
        <DialogHeader className="shrink-0 border-b border-border px-6 py-5 pr-14 sm:px-8 sm:py-6 sm:pr-14">
          <DialogTitle className="text-foreground">
            入会届を{isRejection ? '却下' : '承認'}しますか？
          </DialogTitle>
          <DialogDescription>
            すべての項目を確認し、レビュー担当者印を入力してください。
          </DialogDescription>
        </DialogHeader>

        <div className="min-h-0 flex-1 space-y-6 overflow-y-auto px-6 py-5 sm:px-8 sm:py-6">
          <section
            aria-label="対象申請"
            className="grid gap-4 rounded-xl border border-border bg-muted/20 p-4 sm:grid-cols-3"
          >
            <div className="min-w-0 space-y-1">
              <p className="text-small text-muted-foreground">会社名</p>
              <p className="break-words text-body font-bold text-foreground">
                {review.companyName || '―'}
              </p>
            </div>
            <div className="min-w-0 space-y-1">
              <p className="text-small text-muted-foreground">拠点名</p>
              <p className="break-words text-body font-bold text-foreground">
                {review.siteName || '―'}
              </p>
            </div>
            <div className="min-w-0 space-y-1">
              <p className="text-small text-muted-foreground">プロジェクト名</p>
              <p className="break-words text-body font-bold text-foreground">
                {review.projectName || '―'}
              </p>
            </div>
          </section>

          <section className="space-y-4" aria-labelledby="review-checklist-title">
            <div className="space-y-1.5">
              <h2
                id="review-checklist-title"
                className="text-large font-bold text-foreground"
              >
                レビューチェックリスト
              </h2>
              <p className="text-small text-foreground">
                処理を行うには、すべての項目を確認してください。
              </p>
            </div>

            <div className="grid gap-3 md:grid-cols-2">
              {participationReviewChecklistFields.map((fieldName) => {
                const checkboxId = `decision-review-checklist-${fieldName}`;

                return (
                  <div
                    key={fieldName}
                    className="flex items-start gap-3 rounded-lg border border-border bg-muted/10 p-4"
                  >
                    <Checkbox
                      id={checkboxId}
                      checked={checklist[fieldName]}
                      disabled={isSubmitting}
                      onCheckedChange={(checked) =>
                        onChecklistChange(fieldName, checked === true)
                      }
                      aria-label={participationReviewChecklistLabels[fieldName]}
                    />
                    <Label
                      htmlFor={checkboxId}
                      className="cursor-pointer text-small leading-5 text-foreground"
                    >
                      {participationReviewChecklistLabels[fieldName]}
                    </Label>
                  </div>
                );
              })}
            </div>

            {fieldErrors.checklist && (
              <p role="alert" className="text-small text-destructive">
                {fieldErrors.checklist}
              </p>
            )}
          </section>

          <section className="space-y-5" aria-label="レビュー情報">
            <div className="space-y-2">
              <Label
                htmlFor="reviewer-inkan"
                className="inline-flex items-center gap-1"
              >
                レビュー担当者印
                <RequiredBadge />
              </Label>
              <Input
                id="reviewer-inkan"
                value={reviewerInkan}
                onChange={(event) => onReviewerInkanChange(event.target.value)}
                placeholder="レビュー担当者印を入力"
                disabled={isSubmitting}
                aria-invalid={Boolean(fieldErrors.reviewerInkan)}
                aria-describedby={
                  fieldErrors.reviewerInkan ? 'reviewer-inkan-error' : undefined
                }
              />
              {fieldErrors.reviewerInkan && (
                <p
                  id="reviewer-inkan-error"
                  role="alert"
                  className="text-small text-destructive"
                >
                  {fieldErrors.reviewerInkan}
                </p>
              )}
            </div>

            {isRejection && (
              <div className="space-y-2">
                <Label
                  htmlFor="reject-reason"
                  className="inline-flex items-center gap-1"
                >
                  却下理由
                  <RequiredBadge />
                </Label>
                <p className="text-subtle text-foreground">
                  入力した理由は申請者への通知に使用されます。
                </p>
                <Textarea
                  id="reject-reason"
                  value={rejectReason}
                  onChange={(event) =>
                    onRejectReasonChange(event.target.value)
                  }
                  placeholder="却下理由を入力してください"
                  disabled={isSubmitting}
                  aria-invalid={Boolean(fieldErrors.rejectReason)}
                  aria-describedby={
                    fieldErrors.rejectReason ? 'reject-reason-error' : undefined
                  }
                  className="min-h-28"
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
          </section>
        </div>

        <DialogFooter className="shrink-0 border-t border-border px-6 py-4 sm:px-8 sm:py-5">
          <FormActions
            className="w-full"
            disabled={isSubmitting}
            pendingActionId={isSubmitting ? 'confirm' : null}
            actions={[
              {
                id: 'cancel',
                label: 'キャンセル',
                variant: 'outline',
                onClick: () => onOpenChange(false),
              },
              {
                id: 'confirm',
                label: actionLabel,
                pendingLabel: '処理中...',
                variant: isRejection ? 'destructive' : 'success',
                disabled: !canSubmit,
                onClick: onSubmit,
              },
            ]}
          />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
