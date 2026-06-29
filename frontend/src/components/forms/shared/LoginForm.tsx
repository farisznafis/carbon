import { ReactNode } from 'react';
import { LogIn } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useRedirectIfLoggedIn } from '@/hooks/useRedirectIfLoggedIn';

export type LoginFormProps = {
  title?: string;
  description?: string;
  submitLabel?: string;
  error?: string | null;
  isSubmitting?: boolean;
  children: ReactNode;
  footer?: ReactNode;
  header?: ReactNode;
};

export function LoginForm({
  title = '管理者ログイン',
  // description = 'GXデータの登録・管理を続けるにはログインしてください。',
  description = '',
  submitLabel = '管理者ログイン',
  error = null,
  isSubmitting = false,
  children,
  footer,
  header,
}: LoginFormProps) {
  useRedirectIfLoggedIn();

  return (
    <main className="min-h-screen bg-background px-4 py-16 sm:px-6 lg:px-8">
      <div className="mx-auto flex w-full max-w-[768px] flex-col items-center">
        {header ?? (
          <header className="text-center">
            <div className="mx-auto mb-5 flex h-12 w-12 items-center justify-center rounded-xl bg-secondary text-primary">
              <LogIn className="h-6 w-6" />
            </div>
            <h2>{title}</h2>
            <p className="mt-3 text-sm font-medium leading-6 text-muted-foreground">
              {description}
            </p>
          </header>
        )}

        <div className="mt-10 w-full rounded-xl border border-border bg-card p-8 shadow-app-sm sm:p-12">
          {error && (
            <p className="mb-5 rounded-lg bg-destructive/10 px-4 py-3 text-sm font-medium text-destructive">
              {error}
            </p>
          )}

          <div className="space-y-6">
            {children}

            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? 'ログイン中...' : submitLabel}
            </Button>
          </div>

          {footer && <div className="mt-5 text-center">{footer}</div>}
        </div>
      </div>
    </main>
  );
}

export default LoginForm;
