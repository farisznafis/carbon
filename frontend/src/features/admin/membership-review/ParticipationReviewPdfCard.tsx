import { ExternalLink } from 'lucide-react';
import { ButtonLoader } from '@/components/feedback/loading';
import { FormCard } from '@/components/forms/form-layout';
import { Button } from '@/components/ui/button';

type ParticipationReviewPdfCardProps = {
  isLoading: boolean;
  onView: () => void;
};

/** Displays the membership PDF action separately from the review decision controls. */
export function ParticipationReviewPdfCard({
  isLoading,
  onView,
}: ParticipationReviewPdfCardProps) {
  return (
    <FormCard>
      <section className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-2">
          <h2 className="text-xl font-bold text-foreground">入会届PDF</h2>
          <p className="text-small text-foreground">
            申請内容をPDFで確認できます。
          </p>
        </div>

        <Button
          type="button"
          variant="outline"
          size="md"
          className="w-full shrink-0 sm:w-auto"
          disabled={isLoading}
          aria-busy={isLoading || undefined}
          onClick={onView}
        >
          {isLoading ? <ButtonLoader /> : <ExternalLink />}
          <span>{isLoading ? '取得中...' : 'PDFを表示'}</span>
        </Button>
      </section>
    </FormCard>
  );
}
