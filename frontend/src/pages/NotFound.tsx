import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { NotFoundPage } from '@/components/feedback/NotFoundPage';
import { logger } from '@/lib/logger';

/** Logs an unknown route and renders the shared Figma-aligned 404 page. */
const NotFound = () => {
  const location = useLocation();

  useEffect(() => {
    logger.error('404:', location.pathname);
  }, [location.pathname]);

  return <NotFoundPage />;
};

export default NotFound;
