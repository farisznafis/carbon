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
}: LoginFormProps) {
  useRedirectIfLoggedIn();

  return (
    <main className="flex h-full min-h-0 w-full min-w-0 max-w-full items-center justify-center overflow-hidden bg-background px-4 sm:px-6 lg:px-8">
      <div className="mx-auto flex max-h-full w-full max-w-[768px] flex-col items-center">
        <header className="text-center [@media(max-height:480px)]:sr-only">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-secondary text-primary [@media(max-height:700px)]:hidden">
            <LogIn className="h-6 w-6" />
          </div>
          <h2>{title}</h2>
          <p className="mt-3 text-sm font-medium leading-6 text-foreground [@media(max-height:700px)]:hidden">
            {description}
          </p>
        </header>

        <div className="mt-10 w-full rounded-xl border border-border bg-card p-8 shadow-app-sm sm:p-12 [@media(max-height:700px)]:mt-4 [@media(max-height:700px)]:!p-5 [@media(max-height:480px)]:mt-0 [@media(max-height:480px)]:!p-2">
          {error && (
            <p className="mb-5 rounded-lg bg-destructive/10 px-4 py-3 text-sm font-medium text-destructive [@media(max-height:480px)]:mb-0.5 [@media(max-height:480px)]:px-2 [@media(max-height:480px)]:py-1">
              {error}
            </p>
          )}

          <div className="space-y-6 [@media(max-height:700px)]:space-y-3 [@media(max-height:480px)]:space-y-0.5">
            {children}

            <Button
              type="submit"
              className="w-full [@media(max-height:480px)]:h-8"
              disabled={isSubmitting}
            >
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
