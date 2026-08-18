import { describe, expect, it } from 'vitest';
import { buildMonitoringPayload } from './monitoringPayload';

const baseForm = {
  equipmentId: 100,
  dateRecorded: '2026-08-07',
  ELpv_value: '10',
  ELpvr_value: '5',
  ELPJ_control_value: '2',
  ELPJ_accuracy: '99',
  elpv_evidence: 'pv.pdf',
  elpvr_evidence: 'pvr.pdf',
  elpj_control_evidence: 'control.pdf',
};

const context = {
  projectType: 'solar',
  projectId: 'PROJ0001',
  siteId: '21',
};

describe('buildMonitoringPayload', () => {
  it('includes equipmentId and omits every battery field for equipment without a battery', () => {
    const payload = buildMonitoringPayload(
      {
        ...baseForm,
        changedBattery: true,
        ELPJ_battery_value: '8',
        elpj_battery_evidence: 'battery.pdf',
      },
      { ...context, hasBattery: false },
    );

    expect(payload.equipmentId).toBe(100);
    expect(payload).not.toHaveProperty('changedBattery');
    expect(payload).not.toHaveProperty('ELPJ_battery_value');
    expect(payload).not.toHaveProperty('elpj_battery_evidence');
  });

  it('includes battery fields when the selected equipment has a changed battery', () => {
    const payload = buildMonitoringPayload(
      {
        ...baseForm,
        changedBattery: true,
        ELPJ_battery_value: '8',
        elpj_battery_evidence: 'battery.pdf',
      },
      { ...context, hasBattery: true },
    );

    expect(payload).toEqual(
      expect.objectContaining({
        equipmentId: 100,
        changedBattery: true,
        ELPJ_battery_value: 8,
        elpj_battery_evidence: 'battery.pdf',
      }),
    );
  });

  it('keeps changedBattery null so backend validation remains authoritative', () => {
    const payload = buildMonitoringPayload(baseForm, {
      ...context,
      hasBattery: true,
    });

    expect(payload.changedBattery).toBeNull();
  });
});
