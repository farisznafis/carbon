import { TooltipProvider } from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';
import { SidebarNavItem } from './SidebarNavItem';
import type { SidebarNavigationItem } from './sidebarTypes';

type AppSidebarProps = {
  items: readonly SidebarNavigationItem[];
  label: string;
  collapsed: boolean;
  onNavigate?: () => void;
};

/** Displays role-provided destinations without interpreting role or permission rules. */
export function AppSidebar({ items, label, collapsed, onNavigate }: AppSidebarProps) {
  return (
    <aside className="flex min-h-0 w-full flex-col bg-sidebar text-sidebar-foreground">
      <nav aria-label={label} className="min-h-0 flex-1 overflow-y-auto overflow-x-hidden p-3">
        <TooltipProvider delayDuration={150}>
          <ul className={cn('flex flex-col gap-1', collapsed && 'items-stretch')}>
            {items.map((item) => (
              <SidebarNavItem key={item.key} item={item} collapsed={collapsed} onNavigate={onNavigate} />
            ))}
          </ul>
        </TooltipProvider>
      </nav>
    </aside>
  );
}
