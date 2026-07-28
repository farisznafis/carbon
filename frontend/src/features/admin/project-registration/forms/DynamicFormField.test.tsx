import { render, screen } from '@testing-library/react';
import { useForm, FormProvider } from 'react-hook-form';
import { describe, expect, it } from 'vitest';
import { DynamicFormField } from './DynamicFormField';

type TestFormValues = {
  projectName: string;
};

function TestField() {
  const form = useForm<TestFormValues>({
    defaultValues: { projectName: '' },
  });

  return (
    <FormProvider {...form}>
      <DynamicFormField<TestFormValues>
        field={{
          name: 'projectName',
          label: 'プロジェクト名称',
          type: 'text',
        }}
      />
    </FormProvider>
  );
}

describe('DynamicFormField', () => {
  it('shows the shared required marker and accessibility state by default', () => {
    render(<TestField />);

    expect(screen.getByText('*')).toHaveClass('text-destructive');
    expect(screen.getByLabelText('プロジェクト名称*')).toHaveAttribute(
      'aria-required',
      'true',
    );
  });
});
