import { describe, expect, it } from 'vitest';
import type { Site } from '@/api/site';
import { sortSitesById } from './masterDashboardUtils';

describe('sortSitesById', () => {
  it('does not throw for stale cached data without siteId', () => {
    const staleSite = { siteName: '旧拠点' } as Site;
    const currentSite = { siteId: 'SITE-2', siteName: '新拠点' } as Site;

    expect(() => sortSitesById([currentSite, staleSite])).not.toThrow();
    expect(sortSitesById([currentSite, staleSite])).toHaveLength(2);
  });
});
