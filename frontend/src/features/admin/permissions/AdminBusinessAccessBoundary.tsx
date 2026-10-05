import type { ReactNode } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Card, CardContent } from '@/components/ui/card';
import { LoadingPanel } from '@/components/feedback/loading';
import { ForbiddenAccessPage } from '@/components/feedback/ForbiddenAccessPage';
import { paths } from '@/routes/paths';
import { useAdminPermissions } from './useAdminPermissions';
import { ActionButton } from '@/components/actions';

/** Protects admin business routes while keeping self-profile flows available. */
export function AdminBusinessAccessBoundary({ children }: { children: ReactNode }) {
  const location = useLocation();
  const navigate = useNavigate();
  const permissions = useAdminPermissions();
  const selfProfilePath = permissions.operatorId ? paths.adminOperatorProfile(permissions.operatorId) : null;
  const selfEditPath = permissions.operatorId ? paths.adminOperatorEdit(permissions.operatorId) : null;
  const isSelfProfileFlow = selfProfilePath === location.pathname || selfEditPath === location.pathname;

  if (permissions.isLoading) return <LoadingPanel message="権限情報を読み込んでいます" />;

  if (permissions.isError) {
    return (
      <main className="mx-auto w-full max-w-lg px-4 py-12 sm:px-6">
        <Card>
          <CardContent className="p-8 text-center">
            <p className="font-semibold text-foreground">権限情報を取得できませんでした。</p>
            <p className="mt-2 text-small text-muted-foreground">通信状況を確認して、もう一度お試しください。</p>
            <ActionButton kind="retry" className="mt-6" onClick={() => void permissions.retry()} />
          </CardContent>
        </Card>
      </main>
    );
  }

  if (!permissions.hasAnyPermission && !isSelfProfileFlow) {
    return (
      <ForbiddenAccessPage
        actionLabel="プロファイルを見る"
        onAction={() => {
          if (selfProfilePath) navigate(selfProfilePath, { replace: true });
        }}
      />
    );
  }

  return children;
}
