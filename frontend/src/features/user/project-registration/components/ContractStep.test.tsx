import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ContractStep } from './ContractStep';

const methodologyProps = {
  methodologyNumber: '003',
  methodologyName: '自家消費型太陽光発電設備の導入',
  methodologyVersion: 'Ver.2.0',
};

describe('ContractStep', () => {
  it('marks the title and every required control with the shared required marker', () => {
    render(
      <ContractStep
        {...methodologyProps}
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
    expect(
      screen.getByText(/方法論003 Ver\.2\.0（自家消費型太陽光発電設備の導入）/),
    ).toBeVisible();
  });

  it('shows backend validation messages on the matching controls', async () => {
    render(
      <ContractStep
        {...methodologyProps}
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

  it('omits the methodology version when the project does not provide one', () => {
    render(
      <ContractStep
        methodologyNumber="003"
        methodologyName="自家消費型太陽光発電設備の導入"
        formValues={{}}
        onValueChange={vi.fn()}
      />,
    );

    expect(
      screen.getByText(/方法論003（自家消費型太陽光発電設備の導入）/),
    ).toBeVisible();
    expect(screen.queryByText(/Ver\./)).not.toBeInTheDocument();
  });

  it('reports manual user edits to the parent flow', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <ContractStep
        {...methodologyProps}
        formValues={{}}
        onValueChange={onValueChange}
      />,
    );

    await user.type(screen.getByLabelText(/事業者名\s*\*/), 'ABC株式会社');

    await waitFor(() => {
      expect(onValueChange).toHaveBeenCalledWith(
        expect.objectContaining({ businessName: 'ABC株式会社' }),
      );
    });
  });
});
