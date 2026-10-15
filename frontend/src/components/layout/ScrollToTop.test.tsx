import { act, render } from '@testing-library/react';
import { MemoryRouter, useNavigate, type NavigateFunction } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ScrollToTop } from './ScrollToTop';

let navigate: NavigateFunction;
function Navigator() {
  navigate = useNavigate();
  return null;
}

describe('ScrollToTop', () => {
  beforeEach(() => {
    window.scrollTo = vi.fn() as unknown as typeof window.scrollTo;
  });

  const resetCount = () =>
    vi.mocked(window.scrollTo).mock.calls.filter(([options]) => (options as ScrollToOptions)?.behavior === 'auto').length;

  it('resets on master dashboard tab changes but not on in-page tabs elsewhere', () => {
    render(
      <MemoryRouter initialEntries={['/master/dashboard']}>
        <ScrollToTop />
        <Navigator />
      </MemoryRouter>,
    );
    const afterMount = resetCount();

    act(() => navigate('/master/dashboard?tab=analytics'));
    expect(resetCount()).toBeGreaterThan(afterMount);

    act(() => navigate('/sites/S1/projects/P1'));
    const afterRoute = resetCount();
    act(() => navigate('/sites/S1/projects/P1?tab=equipment'));
    expect(resetCount()).toBe(afterRoute);
  });
});
