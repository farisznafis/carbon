import { describe, expect, it } from 'vitest';
import {
  getActiveAdminNavigationKey,
  getAdminRouteFromLegacyTab,
} from './adminNavigationConfig';

describe('admin navigation routing', () => {
  it('keeps nested admin routes in their parent navigation group', () => {
    expect(getActiveAdminNavigationKey('/admin/projects/PRJ-1')).toBe('projects');
    expect(getActiveAdminNavigationKey('/admin/companies/COMP-1')).toBe('companies');
    expect(getActiveAdminNavigationKey('/admin/operators/12')).toBe('operators');
    expect(
      getActiveAdminNavigationKey('/admin/monitoring/cancel-requests/4'),
    ).toBe('home');
  });

  it('maps legacy dashboard tabs to the new routes', () => {
    expect(getAdminRouteFromLegacyTab('overview')).toBe('/admin/dashboard');
    expect(getAdminRouteFromLegacyTab('operators')).toBe('/admin/operators');
    expect(getAdminRouteFromLegacyTab('monitoring')).toBe('/admin/dashboard?panel=cancel');
  });
});
