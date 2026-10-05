import { BarChart3, Building2, ClipboardList, House, Users } from 'lucide-react';
import { paths } from '@/routes/paths';

export type MasterSidebarKey = 'home' | 'analytics' | 'membership' | 'sites' | 'users';

export const MASTER_SIDEBAR_ITEMS = [
  { key: 'home', label: 'ホーム', href: paths.masterDashboard, icon: House },
  { key: 'analytics', label: 'アナリティクス', href: `${paths.masterDashboard}?tab=analytics`, icon: BarChart3 },
  { key: 'membership', label: 'プロジェクトの入会届', href: paths.masterDashboardMembership, icon: ClipboardList },
  { key: 'sites', label: '拠点', href: `${paths.masterDashboard}?tab=sites`, icon: Building2 },
  { key: 'users', label: '社員', href: `${paths.masterDashboard}?tab=users`, icon: Users },
] as const;

/** Resolves known dashboard tabs while keeping old and empty URLs on Home. */
export function getMasterDashboardTab(tab: string | null): MasterSidebarKey {
  if (tab === 'analytics' || tab === 'membership' || tab === 'sites' || tab === 'users') return tab;
  return 'home';
}
