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
  label = 'トップへ戻る',
  className,
}: ScrollToTopButtonProps) {
  return (
    <div
      className={cn(
        'fixed bottom-6 right-6 z-50 flex origin-bottom flex-col items-center gap-2 transition-all duration-100 ease-in-out',
        isVisible
          ? 'translate-y-0 scale-100 opacity-100'
          : 'translate-y-4 scale-75 opacity-0 pointer-events-none',
        className,
      )}
    >
      <span className="text-base font-bold text-foreground transition-opacity duration-100">
        {label}
      </span>
      <Button
        type="button"
        aria-label={label}
        onClick={onClick}
        className="h-12 w-12 rounded-full bg-[#212121] p-0 text-white shadow-lg transition-all duration-100 ease-out hover:-translate-y-1 hover:bg-[#5f8f59] hover:shadow-app-md active:translate-y-0 active:scale-95"
      >
        <ArrowUp className="h-7 w-7" />
      </Button>
    </div>
  );
}