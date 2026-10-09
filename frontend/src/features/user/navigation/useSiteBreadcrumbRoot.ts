import type { PageBreadcrumbItem } from '@/components/navigation/PageBreadcrumb';
import { useOptionalAuth } from '@/contexts/AuthContext';
import { paths } from '@/routes/paths';

/**
 * The start of a trail for pages under one site's dashboard. A user's site
 * dashboard is their home; a master reaches it from the 拠点 tab, so their
 * trail names the site too.
 */
export function useSiteBreadcrumbRoot(
  siteId: string | undefined,
  siteName?: string,
): PageBreadcrumbItem[] {
  const user = useOptionalAuth()?.user;
  const siteDashboard = siteId ? paths.siteDashboard(siteId) : undefined;

  if (user?.role === 'master') {
    return [
      { label: '拠点', to: `${paths.masterDashboard}?tab=sites` },
      { label: siteName?.trim() && siteName !== '-' ? siteName : '拠点ダッシュボード', to: siteDashboard },
    ];
  }

  return [{ label: 'ホーム', to: siteDashboard ?? paths.userDashboard }];
}
