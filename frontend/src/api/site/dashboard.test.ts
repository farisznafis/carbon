import { afterEach, describe, expect, it, vi } from 'vitest';
import { getSiteData } from './dashboard';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('getSiteData', () => {
  it('maps person and address fields from nested site data', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          statusCode: 'OK',
          site: {
            siteId: 'site001',
            siteName: '',
            blocknum: '1-2-3',
            buildingName: '神宮前ビル',
            data: {
              siteId: 'site001',
              siteName: '渋谷本社太陽光発電所',
              postalCode: '150-0001',
              prefecture: '東京都',
              municipality: '渋谷区神宮前',
              picFirstName: '明子',
              picLastName: '伊藤',
              sumEmissionReduction: '0.5',
              sumEmission: '1.5',
              jcredit: '1',
            },
          },
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await getSiteData('site001');

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/site001',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(result.site).toEqual(
      expect.objectContaining({
        siteId: 'site001',
        siteName: '渋谷本社太陽光発電所',
        picFirstName: '明子',
        picLastName: '伊藤',
        address: '〒150-0001東京都渋谷区神宮前1-2-3神宮前ビル',
      }),
    );
    expect(result.metrics).toEqual(
      expect.objectContaining({
        sumEmissionReduction: '0.5',
        sumEmission: '1.5',
        jcredit: '1',
      }),
    );
  });
});
