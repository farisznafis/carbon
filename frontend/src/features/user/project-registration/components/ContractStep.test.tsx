import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ContractStep } from './ContractStep';

describe('ContractStep', () => {
  it('marks the title and every required control with the shared required marker', () => {
    render(
      <ContractStep
        formValues={{
          businessName: '',
          representativeName: '',
          representativePosition: '',
          contractAgreed: false,
        }}
        onValueChange={vi.fn()}
      />,
    );

    expect(screen.getByLabelText(/事業者名\s*\*/)).toBeRequired();
    expect(screen.getByLabelText(/代表者名\s*\*/)).toBeRequired();
    expect(screen.getByLabelText(/代表者役職\s*\*/)).toBeRequired();
    expect(
      screen.getByRole('checkbox', { name: /同意する\s*\*/ }),
    ).toHaveAttribute('aria-required', 'true');
    expect(screen.getAllByText('*')).toHaveLength(5);
  });

  it('shows backend validation messages on the matching controls', async () => {
    render(
      <ContractStep
        formValues={{
          businessName: '',
          representativeName: '',
          representativePosition: '',
          contractAgreed: false,
        }}
        onValueChange={vi.fn()}
        errors={{
          businessName: '事業者名を入力してください。',
          contractAgreed: '同意が必要です。',
        }}
      />,
    );

    expect(
      await screen.findByText('事業者名を入力してください。'),
    ).toBeInTheDocument();
    expect(await screen.findByText('同意が必要です。')).toBeInTheDocument();
  });
});
