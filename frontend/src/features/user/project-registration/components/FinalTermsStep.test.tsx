import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { FinalTermsStep } from './FinalTermsStep';

describe('FinalTermsStep', () => {
  it('shows the backend validation message on the agreement checkbox', async () => {
    render(
      <FinalTermsStep
        formValues={{ finalTermsAgreed: false }}
        onValueChange={vi.fn()}
        errors={{ finalTermsAgreed: '入会規約への同意が必要です。' }}
      />,
    );

    expect(
      await screen.findByText('入会規約への同意が必要です。'),
    ).toBeInTheDocument();
  });
});
