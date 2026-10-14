import type { ReactNode } from 'react';
import { matchPath, useLocation, useParams } from 'react-router-dom';
import type { AdminOperatorPermissionCode } from '@/api/admin/operators';
import { adminPageContainerClassName } from '@/components/layout/DashboardLayout';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import {
  ADMIN_NAVIGATION_ITEMS,
  getActiveAdminNavigationKey,
} from '@/features/admin/navigation/adminNavigationConfig';
import { useAdminProjectDetailQuery } from '@/hooks/admin/useAdminProjectDetailQuery';
import { PermissionDeniedNotice } from './PermissionDeniedNotice';
import { useAdminPermissions } from './useAdminPermissions';
import { paths } from '@/routes/paths';

type AdminPermissionRouteProps = {
  permission: AdminOperatorPermissionCode;
  title: string;
  description: string;
  children: ReactNode;
  allowSelfOperator?: boolean;
};

type PermissionDeniedPageProps = Pick<
  AdminPermissionRouteProps,
  'permission' | 'title' | 'description'
>;

/**
 * Declares its own breadcrumb only when access is denied; the wrapper must not,
 * or it would overwrite the trail of the page it guards.
 */
function PermissionDeniedPage({
  permission,
  title,
  description,
}: Readonly<PermissionDeniedPageProps>) {
  // Starts from the section the sidebar highlights, plus the project when the page sits under one.
  const { pathname } = useLocation();
  const activeKey = getActiveAdminNavigationKey(pathname) ?? 'home';
  const section = ADMIN_NAVIGATION_ITEMS.find((item) => item.key === activeKey)!;
  const projectMatch = matchPath(`${paths.adminProjectDetailRoute}/*`, pathname);
  // Only pages below the project detail get the project crumb; the detail page itself is the title.
  const projectId = projectMatch?.params['*'] ? projectMatch.params.projectId : undefined;
  const projectName = useAdminProjectDetailQuery(projectId).data?.projectName;
  usePageBreadcrumb([
    { label: section.label, to: section.href },
    ...(projectId
      ? [{ label: projectName || 'プロジェクト詳細', to: paths.adminProjectDetail(projectId) }]
      : []),
    { label: title },
  ]);

  return (
    <main className={`${adminPageContainerClassName} py-8`}>
      <div className="mx-auto max-w-5xl space-y-6">
        <header>
          <h1 className="text-form-title font-bold text-foreground">{title}</h1>
          <p className="mt-2 text-body text-muted-foreground">{description}</p>
        </header>
        <PermissionDeniedNotice permission={permission} />
      </div>
    </main>
  );
}

/** Prevents protected page requests while retaining a usable page-level warning layout. */
export function AdminPermissionRoute({
  permission,
  title,
  description,
  children,
  allowSelfOperator = false,
}: Readonly<AdminPermissionRouteProps>) {
  const { userId } = useParams<{ userId?: string }>();
  const access = useAdminPermissions();
  const isSelfOperator = allowSelfOperator && Boolean(userId) && userId === access.operatorId;

  if (access.hasPermission(permission) || isSelfOperator) return children;

  return (
    <PermissionDeniedPage
      permission={permission}
      title={title}
      description={description}
    />
  );
}
