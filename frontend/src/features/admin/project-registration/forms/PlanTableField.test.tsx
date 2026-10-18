import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useForm, FormProvider, type UseFormReturn } from 'react-hook-form';
import { describe, expect, it } from 'vitest';
import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { DynamicFormField } from './DynamicFormField';

type Row = {
  symbol: string;
  definition: string;
  hasDetail?: boolean;
  detail?: string;
};

type TableFormValues = {
  rows: Row[];
};

const COLUMNS: FieldConfig['columns'] = [
  { name: 'symbol', label: '記号', type: 'text' },
  { name: 'definition', label: '定義', type: 'text' },
];

function TableForm({
  defaultRows = [],
  overrides,
  onForm,
}: {
  defaultRows?: Row[];
  overrides?: Partial<FieldConfig<TableFormValues>>;
  onForm?: (form: UseFormReturn<TableFormValues>) => void;
}) {
  const form = useForm<TableFormValues>({ defaultValues: { rows: defaultRows } });
  onForm?.(form);

  return (
    <FormProvider {...form}>
      <DynamicFormField<TableFormValues>
        field={{
          name: 'rows',
          label: 'モニタリング項目',
          type: 'table',
          columns: COLUMNS,
          ...overrides,
        }}
      />
    </FormProvider>
  );
}

/** Data rows only — the header row is a `tr` too. */
function dataRows() {
  const [body] = screen.getAllByRole('rowgroup').slice(1);
  return within(body).queryAllByRole('row');
}

describe('PlanTableField', () => {
  it('draws one column per config and one row per value', () => {
    render(
      <TableForm
        defaultRows={[
          { symbol: 'EL', definition: '電力量' },
          { symbol: 'FU', definition: '燃料使用量' },
        ]}
      />,
    );

    expect(screen.getByRole('columnheader', { name: /記号/ })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /定義/ })).toBeInTheDocument();
    expect(dataRows()).toHaveLength(2);
    expect(screen.getByDisplayValue('燃料使用量')).toBeInTheDocument();
  });

  it('binds each cell to its own row, so typing does not spread down the column', async () => {
    const user = userEvent.setup();
    let form: UseFormReturn<TableFormValues> | undefined;
    render(
      <TableForm
        defaultRows={[
          { symbol: 'EL', definition: '' },
          { symbol: 'FU', definition: '' },
        ]}
        onForm={(f) => {
          form = f;
        }}
      />,
    );

    const [, secondRow] = dataRows();
    await user.type(within(secondRow).getByLabelText('定義'), '燃料');

    expect(form?.getValues('rows.1.definition')).toBe('燃料');
    expect(form?.getValues('rows.0.definition')).toBe('');
  });

  it('adds and removes rows', async () => {
    const user = userEvent.setup();
    render(<TableForm defaultRows={[{ symbol: 'EL', definition: '電力量' }]} />);

    await user.click(screen.getByRole('button', { name: '行を追加' }));
    expect(dataRows()).toHaveLength(2);

    await user.click(within(dataRows()[1]).getByRole('button', { name: '削除' }));
    expect(dataRows()).toHaveLength(1);
  });

  it('will not go below the minimum, and seeds up to it', () => {
    render(<TableForm overrides={{ minRows: 2 }} />);

    expect(dataRows()).toHaveLength(2);
    dataRows().forEach((row) => {
      expect(within(row).getByRole('button', { name: '削除' })).toBeDisabled();
    });
  });

  it('offers no add or remove for a table the server derives', () => {
    // The 年度 skeleton is built from 認証対象期間; a row added here would be
    // discarded on the next read, so the buttons must not be there at all.
    render(
      <TableForm
        defaultRows={[{ symbol: 'EL', definition: '電力量' }]}
        overrides={{ fixedRows: true }}
      />,
    );

    expect(screen.queryByRole('button', { name: '行を追加' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '削除' })).not.toBeInTheDocument();
  });

  it('shows a rejected cell against that cell', async () => {
    let form: UseFormReturn<TableFormValues> | undefined;
    render(
      <TableForm
        defaultRows={[
          { symbol: 'EL', definition: '電力量' },
          { symbol: '', definition: '燃料使用量' },
        ]}
        onForm={(f) => {
          form = f;
        }}
      />,
    );

    // The shape applyBackendValidationErrors produces from `rows[1].symbol`.
    act(() => {
      form?.setError('rows.1.symbol', { type: 'server', message: '記号は必須です。' });
    });

    const message = await screen.findByText('記号は必須です。');
    expect(within(dataRows()[1]).getByText('記号は必須です。')).toBe(message);
    expect(within(dataRows()[0]).queryByText('記号は必須です。')).toBeNull();
  });

  it('picks up rows that arrive from the server after the page is open', async () => {
    // The wizard loads a page by setting the whole page object at once. The
    // field array does not see that write, so without reconciliation the 38
    // 年度 the server just built would sit in the form values behind an empty
    // table.
    let form: UseFormReturn<TableFormValues> | undefined;
    render(
      <TableForm
        onForm={(f) => {
          form = f;
        }}
      />,
    );

    expect(screen.getByText('行がありません')).toBeInTheDocument();

    act(() => {
      form?.setValue('rows', [
        { symbol: 'EL', definition: '電力量' },
        { symbol: 'FU', definition: '燃料使用量' },
      ]);
    });

    await waitFor(() => expect(dataRows()).toHaveLength(2));
    expect(screen.getByDisplayValue('燃料使用量')).toBeInTheDocument();
  });

  it('hides a column only once its row says the rule does not apply', async () => {
    const user = userEvent.setup();
    render(
      <TableForm
        defaultRows={[{ symbol: 'EL', definition: '電力量' }]}
        overrides={{
          columns: [
            ...COLUMNS,
            { name: 'hasDetail', label: '詳細あり', type: 'checkbox' },
            // What MultiStepForm produces for a row-relative @RequiredIf.
            {
              name: 'detail',
              label: '詳細',
              type: 'text',
              required: false,
              hideWhen: { field: 'hasDetail', value: false },
            },
          ],
        }}
      />,
    );

    // Unanswered: still reachable, because the server will ask for it as soon
    // as the operator ticks the box.
    expect(within(dataRows()[0]).getByLabelText('詳細')).toBeInTheDocument();

    await user.click(within(dataRows()[0]).getByLabelText('詳細あり'));
    expect(within(dataRows()[0]).getByLabelText('詳細')).toBeInTheDocument();

    await user.click(within(dataRows()[0]).getByLabelText('詳細あり'));
    expect(within(dataRows()[0]).queryByLabelText('詳細')).toBeNull();
  });
});
