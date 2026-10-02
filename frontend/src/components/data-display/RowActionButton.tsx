import { forwardRef } from 'react';
import { Button, type ButtonProps } from '@/components/ui/button';

export type RowActionIntent = 'view' | 'review';

type RowActionButtonProps = Omit<ButtonProps, 'variant' | 'size' | 'type'> & {
  /**
   * `view` opens a record's details. `review` marks work waiting on the user
   * (e.g. a pending application) and is the only intent that renders filled.
   */
  intent?: RowActionIntent;
};

/** Renders the one shared row/list action so every table and list card opens details the same way. */
export const RowActionButton = forwardRef<HTMLButtonElement, RowActionButtonProps>(
  ({ intent = 'view', children = '詳細', ...props }, ref) => (
    <Button
      ref={ref}
      type="button"
      size="sm"
      variant={intent === 'review' ? 'default' : 'outline'}
      {...props}
    >
      {children}
    </Button>
  ),
);
RowActionButton.displayName = 'RowActionButton';
