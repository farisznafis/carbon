import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { MonitoringSolarForm } from './MonitoringSolarForm';
import { emptyFormData } from './emptyFormData';

const baseProps = {
  mode: 'create' as const,
  value: emptyFormData,
  onChange: vi.fn(),
  onNext: vi.fn(),
  onBack: vi.fn(),
};

describe('MonitoringSolarForm equipment selection', () => {
  it('auto-selects a single equipment and hides battery fields when it has no battery', async () => {
    const onNext = vi.fn();
    render(
      <MonitoringSolarForm
        {...baseProps}
        onNext={onNext}
        equipmentList={[
          {
            equipmentId: 100,
            deviceNo: 'PV-100',
            hasBattery: false,
          },
        ]}
      />,
    );

    expect(
      await screen.findByRole('combobox', { name: /設備ID/ }),
    ).toHaveTextContent('設備ID：100（PV-100）');
    expect(screen.queryByText('蓄電池交換の有無')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '確認' }));
    await waitFor(() =>
      expect(onNext).toHaveBeenCalledWith(
        expect.objectContaining({
          equipmentId: 100,
          equipmentHasBattery: false,
        }),
      ),
    );
  });

  it('shows battery controls when the selected equipment has a battery', async () => {
    render(
      <MonitoringSolarForm
        {...baseProps}
        equipmentList={[{ equipmentId: 101, hasBattery: true }]}
      />,
    );

    expect(await screen.findByText('蓄電池交換の有無')).toBeInTheDocument();
  });
});
