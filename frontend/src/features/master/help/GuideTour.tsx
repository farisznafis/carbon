import { useEffect, useId, useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import * as PopoverPrimitive from '@radix-ui/react-popover';
import { Button } from '@/components/ui/button';
import { Popover, PopoverContent } from '@/components/ui/popover';
import { GUIDE_CONTENT, type GuideContext, type GuideStep } from './guideSteps';

/** How long to wait for a page's tour targets to render before giving up. */
const TARGET_WAIT_MS = 3000;
const TARGET_POLL_MS = 100;
/** How long the found targets must stay unchanged before the tour starts. */
const TARGET_STABLE_MS = 400;
/** Space between the highlighted element and the spotlight edge. */
const SPOTLIGHT_PADDING = 6;
/** Targets taller than this share of the viewport are tall sections. */
const TALL_TARGET_RATIO = 0.6;
/** Gap between a tall target's bottom edge and the explanation pinned inside it. */
const PINNED_INSET = 16;

type Rect = { top: number; left: number; width: number; height: number };

/** Finds the rendered copy of a target; the sidebar also exists inside the closed mobile drawer. */
function findTarget(target: string): HTMLElement | null {
  const elements = document.querySelectorAll<HTMLElement>(`[data-tour="${target}"]`);
  for (const element of elements) {
    const rect = element.getBoundingClientRect();
    if (rect.width > 0 && rect.height > 0) return element;
  }
  return null;
}

/** Keeps the spotlight on screen even when the target is taller than the viewport. */
function toSpotlightRect(element: HTMLElement): Rect {
  const rect = element.getBoundingClientRect();
  const top = Math.max(rect.top - SPOTLIGHT_PADDING, SPOTLIGHT_PADDING);
  const left = Math.max(rect.left - SPOTLIGHT_PADDING, SPOTLIGHT_PADDING);
  const bottom = Math.min(rect.bottom + SPOTLIGHT_PADDING, window.innerHeight - SPOTLIGHT_PADDING);
  const right = Math.min(rect.right + SPOTLIGHT_PADDING, window.innerWidth - SPOTLIGHT_PADDING);
  return { top, left, width: Math.max(right - left, 0), height: Math.max(bottom - top, 0) };
}

function sameRect(a: Rect | null, b: Rect) {
  return (
    a !== null &&
    a.top === b.top &&
    a.left === b.left &&
    a.width === b.width &&
    a.height === b.height
  );
}

function prefersReducedMotion() {
  return (
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );
}

/** Scrolls only when needed, so elements in the sticky sidebar and topbar stay put. */
function revealTarget(element: HTMLElement) {
  const rect = element.getBoundingClientRect();
  const fullyVisible = rect.top >= 0 && rect.bottom <= window.innerHeight;
  if (fullyVisible) return;

  element.scrollIntoView({
    behavior: prefersReducedMotion() ? 'auto' : 'smooth',
    // Tall sections start at their heading; small elements are centred.
    block: rect.height > window.innerHeight * TALL_TARGET_RATIO ? 'start' : 'center',
  });
}

/**
 * Resolves once the page has rendered: the first step's target exists and the found targets
 * stop changing, so shared targets (sidebar, header) do not start the tour while content loads.
 * Steps whose targets never appear (role-dependent buttons) are dropped.
 */
function useAvailableSteps(steps: readonly GuideStep[]) {
  const [available, setAvailable] = useState<GuideStep[] | null>(null);

  useEffect(() => {
    const startedAt = Date.now();
    let lastKey = '';
    let stableSince = startedAt;
    const timer = window.setInterval(() => {
      const now = Date.now();
      const found = steps.filter((step) => findTarget(step.target));
      const key = found.map((step) => step.target).join('|');
      if (key !== lastKey) {
        lastKey = key;
        stableSince = now;
      }
      const pageReady =
        found[0] === steps[0] &&
        (found.length === steps.length || now - stableSince >= TARGET_STABLE_MS);
      if (pageReady || now - startedAt > TARGET_WAIT_MS) {
        window.clearInterval(timer);
        setAvailable(found);
      }
    }, TARGET_POLL_MS);

    return () => window.clearInterval(timer);
  }, [steps]);

  return available;
}

/** Follows the target's position every frame, so scrolling and layout changes move the spotlight. */
function useTargetRect(target: string | undefined) {
  const [rect, setRect] = useState<Rect | null>(null);

  useLayoutEffect(() => {
    if (!target) return;

    let frame = 0;
    let element = findTarget(target);
    if (element) revealTarget(element);

    const track = () => {
      if (!element?.isConnected) element = findTarget(target);
      if (element) {
        const next = toSpotlightRect(element);
        setRect((current) => (sameRect(current, next) ? current : next));
      }
      frame = window.requestAnimationFrame(track);
    };
    track();

    return () => window.cancelAnimationFrame(frame);
  }, [target]);

  return rect;
}

type GuideTourProps = {
  context: GuideContext;
  onClose: () => void;
};

/**
 * Walks through one page: everything is dimmed except the element being
 * explained, with the explanation beside it. Escape or スキップ ends it.
 */
export function GuideTour({ context, onClose }: Readonly<GuideTourProps>) {
  const steps = useAvailableSteps(GUIDE_CONTENT[context].steps);
  const [index, setIndex] = useState(0);
  const step = steps?.[index];
  const rect = useTargetRect(step?.target);
  const nextButtonRef = useRef<HTMLButtonElement>(null);
  const titleId = useId();
  const descriptionId = useId();

  // Nothing on this page matches the guide (e.g. it changed or is still empty).
  useEffect(() => {
    if (steps?.length === 0) onClose();
  }, [onClose, steps]);

  const hasRect = rect !== null;
  // Keep keyboard focus on the main action as the explanation moves between steps.
  useEffect(() => {
    nextButtonRef.current?.focus();
  }, [index, hasRect]);

  if (!steps || !step) return null;

  // A tall section leaves no room above or below, so the explanation would be pushed off
  // screen with its buttons; pin it inside the section's visible bottom edge instead.
  const pinned = rect !== null && rect.height > window.innerHeight * TALL_TARGET_RATIO;
  const anchorRect = rect && pinned
    ? { top: rect.top + rect.height - PINNED_INSET, left: rect.left + PINNED_INSET, width: rect.width - PINNED_INSET * 2, height: 0 }
    : rect;

  const isFirst = index === 0;
  const isLast = index === steps.length - 1;
  const goNext = () => (isLast ? onClose() : setIndex(index + 1));

  return createPortal(
    <>
      {/* Swallows clicks so the page is not used mid-tour; the spotlight itself casts the dimming. */}
      <div aria-hidden="true" className="fixed inset-0 z-[100]" />

      {rect && anchorRect && (
        <Popover open modal>
          <div
            aria-hidden="true"
            className="pointer-events-none fixed z-[100] rounded-lg transition-all duration-200 ease-out motion-reduce:transition-none"
            style={{
              top: rect.top,
              left: rect.left,
              width: rect.width,
              height: rect.height,
              // One shadow for both the outline and the dimming; a ring class would be overridden.
              boxShadow: '0 0 0 2px hsl(var(--primary)), 0 0 0 9999px rgb(0 0 0 / 0.6)',
            }}
          />
          <PopoverPrimitive.Anchor asChild>
            <div
              aria-hidden="true"
              className="pointer-events-none fixed"
              style={anchorRect}
            />
          </PopoverPrimitive.Anchor>

          <PopoverContent
            role="dialog"
            aria-labelledby={titleId}
            aria-describedby={descriptionId}
            side={pinned ? 'top' : (step.side ?? 'bottom')}
            align="start"
            sideOffset={12}
            collisionPadding={16}
            updatePositionStrategy="always"
            className="z-[101] w-[min(22rem,calc(100vw-2rem))] space-y-4 rounded-lg p-5"
            onOpenAutoFocus={(event) => {
              event.preventDefault();
              nextButtonRef.current?.focus();
            }}
            onEscapeKeyDown={onClose}
            onInteractOutside={(event) => event.preventDefault()}
          >
            <div className="space-y-1.5" aria-live="polite">
              <p className="text-xs font-medium tabular-nums text-primary-strong">
                {index + 1} / {steps.length}
              </p>
              <h2 id={titleId} className="text-base font-bold text-foreground">
                {step.title}
              </h2>
              <p id={descriptionId} className="text-sm leading-6 text-foreground">
                {step.description}
              </p>
            </div>

            <div className="flex items-center gap-2">
              <Button type="button" variant="ghost" size="sm" onClick={onClose} className="-ml-2">
                スキップ
              </Button>
              <div className="ml-auto flex gap-2">
                {!isFirst && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setIndex(index - 1)}
                  >
                    戻る
                  </Button>
                )}
                <Button ref={nextButtonRef} type="button" size="sm" onClick={goNext}>
                  {isLast ? '完了' : '次へ'}
                </Button>
              </div>
            </div>
          </PopoverContent>
        </Popover>
      )}
    </>,
    document.body,
  );
}
