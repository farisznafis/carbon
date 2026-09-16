import { useEffect, useLayoutEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { ScrollToTopButton } from '@/components/navigation/ScrollToTopButton';

const SCROLL_VISIBLE_THRESHOLD = 8;

/** Resets the document scroll position without an animated route transition. */
function resetDocumentScrollPosition() {
  window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  document.documentElement.scrollTop = 0;
  document.body.scrollTop = 0;
}

export const ScrollToTop = () => {
  const { pathname } = useLocation();
  const [isVisible, setIsVisible] = useState(false);

  useLayoutEffect(() => {
    resetDocumentScrollPosition();
    setIsVisible(false);

    const animationFrameId = window.requestAnimationFrame(
      resetDocumentScrollPosition,
    );

    return () => window.cancelAnimationFrame(animationFrameId);
  }, [pathname]);

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
