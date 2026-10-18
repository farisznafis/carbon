import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { TooltipProvider } from '@/components/ui/tooltip';
import { EmissionReductionProgress } from './EmissionReductionProgress';

describe('EmissionReductionProgress', () => {
  it('renders progress content without throwing a runtime error', () => {
    render(
      <TooltipProvider>
        <EmissionReductionProgress
          baselineEmission={100}
          projectEmission={31.6}
          targetReduction={100}
        />
      </TooltipProvider>,
    );

    expect(screen.getByText('排出削減量')).toBeInTheDocument();
    expect(screen.getByText('68.4%')).toBeInTheDocument();
    expect(screen.getByLabelText(/達成率 68.4%/)).toBeInTheDocument();
  });
});
