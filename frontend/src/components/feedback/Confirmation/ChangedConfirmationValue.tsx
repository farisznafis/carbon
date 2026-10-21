import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

type ChangedConfirmationValueProps = {
  children: ReactNode;
  className?: string;
};

export function ChangedConfirmationValue({
  children,
  className,
}: Readonly<ChangedConfirmationValueProps>) {
  return (
    <span className='inline-flex max-w-full flex-wrap items-center gap-2'>
      <span
        className={cn(
          'rounded bg-secondary/60 px-1.5 py-0.5 font-medium text-primary-strong',
          className,
        )}
      >
        {children}
      </span>
      <span className='rounded-full border border-primary/20 bg-secondary px-2 py-0.5 text-xs font-medium text-primary-strong'>
        変更あり
      </span>
    </span>
  );
}
