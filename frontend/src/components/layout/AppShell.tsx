import {
  useEffect,
  useState,
  type ComponentType,
  type ReactNode,
} from 'react';
import { Menu } from 'lucide-react';
import { matchPath, useLocation } from 'react-router-dom';

import { BreadcrumbProvider } from '@/components/navigation/BreadcrumbProvider';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import { useAuth } from '@/contexts/AuthContext';
import { AdminSidebar } from '@/features/admin/navigation/AdminSidebar';
import { MasterSidebar } from '@/features/master/navigation/MasterSidebar';
import { UserSidebar } from '@/features/user/navigation/UserSidebar';
import { storage, storageKeys } from '@/lib/storage';
import { cn } from '@/lib/utils';
import { paths } from '@/routes/paths';
import { getHomePath } from '@/utils/getHomePath';

import { Navbar } from './Navbar';
import { SidebarHeader } from './sidebar/SidebarHeader';
import type { RoleSidebarProps } from './sidebar/sidebarTypes';

const WIDE_DESKTOP_QUERY = '(min-width: 1440px)';
const DESKTOP_QUERY = '(min-width: 1024px)';

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

const ADMIN_ROUTE_PREFIX =
  `${paths.adminDashboard.split('/').slice(0, 2).join('/')}/`;

/**
 * Identifies authenticated application pages that use the persistent
 * application sidebar.
 */
function isApplicationPath(pathname: string): boolean {
  return (
    (
      pathname.startsWith(ADMIN_ROUTE_PREFIX) &&
      pathname !== paths.adminLogin
    ) ||
    APPLICATION_ROUTE_PATTERNS.some((pattern) =>
      Boolean(
        matchPath(
          {
            path: pattern,
            end: true,
          },
          pathname,
        ),
      ),
    )
  );
}

/**
 * Uses a previously saved preference when available.
 *
 * First visit:
 * - >= 1440px: expanded
 * - 1024px–1439px: collapsed
 */
function getInitialCollapsed(): boolean {
  const savedChoice = storage.get(
    storageKeys.sidebarCollapsed,
  );

  if (savedChoice === 'true') {
    return true;
  }

  if (savedChoice === 'false') {
    return false;
  }

  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    !window.matchMedia(WIDE_DESKTOP_QUERY).matches
  );
}

/**
 * Owns the application shell:
 *
 * Desktop:
 *
 * ┌─────────────┬─────────────────────────┐
 * │ Sidebar     │ Breadcrumb      Actions │
 * │             ├─────────────────────────┤
 * │             │ Main content            │
 * └─────────────┴─────────────────────────┘
 *
 * Mobile:
 *
 * ┌───────────────────────────────────────┐
 * │ Menu + GX (or ‹ parent)   Actions     │
 * ├───────────────────────────────────────┤
 * │ Main content                          │
 * └───────────────────────────────────────┘
 */
function AppShellLayout({
  children,
}: {
  children: ReactNode;
}) {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  const [collapsed, setCollapsed] = useState(
    getInitialCollapsed,
  );
  const [drawerOpen, setDrawerOpen] =
    useState(false);

  const showSidebar = Boolean(
    isAuthenticated &&
      user &&
      isApplicationPath(location.pathname),
  );

  const RoleSidebar: ComponentType<RoleSidebarProps> =
    user?.role === 'admin'
      ? AdminSidebar
      : user?.role === 'master'
        ? MasterSidebar
        : UserSidebar;

  const homeHref = getHomePath(user);

  /**
   * Until the user explicitly chooses a state,
   * adapt the initial sidebar density to the viewport.
   */
  useEffect(() => {
    if (
      storage.get(storageKeys.sidebarCollapsed) !== null
    ) {
      return;
    }

    if (typeof window.matchMedia !== 'function') {
      return;
    }

    const media = window.matchMedia(
      WIDE_DESKTOP_QUERY,
    );

    const updateDefault = () => {
      setCollapsed(!media.matches);
    };

    media.addEventListener(
      'change',
      updateDefault,
    );

    return () => {
      media.removeEventListener(
        'change',
        updateDefault,
      );
    };
  }, []);

  /**
   * Close the mobile drawer after route navigation.
   */
  useEffect(() => {
    setDrawerOpen(false);
  }, [location.pathname]);

  /**
   * Close the mobile Sheet if the viewport grows
   * into desktop mode while it is open.
   */
  useEffect(() => {
    if (typeof window.matchMedia !== 'function') {
      return;
    }

    const media = window.matchMedia(
      DESKTOP_QUERY,
    );

    const closeOnDesktop = () => {
      if (media.matches) {
        setDrawerOpen(false);
      }
    };

    media.addEventListener(
      'change',
      closeOnDesktop,
    );

    return () => {
      media.removeEventListener(
        'change',
        closeOnDesktop,
      );
    };
  }, []);

  /**
   * Persist only an explicit user choice.
   */
  function toggleCollapsed() {
    setCollapsed((current) => {
      const next = !current;

      storage.set(
        storageKeys.sidebarCollapsed,
        String(next),
      );

      return next;
    });
  }

  /**
   * Desktop collapse/expand is owned by SidebarHeader.
   * Navbar only owns the mobile drawer trigger.
   */
  const mobileSidebarControl = showSidebar ? (
    <SheetTrigger asChild>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        className="h-11 w-11 lg:hidden"
        aria-label="メニューを開く"
        aria-expanded={drawerOpen}
      >
        <Menu
          className="h-5 w-5"
          aria-hidden="true"
        />
      </Button>
    </SheetTrigger>
  ) : null;

  return (
    <Sheet
      open={drawerOpen}
      onOpenChange={setDrawerOpen}
    >
      <div className="flex min-h-0 min-w-0 flex-1">
        {/* Desktop sidebar */}
        {showSidebar && (
          <aside
            className={cn(
              'sticky top-0 z-50 hidden h-dvh shrink-0 flex-col',
              'border-r border-sidebar-border bg-sidebar',
              'overflow-visible',
              'lg:flex',
              collapsed ? 'w-[72px]' : 'w-[220px] 2xl:w-[240px] min-[1800px]:w-[280px]',
            )}
          >
            <SidebarHeader
              homeHref={homeHref}
              collapsed={collapsed}
              onToggleCollapsed={
                toggleCollapsed
              }
            />

            <RoleSidebar
              collapsed={collapsed}
            />
          </aside>
        )}

        {/* Right application area */}
        <div className="flex min-h-0 min-w-0 flex-1 flex-col">
          <Navbar
            sidebarControl={
              mobileSidebarControl
            }
            appShell={showSidebar}
          />

          <div
            id="main-content"
            className="
              flex
              min-h-0
              min-w-0
              flex-1
              flex-col
            "
          >
            {children}
          </div>
        </div>
      </div>

      {/* Mobile sidebar drawer */}
      {showSidebar && (
        <SheetContent
          side="left"
          aria-describedby={undefined}
          className="
            flex
            w-[min(88vw,360px)]
            flex-col
            p-0
            lg:hidden
          "
        >
          <SheetHeader className="border-b border-border px-6 py-4 text-left">
            <SheetTitle>
              メニュー
            </SheetTitle>
          </SheetHeader>

          <RoleSidebar
            collapsed={false}
            onNavigate={() =>
              setDrawerOpen(false)
            }
          />
        </SheetContent>
      )}
    </Sheet>
  );
}

/** Lets pages publish their breadcrumb to the topbar via `usePageBreadcrumb`. */
export function AppShell({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <BreadcrumbProvider>
      <AppShellLayout>{children}</AppShellLayout>
    </BreadcrumbProvider>
  );
}
