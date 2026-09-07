import { useLocation, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/contexts/AuthContext';
import { getHomePath } from '@/utils/getHomePath';

const NOT_FOUND_ILLUSTRATION_PATH = '/assets/errors/not-found.png';

type NotFoundPageProps = {
  onBeforeNavigate?: () => void;
};

/** Displays the shared Figma-aligned 404 content for missing or unavailable pages. */
export function NotFoundPage({
  onBeforeNavigate,
}: Readonly<NotFoundPageProps>) {
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const homePath = getHomePath(user);

  /** Returns to browser history when available and otherwise uses the role home. */
  const handleBack = () => {
    onBeforeNavigate?.();

    if (location.key === 'default') {
      navigate(homePath, { replace: true });
      return;
    }

    navigate(-1);
  };

  /** Returns to the role-appropriate home without preserving the invalid entry. */
  const handleReturnHome = () => {
    onBeforeNavigate?.();
    navigate(homePath, { replace: true });
  };

  return (
    <main className="flex h-[calc(100dvh-5rem)] items-center justify-center overflow-hidden bg-background px-4 py-10 sm:px-6 lg:px-8">
      <div className="mx-auto flex w-full max-w-[1120px] flex-col items-center gap-3 sm:gap-4">
        <img
          src={NOT_FOUND_ILLUSTRATION_PATH}
          alt=""
          aria-hidden="true"
          className="-my-10 h-auto w-[min(65vw,22rem)] object-contain"
        />

        <div className="flex flex-col items-center gap-6 lg:flex-row lg:gap-12">
          <p
            aria-hidden="true"
            className="text-[clamp(6rem,18vw,12.5rem)] font-bold leading-[0.8] tracking-[-0.04em] text-foreground"
          >
            404
          </p>

          <div className="flex max-w-2xl flex-col items-center gap-4 text-center lg:items-start lg:text-left">
            <div className="space-y-2">
              <h1 className="text-3xl font-bold tracking-tight text-foreground sm:text-4xl lg:text-5xl">
                ページが見つかりません
              </h1>
              <p className="text-sm leading-6 text-foreground sm:text-base lg:text-xl lg:leading-7">
                お探しのページは削除されたか、URLが変更された可能性があります。
              </p>
            </div>

            <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row sm:gap-3">
              <Button type="button" size="lg" onClick={handleBack}>
                前のページに戻る
              </Button>
              <Button
                type="button"
                size="lg"
                variant="outline"
                onClick={handleReturnHome}
              >
                ホームに戻る
              </Button>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
