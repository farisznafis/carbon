import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useForm, FormProvider } from 'react-hook-form';
import { describe, expect, it, vi } from 'vitest';
import { DynamicFormField } from './DynamicFormField';
import {
  PlanFileUploadProvider,
  type PlanFileUploader,
} from './PlanFileUploadContext';

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

type FileFormValues = {
  evidenceImg: string;
};

function FileField({
  uploader,
  onValue,
}: {
  uploader: PlanFileUploader | null;
  onValue: (value: string) => void;
}) {
  const form = useForm<FileFormValues>({ defaultValues: { evidenceImg: '' } });
  onValue(form.watch('evidenceImg'));

  return (
    <FormProvider {...form}>
      <PlanFileUploadProvider uploader={uploader}>
        <DynamicFormField<FileFormValues>
          field={{ name: 'evidenceImg', label: '根拠資料', type: 'file' }}
        />
      </PlanFileUploadProvider>
    </FormProvider>
  );
}

describe('DynamicFormField file input', () => {
  const png = () =>
    new File(['x'], 'evidence.png', { type: 'image/png' });

  it('stores the uploaded key, not the file itself', async () => {
    const user = userEvent.setup();
    const uploader = vi.fn().mockResolvedValue({
      s3Key: 'tmp/projectplan/abc/evidenceImg_evidence.png',
      originalName: 'evidence.png',
    });
    let latest = '';

    render(<FileField uploader={uploader} onValue={(v) => (latest = v)} />);
    await user.upload(screen.getByLabelText('根拠資料*'), png());

    // The backend validates this field as text, so holding the File would send
    // an empty object and the page could never save.
    await waitFor(() =>
      expect(latest).toBe('tmp/projectplan/abc/evidenceImg_evidence.png'),
    );
    expect(uploader).toHaveBeenCalledWith('evidenceImg', expect.any(File));
    expect(await screen.findByText(/添付済み: evidence.png/)).toBeInTheDocument();
  });

  it('reports an upload failure instead of silently keeping the field empty', async () => {
    const user = userEvent.setup();
    const uploader = vi.fn().mockRejectedValue(new Error('アップロードに失敗しました。'));
    let latest = 'untouched';

    render(<FileField uploader={uploader} onValue={(v) => (latest = v)} />);
    await user.upload(screen.getByLabelText('根拠資料*'), png());

    expect(
      await screen.findByText('アップロードに失敗しました。'),
    ).toBeInTheDocument();
    expect(latest).toBe('');
  });
});

function ConfiguredField({
  required,
  disabled,
}: {
  required?: boolean;
  disabled?: boolean;
}) {
  const form = useForm<TestFormValues>({ defaultValues: { projectName: '' } });

  return (
    <FormProvider {...form}>
      <DynamicFormField<TestFormValues>
        field={{
          name: 'projectName',
          label: 'プロジェクト名称',
          type: 'text',
          required,
          disabled,
        }}
      />
    </FormProvider>
  );
}

describe('DynamicFormField required and disabled', () => {
  it('drops the required marker when the field carries no requirement', () => {
    // A field under a 有/無 rule is only required while its toggle holds the
    // value the rule keys on; marked otherwise, the operator hunts for
    // something the server is not asking for.
    render(<ConfiguredField required={false} />);

    expect(screen.queryByText('*')).not.toBeInTheDocument();
    expect(screen.getByLabelText('プロジェクト名称')).toHaveAttribute(
      'aria-required',
      'false',
    );
  });

  it('locks a field the form does not own', () => {
    // readOnlyFields sets this. It had no effect at all until FieldConfig
    // actually carried a flat `disabled`.
    render(<ConfiguredField disabled />);

    expect(screen.getByLabelText('プロジェクト名称*')).toBeDisabled();
  });
});
