import { describe, expect, it } from 'vitest';
import { mapSiteListItem } from './mappers';
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
});
