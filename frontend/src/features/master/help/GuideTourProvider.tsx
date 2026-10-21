import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useLocation } from 'react-router-dom';
import { getSeenGuideTours, markGuideTourSeen } from '@/api/guideTours';
import { useAuth } from '@/contexts/AuthContext';
import { GuideTour } from './GuideTour';
import { getGuideContextForPath, type GuideContext } from './guideSteps';

/** Lets the page settle (and the help popover or mobile drawer close) before dimming it. */
const START_DELAY_MS = 300;

type GuideTourContextValue = {
  /** The guide for the current page, if it has one. */
  currentContext: GuideContext | null;
  startTour: (context: GuideContext) => void;
};

const GuideTourContext = createContext<GuideTourContextValue>({
  currentContext: null,
  startTour: () => undefined,
});

export function useGuideTour() {
  return useContext(GuideTourContext);
}

/**
 * Shows a page's tour the first time each account opens it, and again
 * whenever 使い方ガイド is chosen from the sidebar help menu.
 */
export function GuideTourProvider({ children }: Readonly<{ children: ReactNode }>) {
  const { user, isAuthenticated } = useAuth();
  const { pathname } = useLocation();
  const currentContext = isAuthenticated ? getGuideContextForPath(pathname) : null;
  const [activeContext, setActiveContext] = useState<GuideContext | null>(null);
  const startTimer = useRef<number>();
  // Tours auto-started in this session, so a re-run effect (StrictMode) cannot start one twice.
  const autoStarted = useRef(new Set<string>());
  const queryClient = useQueryClient();
  const guideToursKey = ['guideTours', user?.id] as const;

  const seenGuideTours = useQuery({
    queryKey: guideToursKey,
    queryFn: getSeenGuideTours,
    enabled: isAuthenticated && currentContext !== null,
  });

  const { mutate: recordSeen } = useMutation({
    mutationFn: markGuideTourSeen,
    onSuccess: (seen) => queryClient.setQueryData(guideToursKey, seen),
  });

  const startTour = useCallback((context: GuideContext) => {
    window.clearTimeout(startTimer.current);
    startTimer.current = window.setTimeout(() => setActiveContext(context), START_DELAY_MS);
  }, []);

  const closeTour = useCallback(() => setActiveContext(null), []);

  // A tour belongs to one page; leaving the page ends it.
  useEffect(() => {
    window.clearTimeout(startTimer.current);
    setActiveContext(null);
  }, [pathname]);

  // First visit: recorded as soon as it starts, so a reload mid-tour does not replay it.
  // While the list is loading or failed, no tour: better missing once than shown every time.
  const seen = seenGuideTours.isSuccess ? seenGuideTours.data : null;
  useEffect(() => {
    if (!currentContext || !seen || seen.includes(currentContext)) return;
    const startKey = `${user?.id}:${currentContext}`;
    if (autoStarted.current.has(startKey)) return;

    autoStarted.current.add(startKey);
    recordSeen(currentContext);
    startTour(currentContext);
  }, [currentContext, recordSeen, seen, startTour, user?.id]);

  useEffect(() => () => window.clearTimeout(startTimer.current), []);

  const value = useMemo(
    () => ({ currentContext, startTour }),
    [currentContext, startTour],
  );

  return (
    <GuideTourContext.Provider value={value}>
      {children}
      {activeContext && (
        <GuideTour key={activeContext} context={activeContext} onClose={closeTour} />
      )}
    </GuideTourContext.Provider>
  );
}
