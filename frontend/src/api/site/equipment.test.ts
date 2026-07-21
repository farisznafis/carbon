import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  uploadSiteProjectEquipmentFile,
  validateSiteProjectEquipments,
} from './equipment';
import type { SiteProjectEquipmentValidationItem } from './types';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('validateSiteProjectEquipments', () => {
  it('posts the equipment array to the validation endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ statusCode: '200' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);
    const payload: SiteProjectEquipmentValidationItem[] = [
      {
        tempId: 'temp-1',
        methodologyNumber: 'EN-S-001',
        equipmentOpDate: '2026-07-17',
        nominalMaxOutput: 10,
        pcsManufacturer: 'Maker',
        pcsModel: 'Model',
        powerCondUnitCount: 1,
        deviceNo: 'DEVICE-01',
        powerCondStandby: 2,
        hasBattery: false,
        additionalEquipment: false,
        ictEquipmentPowerConsumption: 0,
        totalInvestment: 1000,
        hasSubsidy: false,
        preProjectUnitPrice: 30,
        files: [],
      },
    ];

    await validateSiteProjectEquipments(payload);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/project/equipments/validate',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    );
  });
});

describe('uploadSiteProjectEquipmentFile', () => {
  it('uploads multipart data and returns validation file metadata', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          statusCode: '200',
          s3Key: 'equipment/temp/catalog.pdf',
          fileId: 'file-1',
          originalName: 'catalog.pdf',
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await uploadSiteProjectEquipmentFile(
      'SITE-1',
      '2',
      'panel_catalog',
      new File(['catalog'], 'catalog.pdf', { type: 'application/pdf' }),
    );

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/project/SITE-1/2/files/upload?fileType=panel_catalog',
      expect.objectContaining({ method: 'POST', body: expect.any(FormData) }),
    );
    expect(result).toEqual({
      fileType: 'panel_catalog',
      s3Key: 'equipment/temp/catalog.pdf',
      fileName: 'catalog.pdf',
      fileId: 'file-1',
    });
  });
});
