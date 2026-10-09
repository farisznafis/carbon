import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Portal as TooltipPortal } from '@radix-ui/react-tooltip';
import { BookOpen, HelpCircle, MessageCircleQuestion } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';
import { paths } from '@/routes/paths';
import { useGuideTour } from './GuideTourProvider';

const HELP_LABEL = 'ヘルプ・ガイド';

type SidebarHelpMenuProps = {
  collapsed: boolean;
  onNavigate?: () => void;
};

/** Help entry pinned to the bottom of the sidebar; offers the page guide and FAQ. */
export function SidebarHelpMenu({
  collapsed,
  onNavigate,
}: Readonly<SidebarHelpMenuProps>) {
  const navigate = useNavigate();
  const { currentContext: guideContext, startTour } = useGuideTour();
  const [popoverOpen, setPopoverOpen] = useState(false);

  /** Replays the same page tour shown on the first visit. */
  const handleOpenGuide = () => {
    if (!guideContext) return;
    setPopoverOpen(false);
    // Closes the mobile drawer so the tour can point at the page behind it.
    onNavigate?.();
    startTour(guideContext);
  };

  const handleOpenFaq = () => {
    setPopoverOpen(false);
    onNavigate?.();
    navigate(paths.adminHelpFaq);
  };

  const trigger = (
    <button
      type='button'
      aria-label={collapsed ? HELP_LABEL : undefined}
      aria-haspopup='menu'
      aria-expanded={popoverOpen}
      className={cn(
        'flex min-h-11 w-full items-center gap-3 rounded-lg px-3.5 text-sm text-sidebar-foreground transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
        collapsed && 'justify-center px-0',
        popoverOpen && 'bg-sidebar-accent text-sidebar-accent-foreground',
      )}
    >
      <HelpCircle
        className='h-5 w-5 shrink-0'
        aria-hidden='true'
      />
      {!collapsed && <span>{HELP_LABEL}</span>}
    </button>
  );

  return (
    <div className='shrink-0 px-3 pb-3' data-tour='help-menu'>
      <Popover
        open={popoverOpen}
        onOpenChange={setPopoverOpen}
      >
        {collapsed ? (
          <TooltipProvider delayDuration={150}>
            <Tooltip>
              <TooltipTrigger asChild>
                <PopoverTrigger asChild>{trigger}</PopoverTrigger>
              </TooltipTrigger>
              <TooltipPortal>
                <TooltipContent side='right'>{HELP_LABEL}</TooltipContent>
              </TooltipPortal>
            </Tooltip>
          </TooltipProvider>
        ) : (
          <PopoverTrigger asChild>{trigger}</PopoverTrigger>
        )}

        <PopoverContent
          side='right'
          align='end'
          sideOffset={12}
          className='w-56 p-1.5'
        >
          {/* Kept visible when unavailable so it does not seem to vanish between pages. */}
          <Button
            type='button'
            variant='ghost'
            onClick={handleOpenGuide}
            disabled={!guideContext}
            aria-describedby={guideContext ? undefined : 'guide-unavailable-note'}
            className='h-auto min-h-9 w-full items-start justify-start rounded-md px-3 py-2 text-left'
          >
            <BookOpen className='mt-0.5 h-4 w-4 shrink-0 text-muted-foreground' aria-hidden='true' />
            <span className='flex flex-col'>
              使い方ガイド
              {!guideContext && (
                <span id='guide-unavailable-note' className='text-xs font-normal text-muted-foreground'>
                  このページにはガイドがありません
                </span>
              )}
            </span>
          </Button>

          <Button
            type='button'
            variant='ghost'
            onClick={handleOpenFaq}
            className='h-9 w-full justify-start rounded-md px-3'
          >
            <MessageCircleQuestion className='h-4 w-4 shrink-0 text-muted-foreground' />
            ヘルプ・FAQ
          </Button>
        </PopoverContent>
      </Popover>
    </div>
  );
}
