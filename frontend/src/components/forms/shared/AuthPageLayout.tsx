import type { ReactNode } from 'react';
import { useRef } from 'react';
import { useGSAP } from '@gsap/react';
import gsap from 'gsap';

gsap.registerPlugin(useGSAP);

type AuthPageLayoutProps = {
  title: string;
  description?: string;
  error?: string | null;
  headerIcon?: ReactNode;
  children: ReactNode;
  footer?: ReactNode;
};

/** Soft brand gradient with slowly drifting light blobs (static under reduced motion). */
function AuthAmbientBackground() {
  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-br from-[hsl(var(--blue-100))] via-background to-[hsl(var(--blue-50))]" />
      <div className="animate-ambient-drift absolute -left-[10%] -top-[20%] h-[70%] w-[50%] rounded-full bg-primary/30 blur-3xl" />
      <div className="animate-ambient-drift-slow absolute -bottom-[25%] -right-[10%] h-[75%] w-[55%] rounded-full bg-[hsl(var(--blue-300))]/40 blur-3xl" />
      <div className="animate-ambient-drift-slow absolute left-[30%] top-[40%] h-[45%] w-[40%] rounded-full bg-[hsl(var(--blue-200))]/50 blur-3xl" />
    </div>
  );
}

/** Renders the shared authentication card centered over the ambient gradient. */
export function AuthPageLayout({
  title,
  description,
  error,
  headerIcon,
  children,
  footer,
}: AuthPageLayoutProps) {
  const layoutRef = useRef<HTMLElement>(null);
  const cardRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);

  useGSAP(
    () => {
      const prefersReducedMotion =
        typeof window.matchMedia === 'function' &&
        window.matchMedia('(prefers-reduced-motion: reduce)').matches;

      if (prefersReducedMotion) return;

      const timeline = gsap.timeline({ defaults: { ease: 'power3.out' } });

      if (cardRef.current) {
        timeline.from(cardRef.current, { y: 16, opacity: 0, duration: 0.5 });
      }

      if (contentRef.current?.children.length) {
        timeline.from(
          contentRef.current.children,
          { y: 8, opacity: 0, duration: 0.3, stagger: 0.05 },
          '-=0.3',
        );
      }
    },
    { scope: layoutRef },
  );

  return (
    <main
      ref={layoutRef}
      className="relative flex h-full min-h-0 w-full min-w-0 max-w-full overflow-hidden"
    >
      <AuthAmbientBackground />

      {/* Scrolls instead of hiding content when the viewport is short. */}
      <div className="relative z-10 flex min-h-0 w-full overflow-y-auto px-4 py-8 sm:px-6">
        <div
          ref={cardRef}
          className="m-auto w-full max-w-[30rem] rounded-lg border border-white/70 bg-card/90 p-6 shadow-app-md backdrop-blur-md sm:p-8"
        >
          <header>
            {headerIcon && (
              <div className="mb-5 flex h-14 w-14 items-center justify-center rounded-lg bg-secondary text-primary">
                {headerIcon}
              </div>
            )}
            <h1 className="text-form-title font-bold tracking-tight text-foreground">
              {title}
            </h1>
            {description && (
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                {description}
              </p>
            )}
          </header>

          {error && (
            <p
              role="alert"
              className="mt-5 rounded-lg border border-destructive/20 bg-destructive/10 px-4 py-3 text-sm font-medium text-destructive"
            >
              {error}
            </p>
          )}

          <div ref={contentRef} className="mt-7 space-y-5">
            {children}
          </div>

          {footer && <div className="mt-6 text-center">{footer}</div>}
        </div>
      </div>
    </main>
  );
}
