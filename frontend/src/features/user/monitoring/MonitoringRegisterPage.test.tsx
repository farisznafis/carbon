import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  submitMonitoringData,
  uploadElpjBatteryFile,
  uploadElpjControlFile,
  uploadElpvFile,
  uploadElpvrFile,
  validateMonitoringData,
} from '@/api/monitoring';
import { getSiteEquipmentList } from '@/api/site';
import { paths } from '@/routes/paths';
import MonitoringRegister from './MonitoringRegisterPage';

vi.mock('@/api/site', () => ({ getSiteEquipmentList: vi.fn() }));
vi.mock('@/api/monitoring', () => ({
  validateMonitoringData: vi.fn().mockResolvedValue(undefined),
  submitMonitoringData: vi.fn().mockResolvedValue({ success: true, message: '', dataId: 'MON0001' }),
  uploadElpvFile: vi.fn().mockResolvedValue('https://example.com/pv.pdf'),
  uploadElpvrFile: vi.fn().mockResolvedValue('https://example.com/pvr.pdf'),
  uploadElpjControlFile: vi.fn().mockResolvedValue('https://example.com/control.pdf'),
  uploadElpjBatteryFile: vi.fn().mockResolvedValue('https://example.com/battery.pdf'),
}));
vi.mock('@/hooks/use-toast', () => ({ useToast: () => ({ toast: vi.fn() }) }));

describe('Monitoring registration evidence upload', () => {
  beforeEach(() => vi.clearAllMocks());

  it.each([true, false])('uploads evidence and sends the DTO for equipment.hasBattery=%s', async (hasBattery) => {
    vi.mocked(getSiteEquipmentList).mockResolvedValue([{ equipmentId: 100, hasBattery }]);
    const { container } = render(
      <MemoryRouter
        initialEntries={[
          paths.userDashboard,
          { pathname: paths.monitoringRegister('solar'), state: { projectId: 'PROJ0001', siteId: 'SITE0001' } },
        ]}
        initialIndex={1}
      >
        <Routes>
          <Route path={paths.monitoringRegisterRoute} element={<MonitoringRegister />} />
          <Route path={paths.userDashboard} element={<div>登録完了</div>} />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByRole('combobox', { name: '設備ID' })).toHaveTextContent('設備ID：100'));

    for (const input of screen.getAllByRole('spinbutton')) {
      fireEvent.change(input, { target: { value: '0' } });
    }
    const evidenceFile = new File(['evidence'], 'evidence.pdf', { type: 'application/pdf' });
    const fileInputIds = ['elpv-file', 'elpvr-file', 'elpj-control-file'];
    if (hasBattery) fileInputIds.push('elpj-battery-file');
    for (const inputId of fileInputIds) {
      const fileInput = container.querySelector<HTMLInputElement>(`#${inputId}`);
      expect(fileInput).not.toBeNull();
      fireEvent.change(fileInput!, { target: { files: [evidenceFile] } });
    }

    fireEvent.click(screen.getByRole('button', { name: '確認' }));
    const confirmButton = await screen.findByRole('button', { name: '確定' });
    expect(validateMonitoringData).toHaveBeenCalledWith(expect.objectContaining({
      ELpv_value: 0,
      ELpvr_value: 0,
      elpv_evidence: evidenceFile.name,
      elpvr_evidence: evidenceFile.name,
      elpj_control_evidence: evidenceFile.name,
    }));
    const validationPayload = vi.mocked(validateMonitoringData).mock.calls[0][0];
    expect(validationPayload).not.toHaveProperty('changedBattery');
    expect(validationPayload).toMatchObject(hasBattery
      ? { ELPJ_battery_value: 0, elpj_battery_evidence: evidenceFile.name }
      : { ELPJ_battery_value: null, elpj_battery_evidence: null });
    fireEvent.click(confirmButton);

    await waitFor(() => expect(submitMonitoringData).toHaveBeenCalledTimes(1));
    for (const upload of [uploadElpvFile, uploadElpvrFile, uploadElpjControlFile]) {
      expect(upload).toHaveBeenCalledWith(
        evidenceFile, 'PROJ0001', 'SITE0001', '', expect.objectContaining({ onProgress: expect.any(Function) }),
      );
    }
    const payload = vi.mocked(submitMonitoringData).mock.calls[0][0];
    expect(payload).not.toHaveProperty('changedBattery');
    expect(payload).toMatchObject({
      ELpv_value: 0,
      ELpvr_value: 0,
      elpv_evidence: 'https://example.com/pv.pdf',
      elpvr_evidence: 'https://example.com/pvr.pdf',
      elpj_control_evidence: 'https://example.com/control.pdf',
    });
    if (hasBattery) {
      expect(uploadElpjBatteryFile).toHaveBeenCalledTimes(1);
      expect(payload).toMatchObject({
        ELPJ_battery_value: 0,
        elpj_battery_evidence: 'https://example.com/battery.pdf',
      });
    } else {
      expect(uploadElpjBatteryFile).not.toHaveBeenCalled();
      expect(payload).not.toHaveProperty('changedBattery');
      expect(payload.ELPJ_battery_value).toBeNull();
      expect(payload.elpj_battery_evidence).toBeNull();
    }
  });
});
