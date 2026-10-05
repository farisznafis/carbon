import type { ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import type { AdminOperatorPermissionCode } from '@/api/admin/operators';
import { adminPageContainerClassName } from '@/components/layout/DashboardLayout';
import { PageBackButton } from '@/components/navigation/PageBackButton';
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

/** Prevents protected page requests while retaining a usable page-level warning layout. */
export function AdminPermissionRoute({
  permission,
  title,
  description,
  children,
  allowSelfOperator = false,
}: Readonly<AdminPermissionRouteProps>) {
  const navigate = useNavigate();
  const { userId } = useParams<{ userId?: string }>();
  const access = useAdminPermissions();
  const isSelfOperator = allowSelfOperator && Boolean(userId) && userId === access.operatorId;

  if (access.hasPermission(permission) || isSelfOperator) return children;

  return (
    <main className={`${adminPageContainerClassName} py-8`}>
      <PageBackButton onClick={() => navigate(-1)} aria-label="前のページに戻る" />
      <div className="mx-auto mt-6 max-w-5xl space-y-6">
        <nav aria-label="パンくずリスト" className="text-small text-muted-foreground">
          <Link to={paths.adminDashboard} className="hover:text-foreground">ホーム</Link>
          <span aria-hidden="true" className="px-2">/</span>
          <span aria-current="page">{title}</span>
        </nav>
        <header>
          <h1 className="text-form-title font-bold text-foreground">{title}</h1>
          <p className="mt-2 text-body text-muted-foreground">{description}</p>
        </header>
        <PermissionDeniedNotice permission={permission} />
      </div>
    </main>
  );
}
