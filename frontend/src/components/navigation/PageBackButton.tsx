import { ArrowLeft } from 'lucide-react';

import {
  Button,
  type ButtonProps,
} from '@/components/ui/button';
import { cn } from '@/lib/utils';

type PageBackButtonPlacement =
  | 'inline'
  | 'floating';

type PageBackButtonProps = {
  label?: string;
  onClick?: () => void;
  className?: string;
  variant?: ButtonProps['variant'];
  placement?: PageBackButtonPlacement;
} & Omit<
  ButtonProps,
  'children' | 'onClick' | 'variant'
>;

/**
 * Backward-compatible floating style.
 *
 * Existing pages that still use:
 *
 * className={floatingPageBackButtonClassName}
 *
 * will continue to work.
 */
export const floatingPageBackButtonClassName =
  'absolute left-4 top-6 z-20 h-12 w-12 rounded-full p-0 sm:left-8';

const pageBackButtonPlacementClassName: Record<
  PageBackButtonPlacement,
  string
> = {
  inline: '',
  floating: floatingPageBackButtonClassName,
};

/**
 * Shared page back button.
 *
 * Usage:
 *
 * Inline:
 * <PageBackButton onClick={handleBack} />
 *
 * Floating:
 * <PageBackButton
 *   placement="floating"
 *   onClick={handleBack}
 * />
 */
export const PageBackButton = ({
  label = '戻る',
  onClick,
  className,
  variant = 'default',
  placement = 'inline',
  'aria-label': ariaLabel,
  ...buttonProps
}: PageBackButtonProps) => {
  if (!onClick) {
    return null;
  }

  const isFloating = placement === 'floating';

  return (
    <Button
      type='button'
      variant={variant}
      onClick={onClick}
      aria-label={
        ariaLabel ??
        (isFloating
          ? '前の画面へ戻る'
          : undefined)
      }
      className={cn(
        pageBackButtonPlacementClassName[
          placement
        ],
        className,
      )}
      {...buttonProps}
    >
      <ArrowLeft
        className='h-4 w-4 shrink-0'
        aria-hidden='true'
      />

      {!isFloating ? label : null}
    </Button>
  );
};