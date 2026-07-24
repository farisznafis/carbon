import { normalizeRecentMonitoringItem } from './mappers';

describe('normalizeRecentMonitoringItem', () => {
  it('preserves the Project public ID without numeric conversion', () => {
    const monitoringItem = normalizeRecentMonitoringItem({
      projectId: 'PROJ0123',
      dateRecorded: '2026-07-23',
    });

    expect(monitoringItem.projectId).toBe('PROJ0123');
  });
});
