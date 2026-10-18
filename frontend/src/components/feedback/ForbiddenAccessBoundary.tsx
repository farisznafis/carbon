import { useEffect, useState, type ReactNode } from 'react';
import { useLocation } from 'react-router-dom';
import {
  getAdminRequestPermissionScope,
  PAGE_FORBIDDEN_EVENT,
  type PageForbiddenEventDetail,
} from '@/api/_shared/forbiddenAccess';
import { getActiveAdminNavigationKey } from '@/features/admin/navigation/adminNavigationConfig';
import { paths } from '@/routes/paths';
import { ForbiddenAccessPage } from './ForbiddenAccessPage';

type ForbiddenAccessBoundaryProps = {
  children: ReactNode;
};

/** Replaces only the route content when a page-loading request returns 403. */
export function ForbiddenAccessBoundary({
  children,
}: Readonly<ForbiddenAccessBoundaryProps>) {
  const location = useLocation();
  const [isForbidden, setIsForbidden] = useState(false);
  const currentRouteKey = `${location.pathname}${location.search}`;

  useEffect(() => {
    /** Switches the current route content to the shared forbidden warning. */
    const handlePageForbidden = (event: Event) => {
      const forbiddenEvent = event as CustomEvent<PageForbiddenEventDetail>;
      const belongsToCurrentRoute =
        forbiddenEvent.detail?.routeKey === currentRouteKey;
      const isAdminHome = location.pathname === paths.adminDashboard;
      const activeNavigationKey = getActiveAdminNavigationKey(
        location.pathname,
        new URLSearchParams(location.search).get('tab'),
      );
      const requestPermissionScope = getAdminRequestPermissionScope(
        forbiddenEvent.detail?.requestPath ?? '',
      );
      const belongsToActivePermissionPage =
        requestPermissionScope !== null &&
        requestPermissionScope === activeNavigationKey;

      if (
        belongsToCurrentRoute &&
        belongsToActivePermissionPage &&
        !isAdminHome
      ) {
        setIsForbidden(true);
      }
    };

    window.addEventListener(PAGE_FORBIDDEN_EVENT, handlePageForbidden);
    return () => {
      window.removeEventListener(PAGE_FORBIDDEN_EVENT, handlePageForbidden);
    };
  }, [currentRouteKey, location.pathname]);

  useEffect(() => {
    setIsForbidden(false);
  }, [location.key]);

  return isForbidden ? (
    <ForbiddenAccessPage onReturnHome={() => setIsForbidden(false)} />
  ) : (
    children
  );
}
