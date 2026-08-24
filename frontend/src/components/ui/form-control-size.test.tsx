import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { PasswordInput } from '@/components/inputs';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

describe('shared form control sizes', () => {
  it('uses the md 48px size for Input by default', () => {
    render(<Input aria-label="会社名" />);

    expect(screen.getByRole('textbox', { name: '会社名' })).toHaveClass(
      'h-12',
      'px-4',
    );
  });

  it('uses the md 48px size for SelectTrigger by default', () => {
    render(
      <Select>
        <SelectTrigger aria-label="都道府県">
          <SelectValue placeholder="選択してください" />
        </SelectTrigger>
      </Select>,
    );

    expect(screen.getByRole('combobox', { name: '都道府県' })).toHaveClass(
      'h-12',
      'px-4',
    );
  });

  it('keeps wrapper controls such as PasswordInput on the md default', () => {
    render(<PasswordInput aria-label="パスワード" />);

    expect(screen.getByLabelText('パスワード')).toHaveClass('h-12', 'px-4');
  });

  it('still supports an intentional compact size for non-form utilities', () => {
    render(<Input aria-label="コンパクト" inputSize="sm" />);

    expect(screen.getByRole('textbox', { name: 'コンパクト' })).toHaveClass(
      'h-10',
      'px-3',
    );
  });
});
