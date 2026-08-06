import type {
  ParticipationReviewChecklist,
  ParticipationReviewChecklistField,
} from '@/api/admin';
import { participationReviewChecklistFields } from '@/api/admin';
import { FormCard } from '@/components/forms/form-layout';
import { RequiredBadge } from '@/components/forms/shared/RequiredLabel';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { participationReviewChecklistLabels } from './participationReviewDisplay';

type ParticipationReviewChecklistCardProps = {
  checklist: ParticipationReviewChecklist;
  reviewerInkan: string;
  readOnly: boolean;
  reviewerInkanError: string | null;
  checklistError: string | null;
  disabled: boolean;
  onChecklistChange: (
    fieldName: ParticipationReviewChecklistField,
    checked: boolean,
  ) => void;
  onReviewerInkanChange: (value: string) => void;
};

export function ParticipationReviewChecklistCard({
  checklist,
  reviewerInkan,
  readOnly,
  reviewerInkanError,
  checklistError,
  disabled,
  onChecklistChange,
  onReviewerInkanChange,
}: ParticipationReviewChecklistCardProps) {
  return (
    <FormCard>
      <section className="space-y-6">
        <div className="space-y-2">
          <h2 className="text-xl font-bold text-foreground">
            レビューチェックリスト
          </h2>
          <p className="text-small text-foreground">
            承認するには、すべての項目を確認してください。
          </p>
        </div>

        <div className="grid gap-3 md:grid-cols-2">
          {participationReviewChecklistFields.map((fieldName) => {
            const checkboxId = `review-checklist-${fieldName}`;
            return (
              <div
                key={fieldName}
                className="flex items-start gap-3 rounded-lg border border-border bg-muted/10 p-4"
              >
                <Checkbox
                  id={checkboxId}
                  checked={checklist[fieldName]}
                  disabled={readOnly || disabled}
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

        {checklistError && (
          <p role="alert" className="text-small text-destructive">
            {checklistError}
          </p>
        )}

        {!readOnly && (
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
              disabled={disabled}
              aria-invalid={Boolean(reviewerInkanError)}
              aria-describedby={
                reviewerInkanError ? 'reviewer-inkan-error' : undefined
              }
            />
            {reviewerInkanError && (
              <p
                id="reviewer-inkan-error"
                role="alert"
                className="text-small text-destructive"
              >
                {reviewerInkanError}
              </p>
            )}
          </div>
        )}
      </section>
    </FormCard>
  );
}

