import {
  matchPath,
  useLocation,
} from 'react-router-dom';

import { AppSidebar } from '@/components/layout/sidebar/AppSidebar';
import type { RoleSidebarProps } from '@/components/layout/sidebar/sidebarTypes';
import { useAuth } from '@/contexts/AuthContext';
import { SidebarHelpMenu } from '@/features/master/help/SidebarHelpMenu';
import { paths } from '@/routes/paths';

import {
  getMasterDashboardTab,
  MASTER_SIDEBAR_ITEMS,
  type MasterSidebarKey,
} from './masterSidebarConfig';
import { MasterSidebarCompanyContext } from './MasterSidebarCompanyContext';

/**
 * Resolves the active Master destination
 * from dashboard tabs and contextual routes.
 */
function getActiveMasterSidebarKey(
  pathname: string,
  search: string,
): MasterSidebarKey | null {
  if (pathname === paths.masterDashboard) {
    return getMasterDashboardTab(
      new URLSearchParams(search).get('tab'),
    );
  }

  if (
    pathname === paths.masterSiteRegister ||
    [
      paths.masterSiteEditRoute,
      paths.siteDashboardRoute,
      paths.siteDetailRoute,
      paths.siteProjectDetailRoute,
      paths.siteProjectEquipmentEditRoute,
    ].some((path) =>
      Boolean(
        matchPath(
          {
            path,
            end: true,
          },
          pathname,
        ),
      ),
    )
  ) {
    return 'sites';
  }

  if (
    [
      paths.masterUserProfileRoute,
      paths.userEditRoute,
    ].some((path) =>
      Boolean(
        matchPath(
          {
            path,
            end: true,
          },
          pathname,
        ),
      ),
    )
  ) {
    return 'users';
  }

  if (
    [
      paths.projectMembershipRoute,
      paths.projectRegisterRoute,
      paths.projectEquipmentRoute,
      paths.projectEquipmentConfirmationRoute,
    ].some((path) =>
      Boolean(
        matchPath(
          {
            path,
            end: true,
          },
          pathname,
        ),
      ),
    )
  ) {
    return 'membership';
  }

  return null;
}

/**
 * Supplies Master navigation and organization context
 * to the shared application sidebar.
 */
export function MasterSidebar({
  collapsed,
  onNavigate,
}: Readonly<RoleSidebarProps>) {
  const { user } = useAuth();
  const location = useLocation();

  const activeKey =
    getActiveMasterSidebarKey(
      location.pathname,
      location.search,
    );

  const items = MASTER_SIDEBAR_ITEMS.map(
    (item) => ({
      ...item,
      isActive: item.key === activeKey,
    }),
  );

  return (
    <div
      className="
        flex
        min-h-0
        w-full
        flex-1
        flex-col
      "
    >
      <AppSidebar
        items={items}
        label="マスターのメインナビゲーション"
        collapsed={collapsed}
        onNavigate={onNavigate}
      />

      <SidebarHelpMenu
        collapsed={collapsed}
        onNavigate={onNavigate}
      />

      <MasterSidebarCompanyContext
        companyName={user?.companyName ?? ''}
        collapsed={collapsed}
      />
    </div>
  );
}