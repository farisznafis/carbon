import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FormProvider, useForm, type UseFormReturn } from 'react-hook-form';
import { describe, expect, it } from 'vitest';
import { DynamicFormField } from './DynamicFormField';
import { joinPersonName, splitPersonName } from './personName';

describe('splitPersonName / joinPersonName', () => {
  it('reads 姓 and 名 from either space', () => {
    expect(splitPersonName('山田 太郎')).toEqual({ lastName: '山田', firstName: '太郎' });
    expect(splitPersonName('山田　太郎')).toEqual({ lastName: '山田', firstName: '太郎' });
  });

  it('keeps a name without a separator whole, as the 姓', () => {
    expect(splitPersonName('山田太郎')).toEqual({ lastName: '山田太郎', firstName: '' });
    expect(splitPersonName(undefined)).toEqual({ lastName: '', firstName: '' });
  });

  it('joins with the half-width space the server uses', () => {
    expect(joinPersonName({ lastName: '山田', firstName: '太郎' })).toBe('山田 太郎');
    expect(joinPersonName({ lastName: '山田', firstName: '' })).toBe('山田');
  });
});

type Values = { picName: string };

function NameField({ onForm }: { onForm: (form: UseFormReturn<Values>) => void }) {
  const form = useForm<Values>({ defaultValues: { picName: '' } });
  onForm(form);
  return (
    <FormProvider {...form}>
      <DynamicFormField<Values>
        field={{ name: 'picName', label: '申請責任者の氏名', type: 'personName' }}
      />
    </FormProvider>
  );
}

describe('personName field', () => {
  it('stores 姓 and 名 as one 氏名', async () => {
    const user = userEvent.setup();
    let form!: UseFormReturn<Values>;
    render(<NameField onForm={(f) => (form = f)} />);

    await user.type(screen.getByLabelText('姓'), '佐藤');
    await user.type(screen.getByLabelText('名'), '花子');

    expect(form.getValues('picName')).toBe('佐藤 花子');
  });

  it('does not move a 名 typed first into 姓', async () => {
    const user = userEvent.setup();
    render(<NameField onForm={() => undefined} />);

    await user.type(screen.getByLabelText('名'), '花子');

    expect(screen.getByLabelText('姓')).toHaveValue('');
    expect(screen.getByLabelText('名')).toHaveValue('花子');
  });

  it('splits a value loaded from the server', () => {
    let form!: UseFormReturn<Values>;
    render(<NameField onForm={(f) => (form = f)} />);

    act(() => form.setValue('picName', '鳥居 沙奈'));

    expect(screen.getByLabelText('姓')).toHaveValue('鳥居');
    expect(screen.getByLabelText('名')).toHaveValue('沙奈');
  });
});
