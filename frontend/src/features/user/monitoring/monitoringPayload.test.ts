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
      {
        ...context,
        hasBattery: false,
        evidenceUrls: { elpj_battery_evidence: 'https://example.com/battery.pdf' },
      },
    );

    expect(payload.equipmentId).toBe(100);
    expect(payload).not.toHaveProperty('changedBattery');
    expect(payload).not.toHaveProperty('ELPJ_battery_value');
    expect(payload).not.toHaveProperty('elpj_battery_evidence');
  });

  it.each([true, false, null, undefined])(
    'always includes battery fields for battery equipment despite changedBattery=%s',
    (changedBattery) => {
      const payload = buildMonitoringPayload(
        {
          ...baseForm,
          changedBattery,
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
    },
  );

  it('uses uploaded evidence URLs under the exact DTO keys', () => {
    const file = new File(['evidence'], 'evidence.pdf', { type: 'application/pdf' });
    const evidenceUrls = {
      elpv_evidence: 'https://example.com/pv.pdf',
      elpvr_evidence: 'https://example.com/pvr.pdf',
      elpj_control_evidence: 'https://example.com/control.pdf',
      elpj_battery_evidence: 'https://example.com/battery.pdf',
    };
    const payload = buildMonitoringPayload(
      {
        ...baseForm,
        elpv_evidence: file,
        elpvr_evidence: file,
        elpj_control_evidence: file,
        elpj_battery_evidence: file,
        changedBattery: false,
        ELPJ_battery_value: '8',
      },
      { ...context, hasBattery: true, evidenceUrls },
    );

    expect(payload).toMatchObject(evidenceUrls);
    expect(Object.keys(payload).filter((key) => key.includes('evidence')).sort()).toEqual(
      Object.keys(evidenceUrls).sort(),
    );
  });

  it('sends every numeric value as zero when the input is "0"', () => {
    const payload = buildMonitoringPayload(
      {
        ...baseForm,
        ELpv_value: '0',
        ELpvr_value: '0',
        ELPJ_battery_value: '0',
        ELPJ_control_value: '0',
        ELPJ_accuracy: '0',
      },
      { ...context, hasBattery: true },
    );

    expect(payload).toMatchObject({
      ELpv_value: 0,
      ELpvr_value: 0,
      ELPJ_battery_value: 0,
      ELPJ_control_value: 0,
      ELPJ_accuracy: 0,
    });
  });

  it.each(['', null, undefined])('keeps an empty numeric value %s as null', (value) => {
    const payload = buildMonitoringPayload(
      { ...baseForm, ELpv_value: value },
      { ...context, hasBattery: false },
    );

    expect(payload.ELpv_value).toBeNull();
  });
});
