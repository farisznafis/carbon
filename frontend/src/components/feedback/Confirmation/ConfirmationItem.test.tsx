import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ConfirmationItem } from './ConfirmationItem';

describe('ConfirmationItem', () => {
  it('uses the shared primary highlight for a changed value', () => {
    render(
      <ConfirmationItem
        label='電話番号'
        value='0354321235'
        originalValue='0312345678'
      />,
    );

    const changedValue = screen.getByText('0354321235');
    expect(changedValue).toHaveClass(
      'text-primary-strong',
      'bg-secondary/60',
      'rounded',
    );
    expect(changedValue).not.toHaveClass('underline');
    expect(screen.getByText('変更あり')).toBeInTheDocument();
  });

  it('keeps an unchanged value in the default foreground color', () => {
    render(
      <ConfirmationItem
        label='電話番号'
        value='0354321235'
        originalValue='0354321235'
      />,
    );

    expect(screen.getByText('0354321235')).toHaveClass('text-foreground');
    expect(screen.getByText('0354321235')).not.toHaveClass('text-primary-strong');
    expect(screen.queryByText('変更あり')).not.toBeInTheDocument();
  });
});
