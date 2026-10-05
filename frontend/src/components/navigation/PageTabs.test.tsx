import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Tabs } from '@/components/ui/tabs';
import { SegmentTabsList, SegmentTabsTrigger, TabCount } from './PageTabs';

describe('SegmentTabs', () => {
  it('marks only the selected segment as active and moves it on click', () => {
    render(
      <Tabs defaultValue="open">
        <SegmentTabsList aria-label="status">
          <SegmentTabsTrigger value="open">
            承認待ち
            <TabCount count={2} tone="warning" />
          </SegmentTabsTrigger>
          <SegmentTabsTrigger value="approved">
            承認済み
            <TabCount count={3} />
          </SegmentTabsTrigger>
        </SegmentTabsList>
      </Tabs>,
    );

    const open = screen.getByRole('tab', { name: '承認待ち2' });
    const approved = screen.getByRole('tab', { name: '承認済み3' });
    expect(open).toHaveAttribute('data-state', 'active');
    expect(approved).toHaveAttribute('data-state', 'inactive');
    // The active look is driven by data-state, so it follows the selection.
    expect(open.className).toContain('data-[state=active]:bg-card');

    fireEvent.mouseDown(approved);
    expect(approved).toHaveAttribute('data-state', 'active');
    expect(open).toHaveAttribute('data-state', 'inactive');
  });

  it('colours a count as waiting work only when asked to', () => {
    render(
      <>
        <TabCount count={2} tone="warning" />
        <TabCount count={3} />
      </>,
    );

    expect(screen.getByText('2')).toHaveClass('bg-warning-subtle');
    expect(screen.getByText('3')).toHaveClass('bg-secondary');
  });
});
