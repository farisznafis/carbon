import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  registerSiteProjectSolar,
  uploadSiteProjectEquipmentFile,
  validateSiteProjectConfirmationItems,
  validateSiteProjectEquipment,
  validateSiteProjectFinalAgreement,
  validateSiteProjectSustainabilityCommitment,
} from './equipment';
import type {
  SiteProjectEquipmentValidationItem,
  SiteProjectSolarRegistrationRequest,
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

describe('registerSiteProjectSolar', () => {
  it('posts the aggregate registration payload to the solar endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ statusCode: '200', participationId: 10 }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);
    const payload: SiteProjectSolarRegistrationRequest = {
      siteId: 'SITE-1',
      projectId: 'PROJ0002',
      equipments: [
        {
          tempId: 'temp-1',
          methodologyNumber: 'EN-R-002',
          isEligible: true,
        equipmentOpDate: '2026-07-22',
        nominalMaxOutput: 10,
        pcsManufacturer: 'Maker',
        pcsModel: 'Model',
          powerCondUnitCount: 1,
        deviceNo: 'DEVICE-1',
          powerCondStandby: 1,
        hasBattery: false,
          ictEquipmentPowerConsumption: null,
          additionalEquipment: false,
        totalInvestment: 1000,
        hasSubsidy: false,
        preProjectUnitPrice: 20,
        files: [],
      },
      ],
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
      energyReport: '',
      energyBusinessCode: '',
      factoryCode: '',
      companyName: 'Company',
      repName: 'Representative',
      repRole: 'Director',
      hasConsent: true,
      isAgreeTerms: true,
    };

    await registerSiteProjectSolar('SITE-1', 'PROJ0002', payload);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/site/project/SITE-1/PROJ0002/solar/register',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    );
  });
});
