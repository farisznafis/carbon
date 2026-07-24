import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ContractStep } from './ContractStep';

describe('ContractStep', () => {
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
