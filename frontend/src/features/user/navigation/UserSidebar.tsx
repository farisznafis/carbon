import { useLocation } from 'react-router-dom';
import { AppSidebar } from '@/components/layout/sidebar/AppSidebar';
import type { RoleSidebarProps } from '@/components/layout/sidebar/sidebarTypes';
import { useAuth } from '@/contexts/AuthContext';
import { paths } from '@/routes/paths';
import { getHomePath } from '@/utils/getHomePath';
import { USER_SIDEBAR_ITEMS } from './userSidebarConfig';

/** Resolves the signed-in user's site-aware dashboard link. */
export function UserSidebar({ collapsed, onNavigate }: RoleSidebarProps) {
  const { user } = useAuth();
  const location = useLocation();
  const isDashboard = location.pathname === paths.userDashboard || /^\/sites\/[^/]+\/dashboard$/.test(location.pathname);
  const items = USER_SIDEBAR_ITEMS.map((item) => ({ ...item, href: getHomePath(user), isActive: isDashboard }));

  return <AppSidebar items={items} label="ユーザーのメインナビゲーション" collapsed={collapsed} onNavigate={onNavigate} />;
}
