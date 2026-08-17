import type { ReactNode } from 'react';
import { Button } from '@/components/ui/button';
import { useRedirectIfLoggedIn } from '@/hooks/useRedirectIfLoggedIn';
import { AuthPageLayout } from './AuthPageLayout';

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
  title = 'ログイン',
  description = 'GXデータの登録・管理を続けるにはログインしてください。',
  submitLabel = 'ログイン',
  error = null,
  isSubmitting = false,
  children,
  footer,
}: LoginFormProps) {
  useRedirectIfLoggedIn();

  return (
    <AuthPageLayout
      title={title}
      description={description}
      error={error}
      footer={footer}
    >
      {children}

      <Button
        type="submit"
        size="lg"
        className="w-full transition-transform duration-300 hover:scale-[1.01] active:scale-[0.99] [@media(max-height:560px)]:h-9"
        disabled={isSubmitting}
      >
        {isSubmitting ? 'ログイン中...' : submitLabel}
      </Button>
    </AuthPageLayout>
  );
}

export default LoginForm;
