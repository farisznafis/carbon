import { ShieldAlert } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { useAuth } from '@/contexts/AuthContext';
import { getHomePath } from '@/utils/getHomePath';

type ForbiddenAccessPageProps = {
  onReturnHome?: () => void;
  actionLabel?: string;
  onAction?: () => void;
};

/** Displays the shared in-content warning for a page-level 403 response. */
export function ForbiddenAccessPage({
  onReturnHome,
  actionLabel = 'ホームに戻る',
  onAction,
}: Readonly<ForbiddenAccessPageProps>) {
  const navigate = useNavigate();
  const { user } = useAuth();

  /** Returns to the role-appropriate home page from the access warning. */
  const handleReturnHome = () => {
    if (onAction) {
      onAction();
      return;
    }

    onReturnHome?.();
    navigate(getHomePath(user), { replace: true });
  };

  return (
    <main className="flex min-h-[calc(100dvh-5rem)] items-center justify-center px-4 py-10 sm:px-6">
      <Card className="w-full max-w-lg border-border shadow-sm">
        <CardContent className="flex flex-col items-center px-6 py-10 text-center sm:px-10">
          <div className="mb-6 flex h-14 w-14 items-center justify-center rounded-lg bg-secondary text-primary-strong">
            <ShieldAlert className="h-7 w-7" aria-hidden="true" />
          </div>
          <h1 className="text-2xl font-bold text-foreground">
            権限がありません
          </h1>
          <p className="mt-3 text-sm leading-6 text-muted-foreground sm:text-base">
            このページを表示する権限がありません。
            <br />
            必要な場合は管理者にお問い合わせください。
          </p>
          <Button type="button" className="mt-8 w-full" onClick={handleReturnHome}>
            {actionLabel}
          </Button>
        </CardContent>
      </Card>
    </main>
  );
}
