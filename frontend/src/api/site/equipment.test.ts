import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  getSiteEquipmentById,
  getSiteEquipmentHistory,
  getSiteProjectEquipment,
  getSiteProjectEquipmentPresignedUrl,
  getSiteEquipmentList,
  joinSiteProject,
  uploadSiteProjectEquipmentFile,
  updateSiteEquipment,
  validateSiteProjectConfirmationItems,
  validateSiteProjectEquipment,
  validateSiteProjectFinalAgreement,
  validateSiteProjectSustainabilityCommitment,
} from './equipment';
import type {
  SiteProjectEquipmentValidationItem,
  SiteProjectJoinRequest,
  SiteProjectEquipmentUpdateRequest,
} from './types';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('validateSiteProjectEquipment', () => {
  it('posts one equipment object to the validation endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ statusCode: '200' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);
    const payload: SiteProjectEquipmentValidationItem = {
      tempId: 'temp-1',
      siteId: 'SITE4f83ac050b20d755',
      equipmentOpDate: '2026-07-17',
      nominalMaxOutput: 10,
      pcsManufacturer: 'Maker',
      pcsModel: 'Model',
      pcsUnitCount: 1,
      deviceNo: 'DEVICE-01',
      pcsStandby: 2,
      hasBattery: false,
      totalInvestment: 1000,
      hasSubsidy: false,
      preProjectUnitPrice: 30,
      files: [],
    };

    await validateSiteProjectEquipment(payload);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/project/equipment/validate',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    );
  });
});

describe('site equipment read and update', () => {
  it.each([
    ['array', [{ id: 10, deviceNo: 'PV-10', hasBattery: true }]],
    [
      'wrapped response',
      { equipments: [{ id: 11, deviceNo: 'PV-11', hasBattery: false }] },
    ],
  ])('gets a site equipment list from a %s response', async (_label, body) => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(body), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await getSiteEquipmentList('SITE A');

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/SITE%20A/equipment',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(result[0]).toEqual(
      expect.objectContaining({
        equipmentId: _label === 'array' ? 10 : 11,
        hasBattery: _label === 'array',
      }),
    );
  });

  it('normalizes backend equipment field names for project equipment lists', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          statusCode: '200',
          equipments: [
            {
              id: 12,
              pcsUnitCount: 3,
              pcsStandby: 0,
              nominalMaxOutput: 25.5,
            },
          ],
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await getSiteProjectEquipment('SITE A', 'PROJECT/1');

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/SITE%20A/PROJECT%2F1/equipment',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(result[0]).toEqual(
      expect.objectContaining({
        equipmentId: 12,
        powerCondUnitCount: 3,
        powerCondStandby: 0,
      }),
    );
  });

  it('gets equipment details and normalizes backend aliases', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({ id: 7, pcsUnitCount: 2, pcsStandby: 1.5 }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await getSiteEquipmentById('SITE A', 7);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/SITE%20A/equipment/7',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(result).toEqual(
      expect.objectContaining({
        equipmentId: 7,
        powerCondUnitCount: 2,
        powerCondStandby: 1.5,
      }),
    );
  });

  it('normalizes existing file metadata regardless of backend key casing', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          id: 7,
          Files: [
            {
              id: 21,
              FileType: 'pcs_spec',
              FileId: 'existing-pcs-id',
              S3Key: 'equipment/existing-pcs.pdf',
              FileName: 'PCS仕様書.pdf',
            },
          ],
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await getSiteEquipmentById('SITE-1', 7);

    expect(result.files).toEqual([
      {
        id: 21,
        fileType: 'pcs_spec',
        fileId: 'existing-pcs-id',
        s3Key: 'equipment/existing-pcs.pdf',
        fileName: 'PCS仕様書.pdf',
      },
    ]);
  });

  it('preserves history metadata and files from a site equipment list', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify([
          {
            id: 1,
            siteId: 'SITE-1',
            changeDate: '2026-08-13',
            previousEquipmentId: 0,
            files: [
              {
                id: 20,
                fileType: 'pcs_spec',
                fileId: 'file-20',
                s3Key: 'equipment/pcs.pdf',
              },
            ],
          },
        ]),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await getSiteEquipmentList('SITE-1');

    expect(result[0]).toEqual(
      expect.objectContaining({
        equipmentId: 1,
        siteId: 'SITE-1',
        changeDate: '2026-08-13',
        previousEquipmentId: 0,
        files: [
          {
            id: 20,
            fileType: 'pcs_spec',
            fileId: 'file-20',
            s3Key: 'equipment/pcs.pdf',
            fileName: null,
          },
        ],
      }),
    );
  });

  it('gets equipment history and normalizes nested equipment data', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify([
          {
            id: 31,
            changeDate: '2026-08-13',
            previousEquipmentId: 6,
            monitoringStartDate: '2026-08-13',
            monitoringEndDate: null,
            isActive: false,
            registeredBy: 'operator-1',
            registeredAt: '2026-08-13T01:39:59.538Z',
            equipment: {
              id: 7,
              pcsUnitCount: 0,
              pcsStandby: 0,
              hasBattery: false,
              Files: [
                {
                  id: 21,
                  FileType: 'pcs_spec',
                  FileId: 'file-21',
                  S3Key: 'equipment/pcs.pdf',
                  FileName: 'PCS仕様書.pdf',
                },
              ],
            },
          },
        ]),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await getSiteEquipmentHistory('SITE A', 7);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/SITE%20A/equipment/7/history',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(result[0]).toEqual(
      expect.objectContaining({
        id: 31,
        previousEquipmentId: 6,
        isActive: false,
        equipment: expect.objectContaining({
          equipmentId: 7,
          powerCondUnitCount: 0,
          powerCondStandby: 0,
          hasBattery: false,
          files: [
            {
              id: 21,
              fileType: 'pcs_spec',
              fileId: 'file-21',
              s3Key: 'equipment/pcs.pdf',
              fileName: 'PCS仕様書.pdf',
            },
          ],
        }),
      }),
    );
  });

  it('patches only the supplied equipment fields and preserves false and zero', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ id: 7, hasBattery: false, pcsStandby: 0 }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);
    const payload: SiteProjectEquipmentUpdateRequest = {
      hasBattery: false,
      pcsStandby: 0,
    };

    await updateSiteEquipment('SITE A', 7, payload);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/SITE%20A/equipment/7',
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify(payload),
      }),
    );
  });
});

