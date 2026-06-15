import { ArrowLeft } from 'lucide-react';
import { Button, type ButtonProps } from '@/components/ui/button';

type PageBackButtonProps = {
  label?: string;
  onClick?: () => void;
  className?: string;
  variant?: ButtonProps['variant'];
} & Omit<ButtonProps, 'children' | 'onClick' | 'variant'>;

export function PageBackButton({
  label = '戻る',
  // label = '戻る',
  onClick,
  className,
  variant = 'default',
  ...buttonProps
}: PageBackButtonProps) {
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
}
