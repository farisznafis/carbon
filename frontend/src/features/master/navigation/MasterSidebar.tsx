import { matchPath, useLocation } from 'react-router-dom';
import { AppSidebar } from '@/components/layout/sidebar/AppSidebar';
import type { RoleSidebarProps } from '@/components/layout/sidebar/sidebarTypes';
import { paths } from '@/routes/paths';
import { getMasterDashboardTab, MASTER_SIDEBAR_ITEMS, type MasterSidebarKey } from './masterSidebarConfig';

/** Resolves the active Master destination from dashboard tabs and contextual routes. */
function getActiveMasterSidebarKey(pathname: string, search: string): MasterSidebarKey | null {
  if (pathname === paths.masterDashboard) {
    return getMasterDashboardTab(new URLSearchParams(search).get('tab'));
  }

  if (
    pathname === paths.masterSiteRegister ||
    [paths.masterSiteEditRoute, paths.siteDashboardRoute, paths.siteDetailRoute, paths.siteProjectDetailRoute, paths.siteProjectEquipmentEditRoute]
      .some((path) => Boolean(matchPath({ path, end: true }, pathname)))
  ) return 'sites';

  if (
    [paths.masterUserProfileRoute, paths.userEditRoute]
      .some((path) => Boolean(matchPath({ path, end: true }, pathname)))
  ) return 'users';

  if (
    [paths.projectMembershipRoute, paths.projectRegisterRoute, paths.projectEquipmentRoute, paths.projectEquipmentConfirmationRoute]
      .some((path) => Boolean(matchPath({ path, end: true }, pathname)))
  ) return 'membership';

  return null;
}

/** Supplies the shared sidebar with Master destinations and active state. */
export function MasterSidebar({ collapsed, onNavigate }: RoleSidebarProps) {
  const location = useLocation();
  const activeKey = getActiveMasterSidebarKey(location.pathname, location.search);
  const items = MASTER_SIDEBAR_ITEMS.map((item) => ({ ...item, isActive: item.key === activeKey }));

  return <AppSidebar items={items} label="マスターのメインナビゲーション" collapsed={collapsed} onNavigate={onNavigate} />;
}