describe('validateSiteProjectConfirmationItems', () => {
  it('posts confirmation items to the dedicated validation endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ statusCode: '200' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);
    const payload = {
      id: 'PROJ0002',
      processedBy: 'user-1',
      companyId: 'PC000000001',
      isAgreeDataUse: null,
      isAgreeExtraData: null,
      isAgreeTransfer: null,
      isNotRegistered: null,
      isKeidanrenParticipation: null,
      isEmitter: null,
      emitterCode: '',
      isAdjustedEmissionsAdded: null,
      isSubjectToPeriodicReport: null,
      isPeriodicReportTypeOne: null,
      energyBusinessCode: '',
      factoryCode: '',
    };

    await validateSiteProjectConfirmationItems(payload);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/project/confirmation-items/validate',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    );
  });
});

describe('validateSiteProjectSustainabilityCommitment', () => {
  it('posts the sustainability commitment to its validation endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ statusCode: '200' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);
    const payload = {
      companyName: 'Company',
      repName: 'Representative',
      repRole: 'Director',
      hasConsent: true,
    };

    await validateSiteProjectSustainabilityCommitment(payload);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/project/sustainability-commitment/validate',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    );
  });
});

describe('validateSiteProjectFinalAgreement', () => {
  it('posts the final agreement before project submission', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ statusCode: '200' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);
    const payload = { isAgreeTerms: true };

    await validateSiteProjectFinalAgreement(payload);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/project/final/validate',
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
      'panel_catalog',
      new File(['catalog'], 'catalog.pdf', { type: 'application/pdf' }),
    );

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/project/sites/SITE-1/files/upload?fileType=panel_catalog',
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

describe('getSiteProjectEquipmentPresignedUrl', () => {
  it.each([
    ['plain text', 'https://storage.example.com/equipment.pdf'],
    ['JSON string', JSON.stringify('https://storage.example.com/equipment.pdf')],
  ])('returns a valid URL from a %s response', async (_label, responseBody) => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(responseBody, {
        status: 200,
        headers: { 'Content-Type': 'text/plain' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    const result = await getSiteProjectEquipmentPresignedUrl(
      'tmp/equipments/file 1/document.pdf',
    );

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/project/presigned-url?s3Key=tmp%2Fequipments%2Ffile%201%2Fdocument.pdf',
      expect.objectContaining({ method: 'GET' }),
    );
    expect(result).toBe('https://storage.example.com/equipment.pdf');
  });
});

describe('joinSiteProject', () => {
  it('posts the complete registration payload to the project join endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ statusCode: '200', participationId: 10 }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);
    const payload: SiteProjectJoinRequest = {
      equipment: {
        tempId: 'temp-1',
        siteId: 'SITE-1',
        equipmentOpDate: '2026-07-22',
        nominalMaxOutput: 10,
        pcsManufacturer: 'Maker',
        pcsModel: 'Model',
        pcsUnitCount: 1,
        deviceNo: 'DEVICE-1',
        pcsStandby: 1,
        hasBattery: false,
        totalInvestment: 1000,
        hasSubsidy: false,
        preProjectUnitPrice: 20,
        files: [
          {
            fileType: 'solar_contract',
            s3Key: 'tmp/equipments/file-1/contract.pdf',
            fileName: 'contract.pdf',
            fileId: 'file-1',
          },
        ],
      },
      confirmationItems: {
        id: 'PROJ0002',
        processedBy: 'user-1',
        companyId: 'PC000000001',
        isAgreeDataUse: true,
        isAgreeExtraData: true,
        isAgreeTransfer: true,
        isNotRegistered: true,
        isKeidanrenParticipation: false,
        isEmitter: false,
        isAdjustedEmissionsAdded: false,
        isSubjectToPeriodicReport: false,
        isPeriodicReportTypeOne: false,
        emitterCode: '',
        energyBusinessCode: '',
        factoryCode: '',
      },
      sustainabilityCommitment: {
        companyName: 'Company',
        repName: 'Representative',
        repRole: 'Director',
        hasConsent: true,
      },
      finalAgreement: {
        isAgreeTerms: true,
      },
    };

    await joinSiteProject('PROJ0002', payload);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/project/projects/PROJ0002/join',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    );
  });
});
