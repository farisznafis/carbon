import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { SolarPowerFormData } from '@/lib/equipmentValidations';
import EquipmentConfirmationPage from './EquipmentConfirmationPage';

const formData: SolarPowerFormData = {
  siteId: 'SITE-1',
  equipmentOpDate: '2026-07-22',
  operationStartDateFile: new File(['contract'], 'contract.pdf'),
  nominalMaxOutput: '10',
  nominalMaxOutputFile: new File(['panel'], 'panel.pdf'),
  pcsManufacturer: 'Maker',
  pcsModel: 'Model',
  powerCondUnitCount: '1',
  deviceNo: 'DEVICE-1',
  powerCondStandby: '1',
  pcsFile: new File(['pcs'], 'pcs.pdf'),
  hasBattery: 'no',
  batteryMaker: '',
  batteryModel: '',
  batteryCapacity: '',
  additionalEquipment: 'no',
  ictEquipmentPowerConsumption: '',
  additionalEquipmentDate: '',
  additionalEquipmentType: '',
  additionalUnitCount: '',
  additionalDeviceUniqueNo: '',
  totalInvestment: '1000',
  totalInvestmentFile: new File(['invoice'], 'invoice.pdf'),
  hasSubsidy: 'no',
  subsidyName: '',
  subsidyAmount: '',
  preProjectUnitPrice: '20',
  preProjectUnitPriceFile: new File(['bill'], 'bill.pdf'),
};

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/projects/SITE-1/PROJ0002/equipment/confirmation',
            state: {
              equipmentId: 'equipment-1',
              isEdit: false,
              formData,
              fromMasterDashboard: false,
            },
          },
        ]}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Routes>
          <Route
            path="/projects/:siteId/:projectId/equipment/confirmation"
            element={<EquipmentConfirmationPage />}
          />
          <Route
            path="/projects/:siteId/:projectId/register"
            element={<div>Project registration</div>}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('EquipmentConfirmationPage', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('user', JSON.stringify({ companyId: 'COMPANY-1' }));
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string | URL | Request) => {
        const requestUrl = String(url);
        if (requestUrl.startsWith('/api/site?companyId=')) {
          return Promise.resolve(
            new Response(
              JSON.stringify({
                statusCode: '200',
                siteList: [{ siteId: 'SITE-1', siteName: '東京拠点' }],
              }),
              { status: 200, headers: { 'Content-Type': 'application/json' } },
            ),
          );
        }

        const fileType = new URL(requestUrl, 'http://localhost').searchParams.get(
          'fileType',
        );
        return Promise.resolve(
          new Response(
            JSON.stringify({
              statusCode: '200',
              s3Key: `equipment/${fileType}.pdf`,
              fileId: `file-${fileType}`,
              originalName: `${fileType}.pdf`,
            }),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          ),
        );
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('uploads files, stores their metadata, and continues registration', async () => {
    renderPage();

    expect(await screen.findByText('設備登録の確認')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '登録' }));

    expect(await screen.findByText('Project registration')).toBeInTheDocument();
    const uploadCalls = vi.mocked(fetch).mock.calls.filter(([url]) =>
      String(url).includes('/files/upload'),
    );
    expect(uploadCalls).toHaveLength(5);

    const stored = JSON.parse(
      localStorage.getItem('project_PROJ0002_equipment') ?? '[]',
    ) as Array<{ files: unknown[]; solarData: Record<string, unknown> }>;
    expect(stored[0]?.files).toHaveLength(5);
    expect(stored[0]?.files[0]).toEqual(
      expect.objectContaining({
        fileType: 'solar_contract',
        s3Key: 'equipment/solar_contract.pdf',
        fileId: 'file-solar_contract',
      }),
    );
    expect(stored[0]?.solarData.operationStartDateFile).toBeUndefined();
  });
});
