import { useLocation } from 'react-router-dom';
import { AppSidebar } from '@/components/layout/sidebar/AppSidebar';
import type { RoleSidebarProps } from '@/components/layout/sidebar/sidebarTypes';
import { ADMIN_NAVIGATION_ITEMS, getActiveAdminNavigationKey } from './adminNavigationConfig';

/** Maps the existing admin navigation source to the shared sidebar. */
export function AdminSidebar({ collapsed, onNavigate }: RoleSidebarProps) {
  const location = useLocation();
  const legacyTab = new URLSearchParams(location.search).get('tab');
  const activeKey = getActiveAdminNavigationKey(location.pathname, legacyTab);
  const items = ADMIN_NAVIGATION_ITEMS.map((item) => ({ ...item, isActive: item.key === activeKey }));

  return <AppSidebar items={items} label="管理画面のメインナビゲーション" collapsed={collapsed} onNavigate={onNavigate} />;
}
