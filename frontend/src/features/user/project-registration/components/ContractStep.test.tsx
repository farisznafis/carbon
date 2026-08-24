import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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

  it('reports user edits separately from prefill resets', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <ContractStep
        formValues={{}}
        onValueChange={onValueChange}
      />,
    );

    await user.type(screen.getByLabelText(/事業者名\s*\*/), 'ABC株式会社');

    await waitFor(() => {
      expect(onValueChange).toHaveBeenCalledWith(
        expect.objectContaining({ businessName: 'ABC株式会社' }),
        { isUserChange: true },
      );
    });
  });

  it('shows non-blocking default loading and retry states', async () => {
    const user = userEvent.setup();
    const onRetryDefaults = vi.fn();
    const { rerender } = render(
      <ContractStep
        formValues={{}}
        onValueChange={vi.fn()}
        isDefaultLoading
      />,
    );

    expect(screen.getByRole('status')).toHaveTextContent(
      '登録済み情報を読み込んでいます...',
    );

    rerender(
      <ContractStep
        formValues={{}}
        onValueChange={vi.fn()}
        defaultLoadError="登録済み情報を取得できませんでした。"
        onRetryDefaults={onRetryDefaults}
      />,
    );
    await user.click(screen.getByRole('button', { name: '再試行' }));

    expect(onRetryDefaults).toHaveBeenCalledTimes(1);
    expect(screen.getByLabelText(/事業者名\s*\*/)).toBeEnabled();
  });
});
