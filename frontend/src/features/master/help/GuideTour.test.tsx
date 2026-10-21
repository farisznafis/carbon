import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { GuideTour } from './GuideTour';

/** jsdom lays nothing out, so give every element a visible box. */
function mockLayout() {
  return vi
    .spyOn(Element.prototype, 'getBoundingClientRect')
    .mockReturnValue({
      top: 10, left: 10, bottom: 50, right: 110, width: 100, height: 40, x: 10, y: 10,
      toJSON: () => ({}),
    } as DOMRect);
}

/** The master guide's targets; account-menu and help-menu are left out on purpose. */
function renderPage(onClose = vi.fn()) {
  render(
    <>
      <nav>
        <span data-tour="nav-home">ホーム</span>
        <span data-tour="nav-analytics">アナリティクス</span>
        <span data-tour="nav-membership">入会届</span>
        <span data-tour="nav-sites">拠点</span>
        <span data-tour="nav-users">社員</span>
      </nav>
      <GuideTour context="master-dashboard" onClose={onClose} />
    </>,
  );
  return onClose;
}

describe('GuideTour', () => {
  beforeEach(() => {
    mockLayout();
    Element.prototype.scrollIntoView = vi.fn();
  });

  afterEach(() => vi.restoreAllMocks());

  it('walks only the steps whose targets are on the page and closes on 完了', async () => {
    const user = userEvent.setup();
    const onClose = renderPage();

    const dialog = await screen.findByRole('dialog', { name: 'ホーム' });
    expect(dialog).toHaveTextContent('1 / 5');
    await waitFor(() => expect(screen.getByRole('button', { name: '次へ' })).toHaveFocus());
    expect(screen.queryByRole('button', { name: '戻る' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '次へ' }));
    expect(await screen.findByRole('dialog', { name: 'アナリティクス' })).toHaveTextContent('2 / 5');

    await user.click(screen.getByRole('button', { name: '戻る' }));
    expect(await screen.findByRole('dialog', { name: 'ホーム' })).toBeInTheDocument();

    for (let step = 0; step < 4; step += 1) {
      await user.click(screen.getByRole('button', { name: '次へ' }));
    }
    expect(await screen.findByRole('dialog', { name: '社員' })).toHaveTextContent('5 / 5');

    await user.click(screen.getByRole('button', { name: '完了' }));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('ends on スキップ and on Escape', async () => {
    const user = userEvent.setup();
    const onClose = renderPage();

    await user.click(await screen.findByRole('button', { name: 'スキップ' }));
    expect(onClose).toHaveBeenCalledTimes(1);

    await user.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalledTimes(2);
  });

  it('keeps the explanation inside a section taller than the screen', async () => {
    vi.mocked(Element.prototype.getBoundingClientRect).mockReturnValue({
      top: 0, left: 10, bottom: 3000, right: 1010, width: 1000, height: 3000, x: 10, y: 0,
      toJSON: () => ({}),
    } as DOMRect);
    renderPage();

    const dialog = await screen.findByRole('dialog', { name: 'ホーム' });
    // GUIDE_CONTENT gives these steps side 'right'; a pinned explanation sits above the bottom edge.
    expect(dialog).toHaveAttribute('data-side', 'top');
    expect(screen.getByRole('button', { name: '次へ' })).toBeInTheDocument();
  });

  it('closes itself when none of its targets exist', async () => {
    const onClose = vi.fn();
    vi.useFakeTimers({ shouldAdvanceTime: true });
    render(<GuideTour context="project-detail" onClose={onClose} />);

    await vi.advanceTimersByTimeAsync(3500);
    expect(onClose).toHaveBeenCalled();
    vi.useRealTimers();
  });
});
