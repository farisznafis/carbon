import type { ReactNode } from 'react';
import { FormActions } from '@/components/forms/form-layout';
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
  // description = 'GXデータの登録・管理を続けるにはログインしてください。',
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
      // description={description}
      error={error}
      footer={footer}
    >
      {children}

      <FormActions
        context="auth"
        pendingActionId={isSubmitting ? 'submit' : null}
        actions={[
          {
            id: 'submit',
            label: submitLabel,
            pendingLabel: 'ログイン中...',
            type: 'submit',
          },
        ]}
      />
    </AuthPageLayout>
  );
}

export default LoginForm;
