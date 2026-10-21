import { StrictMode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { GuideTourProvider, useGuideTour } from './GuideTourProvider';

const { getSeenGuideTours, markGuideTourSeen } = vi.hoisted(() => ({
  getSeenGuideTours: vi.fn(),
  markGuideTourSeen: vi.fn(),
}));

vi.mock('@/api/guideTours', () => ({ getSeenGuideTours, markGuideTourSeen }));
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: { id: 'USER-1' }, isAuthenticated: true }),
}));
// The tour itself is covered in GuideTour.test.tsx; here only whether it opens matters.
vi.mock('./GuideTour', () => ({
  GuideTour: ({ context }: { context: string }) => <div role="dialog" aria-label={context} />,
}));

function HelpMenu() {
  const { currentContext, startTour } = useGuideTour();
  return (
    <button type="button" onClick={() => currentContext && startTour(currentContext)}>
      使い方ガイド
    </button>
  );
}

function renderProvider() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/dashboard']}>
          <GuideTourProvider>
            <HelpMenu />
          </GuideTourProvider>
        </MemoryRouter>
      </QueryClientProvider>
    </StrictMode>,
  );
  return queryClient;
}

/** Longer than the provider's start delay. */
const afterStartDelay = () => new Promise((resolve) => setTimeout(resolve, 400));

describe('GuideTourProvider', () => {
  beforeEach(() => {
    getSeenGuideTours.mockReset();
    markGuideTourSeen.mockReset();
  });

  it('does not show a tour the account has already seen', async () => {
    getSeenGuideTours.mockResolvedValue(['site-dashboard']);
    renderProvider();

    await waitFor(() => expect(getSeenGuideTours).toHaveBeenCalled());
    await afterStartDelay();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(markGuideTourSeen).not.toHaveBeenCalled();
  });

  it('shows an unseen tour once and records it once', async () => {
    getSeenGuideTours.mockResolvedValue(['master-dashboard']);
    markGuideTourSeen.mockResolvedValue(['master-dashboard', 'site-dashboard']);
    const queryClient = renderProvider();

    expect(await screen.findByRole('dialog', { name: 'site-dashboard' })).toBeInTheDocument();
    expect(markGuideTourSeen).toHaveBeenCalledTimes(1);
    expect(markGuideTourSeen.mock.calls[0][0]).toBe('site-dashboard');
    await waitFor(() =>
      expect(queryClient.getQueryData(['guideTours', 'USER-1'])).toEqual([
        'master-dashboard',
        'site-dashboard',
      ]),
    );
  });

  it.each([
    ['pending', () => new Promise<string[]>(() => undefined)],
    ['failed', () => Promise.reject(new Error('down'))],
  ])('shows no tour while the list is %s', async (_state, response) => {
    getSeenGuideTours.mockImplementation(response);
    renderProvider();

    await waitFor(() => expect(getSeenGuideTours).toHaveBeenCalled());
    await afterStartDelay();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(markGuideTourSeen).not.toHaveBeenCalled();
  });

  it('starts from the help menu even when the tour was seen', async () => {
    getSeenGuideTours.mockResolvedValue(['site-dashboard']);
    renderProvider();

    await userEvent.click(screen.getByRole('button', { name: '使い方ガイド' }));
    expect(await screen.findByRole('dialog', { name: 'site-dashboard' })).toBeInTheDocument();
    expect(markGuideTourSeen).not.toHaveBeenCalled();
  });
});
