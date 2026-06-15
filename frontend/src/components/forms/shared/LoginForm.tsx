import { Button } from '@/components/ui/button';
import { useRedirectIfLoggedIn } from '@/hooks/useRedirectIfLoggedIn';
import { ReactNode } from 'react';

export type LoginFormProps = {
  title?: string;
  submitLabel?: string;
  error?: string | null;
  isSubmitting?: boolean;
  children: ReactNode;
  footer?: ReactNode;
};

export function LoginForm({
  title = 'ログイン',
  submitLabel = 'ログイン',
  error = null,
  isSubmitting = false,
  children,
  footer,
}: LoginFormProps) {
  useRedirectIfLoggedIn();

  return (
    <div className='min-h-screen'>
      <div className='bg-primary text-primary-foreground py-6 px-4'>
        <div className='max-w-md mx-auto'>
          <h1 className='text-3xl font-bold text-center'>{title}</h1>
        </div>
      </div>

      <div className='flex items-center justify-center px-4 py-12'>
        <div className='w-full max-w-md animate-slide-up'>
          {error && <p className='mb-4 text-sm text-red-600'>{error}</p>}

          <div className='space-y-4'>
            {children}

            <Button type='submit' className='w-full' disabled={isSubmitting}>
              {isSubmitting ? 'ログイン中...' : submitLabel}
            </Button>
          </div>

          {footer && <div className='mt-6'>{footer}</div>}
        </div>
      </div>
    </div>
  );
}

export default LoginForm;
