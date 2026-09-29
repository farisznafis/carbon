import { useLocation } from 'react-router-dom';
import { AppSidebar } from '@/components/layout/sidebar/AppSidebar';
import type { RoleSidebarProps } from '@/components/layout/sidebar/sidebarTypes';
import { paths } from '@/routes/paths';
import { MASTER_SIDEBAR_ITEMS } from './masterSidebarConfig';

/** Keeps the master menu limited to its persistent dashboard destination. */
export function MasterSidebar({ collapsed, onNavigate }: RoleSidebarProps) {
  const location = useLocation();
  const items = MASTER_SIDEBAR_ITEMS.map((item) => ({ ...item, isActive: location.pathname === paths.masterDashboard }));

  return <AppSidebar items={items} label="マスターのメインナビゲーション" collapsed={collapsed} onNavigate={onNavigate} />;
}
