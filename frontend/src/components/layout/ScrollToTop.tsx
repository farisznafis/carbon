import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { ScrollToTopButton } from '@/components/navigation/ScrollToTopButton';

const SCROLL_VISIBLE_THRESHOLD = 8;

export const ScrollToTop = () => {
  const { pathname } = useLocation();
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    window.scrollTo(0, 0);
    setIsVisible(false);
  }, [pathname]);

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
