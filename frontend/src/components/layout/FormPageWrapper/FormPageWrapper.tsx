import React from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { cn } from '@/lib/utils';

interface FormPageWrapperProps {
  /** Top page header title */
  pageName: string;

  /** Kept for compatibility with existing callers. */
  title: string;

  /** Form contents */
  children: React.ReactNode;

  /** Optional override for supporting text. */
  description?: React.ReactNode;

  onBack?: () => void;
  backText?: string;
  contentMaxWidth?: string;
  contentClassName?: string;
  headerClassName?: string;
  wrapInCard?: boolean;
}

/**
 * Standardized layout wrapper for form pages.
 */
export const FormPageWrapper: React.FC<FormPageWrapperProps> = ({
  pageName,
  children,
  onBack,
  backText,
  contentMaxWidth = 'max-w-md',
  contentClassName = '',
  headerClassName,
  wrapInCard = true,
}) => {
  const content = wrapInCard ? (
    <Card className='border-none'>
      <CardContent className='space-y-4'>{children}</CardContent>
    </Card>
  ) : (
    children
  );

  return (
    <div className='min-h-screen bg-background'>
      <div
        className={cn(
          'bg-primary-strong px-4 py-6 text-primary-foreground',
          headerClassName,
        )}
      >
        <div className='mx-auto max-w-5xl'>
          <h2 className='text-center text-form-title font-bold'>{pageName}</h2>
        </div>
      </div>

      <div className='px-4 py-8'>
        <div className='relative mx-auto max-w-[95%]'>
          {onBack && (
            <div className='absolute left-0 top-0'>
              <PageBackButton
                onClick={onBack}
                // label={backText ?? ''}
                label={''}
                className='mb-4 rounded-full w-12 h-12'
              />
            </div>
          )}

          <div className='flex justify-center'>
            <div
              className={`w-full ${contentMaxWidth} space-y-4 pt-2 animate-slide-up ${contentClassName}`}
            >
              {content}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
