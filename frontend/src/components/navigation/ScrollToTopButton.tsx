import { ArrowUp } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type ScrollToTopButtonProps = {
  onClick: () => void;
  isVisible: boolean;
  label?: string;
  className?: string;
};

export function ScrollToTopButton({
  onClick,
  isVisible,
  label = '',
  className,
}: ScrollToTopButtonProps) {
  return (
    <div
      className={cn(
        'fixed bottom-3 right-3 z-50 flex origin-bottom flex-col items-center gap-1 transition-all duration-100 ease-in-out',
        isVisible
          ? 'translate-y-0 scale-100 opacity-100'
          : 'translate-y-4 scale-75 opacity-0 pointer-events-none',
        className,
      )}
    >
      <span className='text-xs font-medium text-foreground transition-opacity duration-100'>
        {label}
      </span>
      <Button
        type='button'
        size='icon'
        aria-label={label}
        onClick={onClick}
        className='rounded-full bg-foreground text-background shadow-app-sm transition-all duration-100 ease-out hover:-translate-y-0.5 hover:bg-primary hover:text-primary-foreground hover:shadow-app-md active:translate-y-0 active:scale-95'
      >
        <ArrowUp className='h-4 w-4' />
      </Button>
    </div>
  );
}
