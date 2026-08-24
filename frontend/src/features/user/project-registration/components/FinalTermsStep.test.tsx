import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { FinalTermsStep } from './FinalTermsStep';

describe('FinalTermsStep', () => {
  it('uses the shared required consent control', () => {
    render(
      <FinalTermsStep
        projectName="関東太陽光発電プロジェクト"
        formValues={{ finalTermsAgreed: false }}
        onValueChange={vi.fn()}
      />,
    );

    expect(
      screen.getByRole('checkbox', { name: /同意する\s*\*/ }),
    ).toHaveAttribute('aria-required', 'true');
    expect(
      screen.getByRole('heading', {
        name: /「関東太陽光発電プロジェクト」入会規約/,
      }),
    ).toBeVisible();
    expect(
      screen.getByText(
        '本会の名称は、関東太陽光発電プロジェクト (以下「本会」という) とする。',
      ),
    ).toBeVisible();
  });

  it('shows the backend validation message on the agreement checkbox', async () => {
    render(
      <FinalTermsStep
        projectName="関東太陽光発電プロジェクト"
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
