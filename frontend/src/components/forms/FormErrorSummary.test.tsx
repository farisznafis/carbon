import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useForm, type UseFormReturn } from 'react-hook-form';
import { beforeAll, describe, expect, it, vi } from 'vitest';
import { FormSection } from '@/components/forms/form-layout';
import { RequiredLabel } from '@/components/forms/shared';
import { Form, FormControl, FormField, FormItem, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { FormErrorSummary } from './FormErrorSummary';

type Values = { name: string; email: string };

let form: UseFormReturn<Values>;

function TestForm() {
  form = useForm<Values>({ defaultValues: { name: '', email: '' } });
  return (
    <Form {...form}>
      <div>
        <FormSection title="基本情報">
          {(['name', 'email'] as const).map((name) => (
            <FormField
              key={name}
              control={form.control}
              name={name}
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>{name === 'name' ? '氏名' : 'メールアドレス'}</RequiredLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          ))}
        </FormSection>
        <FormErrorSummary />
        <button type="button">保存</button>
      </div>
    </Form>
  );
}

beforeAll(() => {
  Element.prototype.scrollIntoView = vi.fn();
});

describe('FormErrorSummary', () => {
  it('stays hidden until the form has errors', () => {
    render(<TestForm />);
    expect(screen.queryByText(/入力内容を確認してください/)).not.toBeInTheDocument();
  });

  it('goes straight to the field when there is only one error', async () => {
    render(<TestForm />);

    act(() => form.setError('email', { type: 'server', message: '形式が正しくありません' }));

    expect(await screen.findByText('入力内容を確認してください（1件）')).toBeInTheDocument();
    expect(screen.getByLabelText(/メールアドレス/)).toHaveFocus();
  });

  it('lists field errors under their section and jumps to the field', async () => {
    const user = userEvent.setup();
    render(<TestForm />);

    act(() => {
      form.setError('name', { type: 'server', message: '入力してください' });
      form.setError('email', { type: 'server', message: '形式が正しくありません' });
    });

    expect(await screen.findByText('入力内容を確認してください（2件）')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveFocus();

    await user.click(screen.getByRole('button', { name: 'メールアドレス：形式が正しくありません' }));
    expect(screen.getByLabelText(/メールアドレス/)).toHaveFocus();

    act(() => form.clearErrors());
    await vi.waitFor(() =>
      expect(screen.queryByText(/入力内容を確認してください/)).not.toBeInTheDocument(),
    );
  });
});
