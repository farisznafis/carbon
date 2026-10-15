import { useCallback, useEffect, useLayoutEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { ScrollToTopButton } from '@/components/navigation/ScrollToTopButton';
import { paths } from '@/routes/paths';

const SCROLL_VISIBLE_THRESHOLD = 8;

/**
 * Routes whose sidebar destinations are ?tab= views of one page. Their tabs
 * count as separate pages; in-page tabs elsewhere keep their scroll position.
 */
const SIDEBAR_TAB_ROUTES: ReadonlySet<string> = new Set([paths.masterDashboard]);

type ScrollToTopProps = {
  onRouteScrollReset?: () => void;
};

/** Resets the document scroll position without an animated route transition. */
function resetDocumentScrollPosition() {
  window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  document.documentElement.scrollTop = 0;
  document.body.scrollTop = 0;
}

export const ScrollToTop = ({ onRouteScrollReset }: ScrollToTopProps) => {
  const { pathname, search } = useLocation();
  const pageKey = SIDEBAR_TAB_ROUTES.has(pathname)
    ? `${pathname}?tab=${new URLSearchParams(search).get('tab') ?? ''}`
    : pathname;
  const [isVisible, setIsVisible] = useState(false);

  /** Resets both the native document position and an optional smooth-scroll controller. */
  const resetRouteScrollPosition = useCallback(() => {
    resetDocumentScrollPosition();
    onRouteScrollReset?.();
  }, [onRouteScrollReset]);

  useLayoutEffect(() => {
    resetRouteScrollPosition();
    setIsVisible(false);

    const animationFrameId = window.requestAnimationFrame(
      resetRouteScrollPosition,
    );

    return () => window.cancelAnimationFrame(animationFrameId);
  }, [pageKey, resetRouteScrollPosition]);

  useEffect(() => {
    const previousScrollRestoration = window.history.scrollRestoration;
    window.history.scrollRestoration = 'manual';

    return () => {
      window.history.scrollRestoration = previousScrollRestoration;
    };
  }, []);

  useEffect(() => {
    const updateVisibility = () => {
      setIsVisible(window.scrollY > SCROLL_VISIBLE_THRESHOLD);
    };

    updateVisibility();
    window.addEventListener('scroll', updateVisibility, { passive: true });

    return () => {
      window.removeEventListener('scroll', updateVisibility);
    };
  }, []);

  const handleClick = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return <ScrollToTopButton onClick={handleClick} isVisible={isVisible} />;
};
