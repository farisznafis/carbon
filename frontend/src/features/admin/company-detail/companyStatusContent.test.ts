import { describe, expect, it } from 'vitest';
import {
  COMPANY_APPLICATION_STATUSES,
  COMPANY_STATUSES,
  resolveCompanyStatusFilter,
} from './companyStatusContent';

describe('resolveCompanyStatusFilter', () => {
  it('returns a status the viewer may see', () => {
    expect(resolveCompanyStatusFilter('PENDING', COMPANY_STATUSES)).toBe('PENDING');
    expect(resolveCompanyStatusFilter('SUSPENDED', COMPANY_STATUSES)).toBe('SUSPENDED');
  });

  it('falls back to all when the query is missing or unknown', () => {
    expect(resolveCompanyStatusFilter(null, COMPANY_STATUSES)).toBe('all');
    expect(resolveCompanyStatusFilter('all', COMPANY_STATUSES)).toBe('all');
    expect(resolveCompanyStatusFilter('UNKNOWN', COMPANY_STATUSES)).toBe('all');
  });

  it('falls back to all when the status is outside the viewer permissions', () => {
    expect(resolveCompanyStatusFilter('ACTIVE', COMPANY_APPLICATION_STATUSES)).toBe('all');
  });
});
