import { useState } from 'react';
import { matchPath, useLocation, useNavigate } from 'react-router-dom';
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
import { GuideModal } from './GuideModal';
import type { GuideContext } from './guideSteps';

const HELP_LABEL = 'ヘルプ・ガイド';

const GUIDE_CONTEXT_ROUTES: ReadonlyArray<{
  path: string;
  context: GuideContext;
}> = [
  { path: paths.masterDashboard, context: 'master-dashboard' },
  { path: paths.userDashboard, context: 'site-dashboard' },
  { path: paths.siteDashboardRoute, context: 'site-dashboard' },
  { path: paths.siteProjectDetailRoute, context: 'project-detail' },
  { path: paths.legacyProjectRoute, context: 'project-detail' },
];

/** Resolves which page guide applies to the current route, if any. */
export function getGuideContextForPath(pathname: string): GuideContext | null {
  return (
    GUIDE_CONTEXT_ROUTES.find(({ path }) =>
      Boolean(matchPath({ path, end: true }, pathname)),
    )?.context ?? null
  );
}

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
  const location = useLocation();
  const guideContext = getGuideContextForPath(location.pathname);
  const [popoverOpen, setPopoverOpen] = useState(false);
  const [guideOpen, setGuideOpen] = useState(false);
  // Keeps the guide content stable while the modal animates closed.
  const [shownGuideContext, setShownGuideContext] =
    useState<GuideContext | null>(null);

  const handleOpenGuide = () => {
    setPopoverOpen(false);
    setShownGuideContext(guideContext);
    setGuideOpen(true);
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
    <>
      <div className='shrink-0 px-3 pb-3'>
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
            className='w-48 p-1.5'
          >
            {guideContext && (
              <Button
                type='button'
                variant='ghost'
                onClick={handleOpenGuide}
                className='h-9 w-full justify-start rounded-md px-3'
              >
                <BookOpen className='h-4 w-4 shrink-0 text-muted-foreground' />
                使い方ガイド
              </Button>
            )}

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

      {shownGuideContext && (
        <GuideModal
          open={guideOpen}
          context={shownGuideContext}
          onClose={() => setGuideOpen(false)}
        />
      )}
    </>
  );
}
