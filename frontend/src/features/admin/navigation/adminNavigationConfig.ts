import { paths } from '@/routes/paths';
import { BookOpen, Building2, FolderKanban, House, Users, type LucideIcon } from 'lucide-react';

export type AdminNavigationKey =
  | 'home'
  | 'projects'
  | 'companies'
  | 'operators'
  | 'methodologies';

export type AdminNavigationItem = {
  key: AdminNavigationKey;
  label: string;
  href: string;
  icon: LucideIcon;
};

export const ADMIN_NAVIGATION_ITEMS: readonly AdminNavigationItem[] = [
  { key: 'home', label: 'ホーム', href: paths.adminDashboard, icon: House },
  { key: 'projects', label: 'プロジェクト', href: paths.adminProjectList, icon: FolderKanban },
  { key: 'methodologies', label: '方法論', href: paths.adminMethodologyList, icon: BookOpen },
  { key: 'companies', label: '企業', href: paths.adminCompanyList, icon: Building2 },
  { key: 'operators', label: '運営者', href: paths.adminOperatorList, icon: Users },
] as const;

const LEGACY_TAB_NAVIGATION_MAP: Partial<
  Record<string, AdminNavigationKey>
> = {
  overview: 'home',
  projects: 'projects',
  companies: 'companies',
  operators: 'operators',
  methodologies: 'methodologies',
  monitoring: 'home',
};

/** Resolves the active admin navigation item for list and nested detail routes. */
export function getActiveAdminNavigationKey(
  pathname: string,
  legacyTab?: string | null,
): AdminNavigationKey | null {
  if (pathname === paths.adminDashboard && legacyTab) {
    return LEGACY_TAB_NAVIGATION_MAP[legacyTab] ?? 'home';
  }

  if (pathname === paths.adminDashboard) return 'home';

  if (
    pathname === paths.adminProjectList ||
    pathname === paths.adminProjectRegister ||
    pathname.startsWith(`${paths.adminProjectList}/`)
  ) {
    return 'projects';
  }

  if (
    pathname === paths.adminCompanyList ||
    pathname.startsWith(`${paths.adminCompanyList}/`) ||
    pathname.startsWith('/admin/applications/')
  ) {
    return 'companies';
  }

  if (
    pathname === paths.adminOperatorList ||
    pathname.startsWith(`${paths.adminOperatorList}/`)
  ) {
    return 'operators';
  }

  if (pathname === paths.adminMethodologyList) return 'methodologies';

  if (
    pathname === paths.adminMonitoring ||
    pathname.startsWith('/admin/monitoring/')
  ) {
    return 'home';
  }

  return null;
}

/** Converts a legacy dashboard tab into its new route while preserving old bookmarks. */
export function getAdminRouteFromLegacyTab(tab: string | null): string | null {
  if (tab === 'monitoring') return `${paths.adminDashboard}?panel=cancel`;

  const key = tab ? LEGACY_TAB_NAVIGATION_MAP[tab] : undefined;
  return ADMIN_NAVIGATION_ITEMS.find((item) => item.key === key)?.href ?? null;
}
