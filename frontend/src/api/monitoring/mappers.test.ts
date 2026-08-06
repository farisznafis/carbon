import {
  normalizeMonitoringRecordListItem,
  normalizeRecentMonitoringItem,
} from './mappers';

describe('normalizeRecentMonitoringItem', () => {
  it('preserves the Project public ID without numeric conversion', () => {
    const monitoringItem = normalizeRecentMonitoringItem({
      projectId: 'PROJ0123',
      dateRecorded: '2026-07-23',
    });

    expect(monitoringItem.projectId).toBe('PROJ0123');
  });
});

describe('normalizeMonitoringRecordListItem', () => {
  it('maps the current API field names and preserves every detail field', () => {
    const monitoringItem = normalizeMonitoringRecordListItem({
      id: 'MON0001',
      projectId: 'PROJ0123',
      siteId: 'SITE0001',
      participatingUserId: '42',
      timeUpdated: '2026-07-23T12:30:00Z',
      dateRecorded: '2026-07-23',
      elpjControlValue: '10.5',
      elpjControlEquipmentSpecFiles: 'control.pdf',
      elpjBatteryValue: '20',
      elpjAccuracy: '99.5',
      elpjBatteryEvidence: 'battery.pdf',
      elPv: '30',
      elPvEvidence: 'elpv.pdf',
      elPvr: '15',
      elPvrEvidence: 'elpvr.pdf',
      elPvEvidenceUrl: 'https://example.com/elpv.pdf',
      elPvrEvidenceUrl: 'https://example.com/elpvr.pdf',
      elpjBatterySpecificationUrl: 'https://example.com/battery.pdf',
      elpjControlEvidenceUrl: 'https://example.com/control.pdf',
      changedBattery: 'false',
      isActive: 'true',
      processedAt: '2026-07-23T13:00:00Z',
      methodologyType: 'EN-R-002',
    });

    expect(monitoringItem).toEqual({
      id: 'MON0001',
      projectId: 'PROJ0123',
      siteId: 'SITE0001',
      participatingUserId: 42,
      timeUpdated: '2026-07-23T12:30:00Z',
      dateRecorded: '2026-07-23',
      elpjControlValue: 10.5,
      elpjControlEquipmentSpecFiles: 'control.pdf',
      elpjBatteryValue: 20,
      elpjAccuracy: 99.5,
      elpjBatteryEvidence: 'battery.pdf',
      elPv: 30,
      elPvEvidence: 'elpv.pdf',
      elPvr: 15,
      elPvrEvidence: 'elpvr.pdf',
      elPvEvidenceUrl: 'https://example.com/elpv.pdf',
      elPvrEvidenceUrl: 'https://example.com/elpvr.pdf',
      elpjBatterySpecificationUrl: 'https://example.com/battery.pdf',
      elpjControlEvidenceUrl: 'https://example.com/control.pdf',
      changedBattery: false,
      isActive: true,
      processedAt: '2026-07-23T13:00:00Z',
      methodologyType: 'EN-R-002',
    });
  });
});
