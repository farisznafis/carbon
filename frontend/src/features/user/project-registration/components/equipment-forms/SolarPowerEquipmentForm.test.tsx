import { fireEvent, render, screen } from '@testing-library/react';
import { useForm } from 'react-hook-form';
import { describe, expect, it } from 'vitest';
import { Form } from '@/components/ui/form';
import { SOLAR_FORM_DEFAULT_VALUES } from '@/features/user/project-registration/solarEquipmentFormModel';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';
import { MAX_UPLOAD_FILE_SIZE_BYTES } from '@/lib/uploadFileValidation';
import { SolarPowerEquipmentForm } from './SolarPowerEquipmentForm';

/** Renders the shared equipment form with React Hook Form context. */
function SolarPowerEquipmentFormHarness() {
  const form = useForm<SolarPowerFormData>({
    defaultValues: { ...SOLAR_FORM_DEFAULT_VALUES },
  });

  return (
    <Form {...form}>
      <SolarPowerEquipmentForm
        form={form}
        sites={[]}
        isSiteOptionsLoading={false}
        siteOptionsError={null}
      />
    </Form>
  );
}

describe('SolarPowerEquipmentForm file validation', () => {
  it('shows a frontend warning and clears an oversized project-registration file', async () => {
    render(<SolarPowerEquipmentFormHarness />);
    const input = document.querySelector<HTMLInputElement>('#pcs-file');
    const oversizedFile = new File(['content'], 'pcs-spec.pdf');
    Object.defineProperty(oversizedFile, 'size', {
      value: MAX_UPLOAD_FILE_SIZE_BYTES + 1,
    });

    expect(input).not.toBeNull();
    expect(input).toHaveAttribute('accept', '.pdf,.csv,.jpg,.jpeg,.png');
    fireEvent.change(input!, { target: { files: [oversizedFile] } });

    expect(
      await screen.findByText('ファイルサイズは10MB以下にしてください。'),
    ).toBeVisible();
    expect(screen.queryByText('pcs-spec.pdf')).not.toBeInTheDocument();
  });

  it('clears the previous valid file when an unsupported replacement is selected', async () => {
    render(<SolarPowerEquipmentFormHarness />);
    const input = document.querySelector<HTMLInputElement>('#pcs-file');
    const supportedFile = new File(['image'], 'pcs-spec.jpg');
    const unsupportedFile = new File(['slides'], 'pcs-spec.pptx');

    fireEvent.change(input!, { target: { files: [supportedFile] } });
    expect(await screen.findByText('pcs-spec.jpg')).toBeVisible();

    fireEvent.change(input!, { target: { files: [unsupportedFile] } });

    expect(
      await screen.findByText(
        '対応していないファイル形式です。（.pdf, .csv, .jpg, .jpeg, .png）',
      ),
    ).toBeVisible();
    expect(screen.queryByText('pcs-spec.jpg')).not.toBeInTheDocument();
    expect(screen.queryByText('pcs-spec.pptx')).not.toBeInTheDocument();
  });
});
