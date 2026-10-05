import { forwardRef, type ReactNode } from 'react';
import { ArrowLeft, ArrowRight, Pencil, Plus, RefreshCw } from 'lucide-react';
import { Button, type ButtonProps } from '@/components/ui/button';
import { cn } from '@/lib/utils';

/**
 * What the button does. The kind fixes shape, color, size and icon so the
 * same function looks the same on every page; only the label varies.
 *
 * - `view`   open a record's details from a row or list item (詳細, プロファイル)
 * - `review` open a record that is waiting on the user (申請内容を確認)
 * - `add`    create a new record (運用者を追加)
 * - `edit`   edit the record the page is showing (更新, 編集)
 * - `retry`  reload after a failed fetch (再試行, 再読み込み)
 * - `back`   return to the previous screen from inside the content (一覧へ戻る).
 *            The page-level round back button stays `PageBackButton`.
 * - `link`   jump to a related list or page from a card header (すべて見る)
 */
export type ActionKind =
  | 'view'
  | 'review'
  | 'add'
  | 'edit'
  | 'retry'
  | 'back'
  | 'link';

type ActionPresentation = {
  variant: NonNullable<ButtonProps['variant']>;
  size: NonNullable<ButtonProps['size']>;
  leadingIcon?: ReactNode;
  trailingIcon?: ReactNode;
  className?: string;
  defaultLabel: string;
};

const ACTION_PRESENTATION: Record<ActionKind, ActionPresentation> = {
  view: { variant: 'outline', size: 'sm', defaultLabel: '詳細' },
  review: { variant: 'default', size: 'sm', defaultLabel: '確認' },
  add: {
    variant: 'default',
    size: 'md',
    leadingIcon: <Plus aria-hidden="true" />,
    defaultLabel: '追加',
  },
  edit: {
    variant: 'outline',
    size: 'md',
    leadingIcon: <Pencil aria-hidden="true" />,
    defaultLabel: '更新',
  },
  retry: {
    variant: 'outline',
    size: 'md',
    leadingIcon: <RefreshCw aria-hidden="true" />,
    defaultLabel: '再試行',
  },
  back: {
    variant: 'outline',
    size: 'md',
    leadingIcon: <ArrowLeft aria-hidden="true" />,
    defaultLabel: '戻る',
  },
  link: {
    variant: 'ghost',
    size: 'sm',
    trailingIcon: <ArrowRight aria-hidden="true" />,
    className: 'px-2 text-primary-strong hover:text-primary-strong',
    defaultLabel: 'すべて見る',
  },
};

export type ActionButtonProps = Omit<ButtonProps, 'variant' | 'size' | 'asChild'> & {
  kind: ActionKind;
  /** Hides the kind's icon, e.g. when the label alone must fit a narrow cell. */
  hideIcon?: boolean;
};

/** Renders a button whose appearance is decided by what it does, not by the page using it. */
export const ActionButton = forwardRef<HTMLButtonElement, ActionButtonProps>(
  ({ kind, hideIcon = false, className, children, type = 'button', ...props }, ref) => {
    const presentation = ACTION_PRESENTATION[kind];

    return (
      <Button
        ref={ref}
        type={type}
        variant={presentation.variant}
        size={presentation.size}
        className={cn(presentation.className, className)}
        {...props}
      >
        {!hideIcon && presentation.leadingIcon}
        {children ?? presentation.defaultLabel}
        {!hideIcon && presentation.trailingIcon}
      </Button>
    );
  },
);
ActionButton.displayName = 'ActionButton';
