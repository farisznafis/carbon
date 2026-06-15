import { ArrowLeft } from 'lucide-react';
import { FormPageWrapper } from '@/components/layout/FormPageWrapper/FormPageWrapper';
import { Button } from '@/components/ui/button';
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
    <FormPageWrapper
      pageName={`${title}の確認`}
      title={title}
      description={description}
      onBack={onBack}
    >
      <div className='space-y-4'>
        {items.map((item, i) => (
          <div key={`${item.label}-${i}`}>
            <div className='text-sm text-muted-foreground mb-1'>
              {item.label}
            </div>
            <div className={item.isChanged ? 'underline font-bold' : ''}>
              {item.value}
            </div>
          </div>
        ))}

        <div className='flex justify-end gap-4 pt-4'>
          <Button
            variant='outline'
            onClick={onBack}
            disabled={isSubmitting}
          >
            <ArrowLeft className='mr-2 h-4 w-4' />
            戻る
          </Button>
          <Button
            onClick={onConfirm}
            disabled={isSubmitting}
            size='lg'
          >
            {isSubmitting ? '確定中...' : '確定'}
          </Button>
        </div>
      </div>
    </FormPageWrapper>
  );
}
