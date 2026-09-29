import { useEffect, useState, type ComponentType, type ReactNode } from 'react';
import { Menu, PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { matchPath, useLocation } from 'react-router-dom';
import { AdminSidebar } from '@/features/admin/navigation/AdminSidebar';
import { MasterSidebar } from '@/features/master/navigation/MasterSidebar';
import { UserSidebar } from '@/features/user/navigation/UserSidebar';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet';
import { storage, storageKeys } from '@/lib/storage';
import { cn } from '@/lib/utils';
import { paths } from '@/routes/paths';
import type { RoleSidebarProps } from './sidebar/sidebarTypes';
import { Navbar } from './Navbar';

const WIDE_DESKTOP_QUERY = '(min-width: 1440px)';
const APPLICATION_ROUTE_PATTERNS = [
  paths.masterDashboard,
  paths.masterSiteRegister,
  paths.masterSiteEditRoute,
  paths.masterUserProfileRoute,
  paths.userEditRoute,
  paths.userDashboard,
  paths.siteDashboardRoute,
  paths.legacyDashboardRoute,
  paths.projectMembershipRoute,
  paths.projectRegisterRoute,
  paths.projectEquipmentRoute,
  paths.projectEquipmentConfirmationRoute,
  paths.equipmentAddRoute,
  paths.siteProjectDetailRoute,
  paths.siteProjectEquipmentEditRoute,
  paths.legacyProjectRoute,
  paths.monitoringRegisterRoute,
  paths.siteDetailRoute,
  paths.profile,
  paths.passwordChange,
] as const;
const ADMIN_ROUTE_PREFIX = `${paths.adminDashboard.split('/').slice(0, 2).join('/')}/`;

/** Identifies existing application URLs without changing their route guards. */
function isApplicationPath(pathname: string): boolean {
  return (
    (pathname.startsWith(ADMIN_ROUTE_PREFIX) && pathname !== paths.adminLogin) ||
    APPLICATION_ROUTE_PATTERNS.some((pattern) => Boolean(matchPath({ path: pattern, end: true }, pathname)))
  );
}

/** Reads a saved choice, falling back to the available CSS viewport width. */
function getInitialCollapsed(): boolean {
  const savedChoice = storage.get(storageKeys.sidebarCollapsed);
  if (savedChoice === 'true') return true;
  if (savedChoice === 'false') return false;
  return typeof window !== 'undefined' && typeof window.matchMedia === 'function' && !window.matchMedia(WIDE_DESKTOP_QUERY).matches;
}

/** Owns the topbar, responsive sidebar, and content offset for application pages. */
export function AppShell({ children }: { children: ReactNode }) {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(getInitialCollapsed);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const showSidebar = Boolean(isAuthenticated && user && isApplicationPath(location.pathname));
  const RoleSidebar: ComponentType<RoleSidebarProps> =
    user?.role === 'admin' ? AdminSidebar : user?.role === 'master' ? MasterSidebar : UserSidebar;

  /** Keeps the first-visit default responsive until a manual choice is saved. */
  useEffect(() => {
    if (storage.get(storageKeys.sidebarCollapsed) !== null) return;
    if (typeof window.matchMedia !== 'function') return;
    const media = window.matchMedia(WIDE_DESKTOP_QUERY);
    const updateDefault = () => setCollapsed(!media.matches);
    media.addEventListener('change', updateDefault);
    return () => media.removeEventListener('change', updateDefault);
  }, []);

  /** Closes the mobile drawer when browser navigation changes the route. */
  useEffect(() => setDrawerOpen(false), [location.pathname]);

  /** Clears a mobile dialog if the viewport grows into the desktop layout. */
  useEffect(() => {
    if (typeof window.matchMedia !== 'function') return;
    const media = window.matchMedia('(min-width: 1024px)');
    const closeOnDesktop = () => {
      if (media.matches) setDrawerOpen(false);
    };
    media.addEventListener('change', closeOnDesktop);
    return () => media.removeEventListener('change', closeOnDesktop);
  }, []);

  /** Saves only a deliberate desktop width choice. */
  function toggleCollapsed() {
    setCollapsed((current) => {
      const next = !current;
      storage.set(storageKeys.sidebarCollapsed, String(next));
      return next;
    });
  }

  const sidebarControl = showSidebar ? (
    <>
      <SheetTrigger asChild>
        <Button type="button" variant="ghost" size="icon" className="h-11 w-11 lg:hidden" aria-label="メニューを開く" aria-expanded={drawerOpen}>
          <Menu className="h-5 w-5" aria-hidden="true" />
        </Button>
      </SheetTrigger>
      <Button type="button" variant="ghost" size="icon" className="hidden h-11 w-11 lg:inline-flex" onClick={toggleCollapsed} aria-label={collapsed ? 'サイドバーを展開' : 'サイドバーを折りたたむ'} aria-expanded={!collapsed}>
        {collapsed ? <PanelLeftOpen className="h-5 w-5" aria-hidden="true" /> : <PanelLeftClose className="h-5 w-5" aria-hidden="true" />}
      </Button>
    </>
  ) : null;

  return (
    <Sheet open={drawerOpen} onOpenChange={setDrawerOpen}>
      <div className="flex min-h-0 min-w-0 flex-1 flex-col">
        <Navbar sidebarControl={sidebarControl} appShell={showSidebar} />
        <div className="flex min-h-0 min-w-0 flex-1">
          {showSidebar && (
            <div className={cn('sticky top-20 hidden h-[calc(100dvh-5rem)] shrink-0 border-r border-sidebar-border lg:flex', collapsed ? 'w-[72px]' : 'w-60')}>
              <RoleSidebar collapsed={collapsed} />
            </div>
          )}
          <div id="main-content" className="flex min-h-0 min-w-0 flex-1 flex-col">{children}</div>
        </div>
      </div>
      {showSidebar && (
        <SheetContent side="left" aria-describedby={undefined} className="flex w-[min(88vw,360px)] flex-col p-0 lg:hidden">
          <SheetHeader className="border-b px-6 py-4 text-left">
            <SheetTitle>メニュー</SheetTitle>
          </SheetHeader>
          <RoleSidebar collapsed={false} onNavigate={() => setDrawerOpen(false)} />
        </SheetContent>
      )}
    </Sheet>
  );
}
