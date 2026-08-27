import { ArrowLeft } from 'lucide-react';
import {
  FormActions,
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { ChangedConfirmationValue } from '@/components/feedback/Confirmation/ChangedConfirmationValue';
import {
  FileUploadProgress,
  type FileUploadProgressItem,
} from '@/components/feedback/loading';
import { ConfirmationItem } from './confirmationItem';

type ConfirmationLayoutProps = {
  title: string;
  description?: string;
  items: ConfirmationItem[];
  onBack: () => void;
  onConfirm: () => void;
  isSubmitting?: boolean;
  uploadItems?: FileUploadProgressItem[];
  onRetryUpload?: (itemId: string) => void;
};

export function MonitoringConfirmationLayout({
  title,
  description,
  items,
  onBack,
  onConfirm,
  isSubmitting,
  uploadItems = [],
  onRetryUpload,
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
            <p className="text-small text-foreground">{description}</p>
          )}
        </div>

        <FormCard>
          <div className="space-y-5">
            <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
              {items.map((item, i) => (
                <div key={`${item.label}-${i}`}>
                  <div className="mb-1 text-sm font-medium text-foreground">
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

            <FileUploadProgress items={uploadItems} onRetry={onRetryUpload} />

            <FormActions
              className="pt-4"
              pendingActionId={isSubmitting ? 'confirm' : null}
              actions={[
                {
                  id: 'back',
                  label: (
                    <>
                      <ArrowLeft className="h-4 w-4" />
                      戻る
                    </>
                  ),
                  variant: 'outline',
                  onClick: onBack,
                },
                {
                  id: 'confirm',
                  label: '確定',
                  pendingLabel: '送信中...',
                  onClick: onConfirm,
                },
              ]}
            />
          </div>
        </FormCard>
      </FormPageContent>
    </FormStandalonePage>
  );
}
