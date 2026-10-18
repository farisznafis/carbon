import type { ReactNode } from 'react';
import { useRef } from 'react';
import { useGSAP } from '@gsap/react';
import gsap from 'gsap';
import { ArrowLeft } from 'lucide-react';
import { paths } from '@/routes/paths';

gsap.registerPlugin(useGSAP);

type AuthPageLayoutProps = {
  title: string;
  description?: string;
  error?: string | null;
  headerIcon?: ReactNode;
  preserveHeaderOnShortViewport?: boolean;
  children: ReactNode;
  footer?: ReactNode;
};

function AuthAmbientBackground() {
  return (
    <div aria-hidden="true" className="absolute inset-0 overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-br from-primary/70 via-secondary to-background" />
      <div className="animate-ambient-drift absolute -left-[12%] bottom-[-35%] h-[72%] w-[62%] rounded-full bg-primary/45 blur-3xl" />
      <div className="animate-ambient-drift-slow absolute -right-[14%] top-[-30%] h-[68%] w-[58%] rounded-full bg-card/70 blur-3xl" />
      <div className="absolute inset-x-[18%] bottom-[-30%] h-[58%] rounded-full bg-accent/80 blur-3xl" />
    </div>
  );
}

function AuthPageHeader() {
  return (
    <header className="px-5 py-5 sm:px-8 sm:py-7 lg:px-10">
      <a
        href={paths.home}
        className="inline-flex items-center gap-2 text-small font-bold text-foreground transition-colors hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        <span className="hidden sm:inline">ホームへ</span>
      </a>
    </header>
  );
}

/** Renders the shared authentication card within the available viewport. */
export function AuthPageLayout({
  title,
  description,
  error,
  headerIcon,
  preserveHeaderOnShortViewport = false,
  children,
  footer,
}: AuthPageLayoutProps) {
  const layoutRef = useRef<HTMLElement>(null);
  const pageHeaderRef = useRef<HTMLElement>(null);
  const cardRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);

  useGSAP(
    () => {
      const prefersReducedMotion =
        typeof window.matchMedia === 'function' &&
        window.matchMedia('(prefers-reduced-motion: reduce)').matches;

      if (prefersReducedMotion) return;

      const timeline = gsap.timeline({ defaults: { ease: 'power3.out' } });

      if (pageHeaderRef.current) {
        timeline.from(pageHeaderRef.current, {
          y: -12,
          opacity: 0,
          duration: 0.45,
        });
      }

      if (cardRef.current) {
        timeline.from(
          cardRef.current,
          { y: 28, scale: 0.98, opacity: 0, duration: 0.65 },
          '<0.08',
        );
      }

      if (contentRef.current?.children.length) {
        timeline.from(
          contentRef.current.children,
          { y: 10, opacity: 0, duration: 0.35, stagger: 0.055 },
          '-=0.34',
        );
      }
    },
    { scope: layoutRef },
  );

  return (
    <main
      ref={layoutRef}
      className="relative flex h-full min-h-0 w-full min-w-0 max-w-full items-center justify-center overflow-hidden"
    >
      <AuthAmbientBackground />
      {/* <div ref={pageHeaderRef} className="absolute inset-x-0 top-0 z-20">
        <AuthPageHeader />
      </div> */}

      <div className="relative z-10 flex max-h-full w-full items-center justify-center px-4 pb-5 pt-20 sm:px-6 sm:pb-8 sm:pt-24 [@media(max-height:560px)]:pb-3 [@media(max-height:560px)]:pt-16">
        <div
          ref={cardRef}
          className="w-full max-w-[480px] rounded-lg border border-border/70 bg-card/95 p-6 shadow-app-md backdrop-blur-md sm:p-9 lg:p-[calc(var(--card-padding)*1.5)] [@media(max-height:700px)]:max-w-[460px] [@media(max-height:700px)]:p-5 [@media(max-height:560px)]:p-4"
        >
          <header
            className={
              preserveHeaderOnShortViewport
                ? 'text-g'
                : 'text-g [@media(max-height:620px)]:sr-only'
            }
          >
            {headerIcon && (
              <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-lg bg-secondary text-primary [@media(max-height:700px)]:mb-3 [@media(max-height:700px)]:h-14 [@media(max-height:700px)]:w-14">
                {headerIcon}
              </div>
            )}
            <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-form-title">
              {title}
            </h1>
            {description && (
              <p className="mt-2 max-w-sm text-small text-muted-foreground">
                {description}
              </p>
            )}
          </header>

          {error && (
            <p
              role="alert"
              className="mt-5 rounded-lg border border-destructive/20 bg-destructive/10 px-4 py-3 text-sm font-medium text-destructive [@media(max-height:700px)]:mt-3 [@media(max-height:700px)]:py-2"
            >
              {error}
            </p>
          )}

          <div
            ref={contentRef}
            className="mt-7 space-y-5 [@media(max-height:700px)]:mt-4 [@media(max-height:700px)]:space-y-3 [@media(max-height:620px)]:mt-0"
          >
            {children}
          </div>

          {footer && (
            <div className="mt-5 text-center [@media(max-height:700px)]:mt-3">
              {footer}
            </div>
          )}
        </div>
      </div>
    </main>
  );
}
