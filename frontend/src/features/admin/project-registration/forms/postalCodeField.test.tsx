import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FormProvider, useForm, type UseFormReturn } from 'react-hook-form';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { normalizePostalCode } from '@/lib/postalCode';
import { DynamicFormField } from './DynamicFormField';

type Values = {
  op: { postalCode: string; prefecture: string; city: string; address: string };
};

function AddressFields({ onForm, address = '' }: { onForm: (f: UseFormReturn<Values>) => void; address?: string }) {
  const form = useForm<Values>({
    defaultValues: { op: { postalCode: '', prefecture: '', city: '', address } },
  });
  onForm(form);
  return (
    <FormProvider {...form}>
      <DynamicFormField<Values>
        field={{
          name: 'op.postalCode',
          label: '郵便番号',
          type: 'postalCode',
          required: false,
          addressFields: { prefecture: 'prefecture', city: 'city', address: 'address' },
        }}
      />
    </FormProvider>
  );
}

const zipcloud = (results: unknown) =>
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    json: async () => ({ status: 200, results }),
  } as Response);

afterEach(() => vi.restoreAllMocks());

describe('normalizePostalCode', () => {
  it('accepts hyphens and full-width digits', () => {
    expect(normalizePostalCode('100-0005')).toBe('1000005');
    expect(normalizePostalCode('１００ー０００５')).toBe('1000005');
    expect(normalizePostalCode('10000')).toBeNull();
  });
});

describe('postalCode field', () => {
  it('fills 都道府県, 市区町村 and the start of 以降 from seven digits', async () => {
    zipcloud([{ address1: '東京都', address2: '千代田区', address3: '丸の内' }]);
    const user = userEvent.setup();
    let form!: UseFormReturn<Values>;
    render(<AddressFields onForm={(f) => (form = f)} />);

    await user.type(screen.getByRole('textbox'), '100-0005');

    await waitFor(() => expect(form.getValues('op.prefecture')).toBe('東京都'));
    expect(form.getValues('op')).toEqual({
      postalCode: '1000005',
      prefecture: '東京都',
      city: '千代田区',
      address: '丸の内',
    });
    expect(screen.getByText(/番地・建物名を追記/)).toBeInTheDocument();
  });

  it('leaves a 住所 the operator already typed', async () => {
    zipcloud([{ address1: '東京都', address2: '千代田区', address3: '丸の内' }]);
    const user = userEvent.setup();
    let form!: UseFormReturn<Values>;
    render(<AddressFields onForm={(f) => (form = f)} address="丸の内1-1-1 ビル5F" />);

    await user.type(screen.getByRole('textbox'), '1000005');

    await waitFor(() => expect(form.getValues('op.city')).toBe('千代田区'));
    expect(form.getValues('op.address')).toBe('丸の内1-1-1 ビル5F');
  });

  it('says so when the code matches nothing', async () => {
    zipcloud(null);
    const user = userEvent.setup();
    render(<AddressFields onForm={() => undefined} />);

    await user.type(screen.getByRole('textbox'), '0000000');

    expect(await screen.findByText(/該当する住所が見つかりません/)).toBeInTheDocument();
  });
});
