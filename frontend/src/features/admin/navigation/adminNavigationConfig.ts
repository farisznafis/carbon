import { paths } from '@/routes/paths';

export type AdminNavigationKey =
  | 'home'
  | 'projects'
  | 'companies'
  | 'operators'
  | 'methodologies'
  | 'monitoring';

export type AdminNavigationItem = {
  key: AdminNavigationKey;
  label: string;
  href: string;
};

export const ADMIN_NAVIGATION_ITEMS: readonly AdminNavigationItem[] = [
  { key: 'home', label: 'ホーム', href: paths.adminDashboard },
  { key: 'projects', label: 'プロジェクト', href: paths.adminProjectList },
  { key: 'companies', label: '企業管理', href: paths.adminCompanyList },
  { key: 'operators', label: '運営管理', href: paths.adminOperatorList },
  { key: 'methodologies', label: '方法論', href: paths.adminMethodologyList },
  { key: 'monitoring', label: 'モニタリング管理', href: paths.adminMonitoring },
] as const;

const LEGACY_TAB_NAVIGATION_MAP: Partial<
  Record<string, AdminNavigationKey>
> = {
  overview: 'home',
  projects: 'projects',
  companies: 'companies',
  operators: 'operators',
  methodologies: 'methodologies',
  monitoring: 'monitoring',
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
    return 'monitoring';
  }

  return null;
}

/** Converts a legacy dashboard tab into its new route while preserving old bookmarks. */
export function getAdminRouteFromLegacyTab(tab: string | null): string | null {
  const key = tab ? LEGACY_TAB_NAVIGATION_MAP[tab] : undefined;
  return ADMIN_NAVIGATION_ITEMS.find((item) => item.key === key)?.href ?? null;
}
