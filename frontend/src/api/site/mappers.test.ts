import { describe, expect, it } from 'vitest';
import { mapSiteDashboard, mapSiteListItem } from './mappers';
import type { SiteListItem } from './types';

describe('mapSiteListItem', () => {
  it('prefers the public siteId', () => {
    const site = mapSiteListItem({
      siteId: 'SITE-public-id',
      id: '12',
      siteName: '東京拠点',
    } as SiteListItem);

    expect(site.siteId).toBe('SITE-public-id');
  });

  it('falls back to the legacy id', () => {
    const site = mapSiteListItem({
      id: '12',
      siteName: '東京拠点',
    } as SiteListItem);

    expect(site.siteId).toBe('12');
  });

  it('maps picUserId to picId and keeps it empty when null', () => {
    expect(
      mapSiteListItem({ siteId: 'SITE-1', siteName: '東京拠点', picUserId: 'USR-1' }).picId,
    ).toBe('USR-1');
    expect(
      mapSiteListItem({ siteId: 'SITE-1', siteName: '東京拠点', picUserId: null }).picId,
    ).toBe('');
  });
});

describe('mapSiteDashboard', () => {
  it('maps picUserId to picId', () => {
    expect(
      mapSiteDashboard({ siteId: 'SITE-1', siteName: '東京拠点', picUserId: 'USR-1' }).picId,
    ).toBe('USR-1');
  });
});
