import { ArrowLeft } from 'lucide-react';
import { Button, type ButtonProps } from '@/components/ui/button';

type PageBackButtonProps = {
  label?: string;
  onClick?: () => void;
  className?: string;
  variant?: ButtonProps['variant'];
} & Omit<ButtonProps, 'children' | 'onClick' | 'variant'>;

export const floatingPageBackButtonClassName =
  'absolute left-4 top-10 z-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16';

export const PageBackButton = ({
  label = '戻る',
  onClick,
  className,
  variant = 'default',
  ...buttonProps
}: PageBackButtonProps) => {
  if (!onClick) return null;

  return (
    <Button
      type='button'
      variant={variant}
      onClick={onClick}
      className={className}
      {...buttonProps}
    >
      <ArrowLeft className='h-4 w-4' />
      {label}
    </Button>
  );
};
