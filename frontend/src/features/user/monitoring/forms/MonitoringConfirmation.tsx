import { ArrowLeft } from 'lucide-react';
import {
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Button } from '@/components/ui/button';
import { ChangedConfirmationValue } from '@/components/feedback/Confirmation/ChangedConfirmationValue';
import { ConfirmationItem } from './confirmationItem';

type ConfirmationLayoutProps = {
  title: string;
  description?: string;
  items: ConfirmationItem[];
  onBack: () => void;
  onConfirm: () => void;
  isSubmitting?: boolean;
};

export function MonitoringConfirmationLayout({
  title,
  description,
  items,
  onBack,
  onConfirm,
  isSubmitting,
}: ConfirmationLayoutProps) {
  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={onBack}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />

      <FormPageContent>
        <div className="space-y-3">
          <FormPageTitle>{title}</FormPageTitle>
          {description && (
            <p className="text-small text-muted-foreground">{description}</p>
          )}
        </div>

        <FormCard>
          <div className="space-y-5">
            <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
              {items.map((item, i) => (
                <div key={`${item.label}-${i}`}>
                  <div className="mb-1 text-sm font-medium text-muted-foreground">
                    {item.label}
                  </div>
                  <div>
                    {item.isChanged ? (
                      <ChangedConfirmationValue>
                        {item.value}
                      </ChangedConfirmationValue>
                    ) : (
                      item.value
                    )}
                  </div>
                </div>
              ))}
            </div>

            <div className="flex justify-end gap-4 pt-4">
              <Button variant="outline" onClick={onBack} disabled={isSubmitting} size="md">
                <ArrowLeft className="mr-2 h-4 w-4" />
                戻る
              </Button>
              <Button onClick={onConfirm} disabled={isSubmitting} size="md">
                {isSubmitting ? '送信中...' : '確定'}
              </Button>
            </div>
          </div>
        </FormCard>
      </FormPageContent>
    </FormStandalonePage>
  );
}
