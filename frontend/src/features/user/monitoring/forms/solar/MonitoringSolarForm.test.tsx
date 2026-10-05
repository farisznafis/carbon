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
    expect(screen.queryByText('蓄電池放電量 (ELPJ)')).not.toBeInTheDocument();
    expect(screen.queryByText('蓄電池エビデンス')).not.toBeInTheDocument();

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

  it('fixes battery selection to あり and always shows required battery inputs', async () => {
    const onNext = vi.fn();
    render(
      <MonitoringSolarForm
        {...baseProps}
        value={{ ...emptyFormData, equipmentId: 101, changedBattery: false, ELPJ_battery_value: 0 }}
        onNext={onNext}
        equipmentList={[{ equipmentId: 101, hasBattery: true }]}
      />,
    );

    const batterySelect = await screen.findByRole('combobox', { name: /蓄電池交換の有無/ });
    expect(batterySelect).toBeDisabled();
    expect(batterySelect).toHaveTextContent('あり');
    expect(screen.getByRole('spinbutton', { name: /蓄電池放電量/ })).toBeRequired();
    expect(screen.getByText('蓄電池エビデンス')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '確認' }));
    await waitFor(() => expect(onNext).toHaveBeenCalledWith(
      expect.objectContaining({ equipmentHasBattery: true, changedBattery: true, ELPJ_battery_value: 0 }),
    ));
  });

  it('retains zero in numeric inputs and only clears an empty input to null', async () => {
    const onNext = vi.fn();
    render(
      <MonitoringSolarForm
        {...baseProps}
        onNext={onNext}
        equipmentList={[{ equipmentId: 100, hasBattery: false }]}
      />,
    );
    await waitFor(() => expect(screen.getByRole('combobox', { name: '設備ID' })).toHaveTextContent('設備ID：100'));
    const generationInput = screen.getByRole('spinbutton', { name: /発電量/ });

    fireEvent.change(generationInput, { target: { value: '0' } });
    expect(generationInput).toHaveValue(0);
    fireEvent.click(screen.getByRole('button', { name: '確認' }));
    await waitFor(() => expect(onNext).toHaveBeenCalledWith(
      expect.objectContaining({ ELpv_value: '0' }),
    ));

    onNext.mockClear();
    fireEvent.change(generationInput, { target: { value: '' } });
    fireEvent.click(screen.getByRole('button', { name: '確認' }));
    await waitFor(() => expect(onNext).toHaveBeenCalledWith(
      expect.objectContaining({ ELpv_value: null }),
    ));
  });
});
