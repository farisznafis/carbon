import { describe, expect, it } from 'vitest';
import {
  COMPANY_APPLICATION_STATUSES,
  COMPANY_PARTICIPATING_STATUSES,
  COMPANY_STATUSES,
  getCompanyFilterStatuses,
  getCompanyStatusGroup,
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

describe('company status groups', () => {
  it('resolves a group only when the viewer can see both groups', () => {
    expect(resolveCompanyStatusFilter('participating', COMPANY_STATUSES)).toBe('participating');
    expect(resolveCompanyStatusFilter('applications', COMPANY_STATUSES)).toBe('applications');
    // 片方の区分しか見えない場合、区分の絞り込みは「すべて」と同じになる
    expect(resolveCompanyStatusFilter('applications', COMPANY_APPLICATION_STATUSES)).toBe('all');
    expect(resolveCompanyStatusFilter('participating', COMPANY_PARTICIPATING_STATUSES)).toBe('all');
  });

  it('expands a group into the statuses counted on the home card', () => {
    expect(getCompanyFilterStatuses('participating', COMPANY_STATUSES)).toEqual(['ACTIVE', 'SUSPENDED']);
    expect(getCompanyFilterStatuses('applications', COMPANY_STATUSES)).toEqual([
      'PENDING',
      'APPROVED_PENDING_SETUP',
      'REJECTED',
    ]);
    expect(getCompanyFilterStatuses('PENDING', COMPANY_STATUSES)).toEqual(['PENDING']);
    expect(getCompanyFilterStatuses('all', COMPANY_STATUSES)).toEqual(COMPANY_STATUSES);
  });

  it('maps a single status to its group', () => {
    expect(getCompanyStatusGroup('SUSPENDED')).toBe('participating');
    expect(getCompanyStatusGroup('REJECTED')).toBe('applications');
    expect(getCompanyStatusGroup('all')).toBe('all');
  });
});
