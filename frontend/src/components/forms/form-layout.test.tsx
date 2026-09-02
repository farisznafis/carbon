import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { FormActions, FormSection } from './form-layout';

describe('FormSection', () => {
  it('renders an icon marker without changing numbered section support', () => {
    const { rerender } = render(
      <FormSection icon={<span aria-hidden="true">U</span>} title="個人情報">
        <p>内容</p>
      </FormSection>,
    );

    expect(screen.getByText('U')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '個人情報' })).toBeInTheDocument();

    rerender(
      <FormSection number={1} title="基本情報">
        <p>内容</p>
      </FormSection>,
    );

    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '基本情報' })).toBeInTheDocument();
  });
});

describe('FormActions', () => {
  it('renders one standard action at full width with the md size', () => {
    render(
      <FormActions
        actions={[{ id: 'submit', label: '登録', type: 'submit' }]}
      />,
    );

    const submitButton = screen.getByRole('button', { name: '登録' });
    expect(submitButton).toHaveAttribute('type', 'submit');
    expect(submitButton).toHaveClass('h-10', 'w-full');
  });

  it('renders two equal responsive standard actions and forwards handlers', () => {
    const onBack = vi.fn();
    render(
      <FormActions
        actions={[
          { id: 'back', label: '戻る', variant: 'outline', onClick: onBack },
          { id: 'next', label: '次へ' },
        ]}
      />,
    );

    const actionGroup = screen.getByRole('button', { name: '戻る' }).parentElement;
    expect(actionGroup).toHaveClass('sm:grid-cols-2');
    fireEvent.click(screen.getByRole('button', { name: '戻る' }));
    expect(onBack).toHaveBeenCalledOnce();
  });

  it('renders three equal actions and forwards semantic variants', () => {
    render(
      <FormActions
        actions={[
          { id: 'reject', label: '却下', variant: 'destructive' },
          { id: 'back', label: '戻る', variant: 'outline' },
          { id: 'approve', label: '承認', variant: 'success' },
        ]}
      />,
    );

    const actionGroup = screen.getByRole('button', { name: '承認' }).parentElement;
    expect(actionGroup).toHaveClass('sm:grid-cols-3');
    expect(screen.getByRole('button', { name: '承認' })).toHaveClass('bg-success');
  });

  it('supports an independent leading action slot', () => {
    render(
      <FormActions
        disabled
        leadingAction={<button type="button">削除</button>}
        actions={[
          { id: 'back', label: '戻る' },
          { id: 'save', label: '更新' },
        ]}
      />,
    );

    const actionGroup = screen.getByRole('button', { name: '削除' }).closest('div');
    expect(actionGroup).toHaveClass(
      'grid-cols-[2.5rem_minmax(0,1fr)_minmax(0,1fr)]',
    );
    expect(screen.getByRole('button', { name: '削除' })).toBeDisabled();
  });

  it('uses auth and compact context sizes', () => {
    const { rerender } = render(
      <FormActions
        context="auth"
        actions={[{ id: 'login', label: 'ログイン', type: 'submit' }]}
      />,
    );
    expect(screen.getByRole('button', { name: 'ログイン' })).toHaveClass('h-12');

    rerender(
      <FormActions
        context="compact"
        actions={[{ id: 'confirm', label: '確定' }]}
      />,
    );
    expect(screen.getByRole('button', { name: '確定' })).toHaveClass(
      'h-8',
      'w-full',
    );
  });

  it('shows the pending label and disables every action while pending', () => {
    render(
      <FormActions
        pendingActionId="submit"
        actions={[
          { id: 'back', label: '戻る', variant: 'outline' },
          { id: 'submit', label: '登録', pendingLabel: '登録中...' },
        ]}
      />,
    );

    expect(screen.getByRole('button', { name: '戻る' })).toBeDisabled();
    const pendingButton = screen.getByRole('button', { name: '登録中...' });
    expect(pendingButton).toBeDisabled();
    expect(pendingButton).toHaveAttribute('aria-busy', 'true');
    expect(pendingButton.querySelector('.animate-spin')).toBeInTheDocument();
  });
});
