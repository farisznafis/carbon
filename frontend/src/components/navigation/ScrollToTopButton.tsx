import { ArrowUp } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type ScrollToTopButtonProps = {
  onClick: () => void;
  label?: string;
  className?: string;
};

export function ScrollToTopButton({
  onClick,
  label = 'トップへ戻る',
  className,
}: ScrollToTopButtonProps) {
  return (
    <div
      className={cn(
        'fixed bottom-6 right-6 z-50 flex flex-col items-center gap-2',
        className,
      )}
    >
      <span className='text-base font-bold text-foreground'>{label}</span>
      <Button
        type='button'
        aria-label={label}
        onClick={onClick}
        className='h-12 w-12 rounded-full bg-[#212121] p-0 text-white shadow-lg hover:bg-[#5f8f59]'
      >
        <ArrowUp className='h-7 w-7' />
      </Button>
    </div>
  );
}
