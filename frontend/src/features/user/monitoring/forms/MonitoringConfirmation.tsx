import {
  FormActions,
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
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

/** Renders monitoring review details, upload progress, and submission actions. */
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
                  label: '戻る',
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
