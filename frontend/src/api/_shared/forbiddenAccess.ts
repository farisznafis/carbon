export const PAGE_FORBIDDEN_EVENT = 'gx:page-forbidden';

export type ForbiddenHandling = 'page' | 'caller';

export type PageForbiddenEventDetail = {
  routeKey: string;
  requestPath: string;
};

export type AdminPermissionScope =
  | 'projects'
  | 'companies'
  | 'operators'
  | 'methodologies'
  | 'monitoring';

/** Returns the browser route that owns an outgoing page request. */
export function getCurrentBrowserRouteKey(): string {
  if (typeof window === 'undefined') return '';

  return `${window.location.pathname}${window.location.search}`;
}

/** Resolves which admin permission area owns an API endpoint. */
export function getAdminRequestPermissionScope(
  requestPath: string,
): AdminPermissionScope | null {
  if (requestPath.startsWith('/admin/companies/operator-user')) {
    return 'operators';
  }

  if (
    requestPath.startsWith('/admin/companies/participation') ||
    requestPath.startsWith('/admin/companies/participate')
  ) {
    return 'projects';
  }

  if (requestPath.startsWith('/admin/companies')) {
    return 'companies';
  }

  if (
    requestPath.startsWith('/admin/methodologies') ||
    requestPath.startsWith('/methodology') ||
    requestPath.startsWith('/methodologies')
  ) {
    return 'methodologies';
  }

  if (
    requestPath.startsWith('/admin/monitoring') ||
    requestPath.startsWith('/monitoring/cancel-requests') ||
    requestPath.startsWith('/batch/history')
  ) {
    return 'monitoring';
  }

  if (
    requestPath.startsWith('/admin/projects') ||
    requestPath.startsWith('/project') ||
    requestPath.startsWith('/projectplan') ||
    /^\/monitoring\/[^/]+\/list(?:\?|$)/.test(requestPath)
  ) {
    return 'projects';
  }

  if (
    requestPath.startsWith('/monitoring') ||
    requestPath.startsWith('/batch')
  ) {
    return 'monitoring';
  }

  return null;
}

/** Notifies the application shell that a permission-scoped page request was forbidden. */
export function reportPageForbidden(
  detail: PageForbiddenEventDetail,
): void {
  if (typeof window === 'undefined') return;

  window.dispatchEvent(
    new CustomEvent<PageForbiddenEventDetail>(PAGE_FORBIDDEN_EVENT, {
      detail,
    }),
  );
}
