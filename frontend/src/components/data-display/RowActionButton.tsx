import { forwardRef } from 'react';
import {
  ActionButton,
  type ActionButtonProps,
} from '@/components/actions/ActionButton';

export type RowActionIntent = 'view' | 'review';

type RowActionButtonProps = Omit<ActionButtonProps, 'kind'> & {
  /**
   * `view` opens a record's details. `review` marks work waiting on the user
   * (e.g. a pending application) and is the only intent that renders filled.
   */
  intent?: RowActionIntent;
};

/** Renders the shared row/list action; the label defaults to 詳細 and can be replaced (e.g. プロファイル). */
export const RowActionButton = forwardRef<HTMLButtonElement, RowActionButtonProps>(
  ({ intent = 'view', ...props }, ref) => (
    <ActionButton ref={ref} kind={intent} {...props} />
  ),
);
RowActionButton.displayName = 'RowActionButton';
