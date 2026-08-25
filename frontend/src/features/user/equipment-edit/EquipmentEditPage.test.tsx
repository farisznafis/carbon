import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import EquipmentEditPage from './EquipmentEditPage';

const equipmentResponse = {
  id: 7,
  siteId: 'SITE-1',
  equipmentOpDate: '2026-08-01',
  nominalMaxOutput: 25.5,
  pcsManufacturer: 'PCS Maker',
  pcsModel: 'PCS-1',
  pcsUnitCount: 2,
  deviceNo: 'DEVICE-1',
  pcsStandby: 1,
  hasBattery: false,
  totalInvestment: 100000,
  hasSubsidy: false,
  preProjectUnitPrice: 20,
  files: [
    {
      id: 20,
      fileType: 'pcs_spec',
      fileId: 'existing-pcs-id',
      s3Key: 'equipment/existing-pcs.pdf',
    },
  ],
};

function renderPage(fetchMock: ReturnType<typeof vi.fn>) {
  vi.stubGlobal('fetch', fetchMock);

  return render(
    <MemoryRouter
      initialEntries={['/sites/SITE-1/projects/PROJECT-1/equipment/7/edit']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <Routes>
        <Route
          path="/sites/:siteId/projects/:projectId/equipment/:equipmentId/edit"
          element={<EquipmentEditPage />}
        />
        <Route
          path="/sites/:siteId/projects/:projectId"
          element={<div>Equipment list returned</div>}
        />
      </Routes>
    </MemoryRouter>,
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('EquipmentEditPage', () => {
  it('loads editable data, hides site selection, shows replacement files, and posts a change', async () => {
    const fetchMock = vi.fn().mockImplementation(
      (_url: string | URL | Request, options?: RequestInit) =>
        Promise.resolve(
          new Response(
            JSON.stringify(
              options?.method === 'POST'
                ? { ...equipmentResponse, nominalMaxOutput: 30 }
                : equipmentResponse,
            ),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          ),
        ),
    );
    const { container } = renderPage(fetchMock);

    expect(
      await screen.findByRole('heading', { name: '設備変更' }),
    ).toBeInTheDocument();
    expect(screen.queryByText('対象拠点')).not.toBeInTheDocument();
    expect(
      screen.getByText('太陽光発電所・発電設備を特定できる資料'),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'existing-pcs.pdfを表示' }),
    ).toBeInTheDocument();
    const visibleFileInputs = Array.from(
      container.querySelectorAll<HTMLInputElement>('input[type="file"]'),
    );
    expect(visibleFileInputs).toHaveLength(5);
    visibleFileInputs.forEach((fileInput) => {
      expect(fileInput).toHaveAttribute('aria-required', 'true');
    });
    expect(
      screen.getByRole('button', { name: '変更内容を確認' }),
    ).toBeDisabled();
    const changeDate = container.querySelector<HTMLInputElement>(
      '[name="changeDate"]',
    )?.value;
    expect(changeDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);

    const outputInput = container.querySelector<HTMLInputElement>(
      '[name="nominalMaxOutput"]',
    );
    expect(outputInput).not.toBeNull();
    fireEvent.change(outputInput!, { target: { value: '30' } });
    fireEvent.click(screen.getByRole('button', { name: '変更内容を確認' }));

    expect(await screen.findByText('設備変更の確認')).toBeInTheDocument();
    expect(screen.getByText('変更あり')).toBeInTheDocument();
    const validationCall = fetchMock.mock.calls.find(([url]) =>
      String(url).endsWith('/site/project/equipment/validate'),
    );
    expect(validationCall?.[0]).toBe('/api/site/project/equipment/validate');
    expect(
      JSON.parse(String((validationCall?.[1] as RequestInit).body)),
    ).toEqual(
      expect.objectContaining({
        tempId: 'equipment-7',
        siteId: 'SITE-1',
        equipmentOpDate: changeDate,
        nominalMaxOutput: 30,
        files: [
          {
            fileType: 'pcs_spec',
            s3Key: 'equipment/existing-pcs.pdf',
            fileName: 'existing-pcs.pdf',
            fileId: 'existing-pcs-id',
          },
        ],
      }),
    );
    fireEvent.click(screen.getByRole('button', { name: '変更する' }));

    expect(await screen.findByText('Equipment list returned')).toBeInTheDocument();
    const changeCall = fetchMock.mock.calls.find(
      ([url, options]) =>
        String(url).endsWith('/equipment/7/change') &&
        (options as RequestInit | undefined)?.method === 'POST',
    );
    expect(changeCall?.[0]).toBe('/api/site/SITE-1/equipment/7/change');
    expect(JSON.parse(String((changeCall?.[1] as RequestInit).body))).toEqual({
      changeDate,
      nominalMaxOutput: 30,
      pcsManufacturer: 'PCS Maker',
      pcsModel: 'PCS-1',
      pcsUnitCount: 2,
      deviceNo: 'DEVICE-1',
      pcsStandby: 1,
      hasBattery: false,
      batteryMaker: null,
      batteryModel: null,
      batteryCapacity: null,
      totalInvestment: 100000,
      hasSubsidy: false,
      subsidyName: null,
      subsidyAmount: null,
      preProjectUnitPrice: 20,
      files: [
        {
          fileType: 'pcs_spec',
          s3Key: 'equipment/existing-pcs.pdf',
          fileName: 'existing-pcs.pdf',
          fileId: 'existing-pcs-id',
        },
      ],
    });
  });

  it('uploads a replacement document and includes merged file metadata in the change request', async () => {
    const fetchMock = vi.fn().mockImplementation(
      (url: string | URL | Request, options?: RequestInit) => {
        if (String(url).includes('/files/upload')) {
          return Promise.resolve(
            new Response(
              JSON.stringify({
                statusCode: 'OK',
                s3Key: 'equipment/new-contract.pdf',
                fileId: 'new-contract-id',
                originalName: 'new-contract.pdf',
              }),
              { status: 200, headers: { 'Content-Type': 'application/json' } },
            ),
          );
        }

        return Promise.resolve(
          new Response(JSON.stringify(equipmentResponse), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          }),
        );
      },
    );
    const { container } = renderPage(fetchMock);

    await screen.findByRole('heading', { name: '設備変更' });
    const changeDate = container.querySelector<HTMLInputElement>(
      '[name="changeDate"]',
    )?.value;
    const fileInput = container.querySelector<HTMLInputElement>(
      '#equipment-op-date-file',
    );
    expect(fileInput).not.toBeNull();
    fireEvent.change(fileInput!, {
      target: {
        files: [
          new File(['contract'], 'new-contract.pdf', {
            type: 'application/pdf',
          }),
        ],
      },
    });

    const reviewButton = screen.getByRole('button', {
      name: '変更内容を確認',
    });
    await waitFor(() => expect(reviewButton).toBeEnabled());
    fireEvent.click(reviewButton);
    expect(await screen.findByText('設備変更の確認')).toBeInTheDocument();
    expect(screen.getByText('new-contract.pdf')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '変更する' }));

    expect(await screen.findByText('Equipment list returned')).toBeInTheDocument();
    const uploadCall = fetchMock.mock.calls.find(([url]) =>
      String(url).includes('/files/upload'),
    );
    expect(uploadCall?.[0]).toBe(
      '/api/site/project/sites/SITE-1/files/upload?fileType=solar_contract',
    );
    const changeCall = fetchMock.mock.calls.find(
      ([url, options]) =>
        String(url).endsWith('/equipment/7/change') &&
        (options as RequestInit | undefined)?.method === 'POST',
    );
    const changeBody = JSON.parse(String((changeCall?.[1] as RequestInit).body));
    expect(changeBody).toEqual({
      changeDate,
      nominalMaxOutput: 25.5,
      pcsManufacturer: 'PCS Maker',
      pcsModel: 'PCS-1',
      pcsUnitCount: 2,
      deviceNo: 'DEVICE-1',
      pcsStandby: 1,
      hasBattery: false,
      batteryMaker: null,
      batteryModel: null,
      batteryCapacity: null,
      totalInvestment: 100000,
      hasSubsidy: false,
      subsidyName: null,
      subsidyAmount: null,
      preProjectUnitPrice: 20,
      files: [
        {
          fileType: 'pcs_spec',
          s3Key: 'equipment/existing-pcs.pdf',
          fileName: 'existing-pcs.pdf',
          fileId: 'existing-pcs-id',
        },
        {
          fileType: 'solar_contract',
          s3Key: 'equipment/new-contract.pdf',
          fileName: 'new-contract.pdf',
          fileId: 'new-contract-id',
        },
      ],
    });
  });

  it('requires a change date before opening the confirmation page', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(equipmentResponse), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    const { container } = renderPage(fetchMock);

    await screen.findByRole('heading', { name: '設備変更' });
    fireEvent.change(
      container.querySelector<HTMLInputElement>('[name="nominalMaxOutput"]')!,
      { target: { value: '30' } },
    );
    fireEvent.change(
      container.querySelector<HTMLInputElement>('[name="changeDate"]')!,
      { target: { value: '' } },
    );
    fireEvent.click(screen.getByRole('button', { name: '変更内容を確認' }));

    expect(
      await screen.findByText('変更日を入力してください。'),
    ).toBeInTheDocument();
    expect(screen.queryByText('設備変更の確認')).not.toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(
        ([, options]) => (options as RequestInit | undefined)?.method === 'POST',
      ),
    ).toBe(false);
  });

  it('returns validation errors to the editable field before confirmation', async () => {
    const fetchMock = vi.fn().mockImplementation(
      (_url: string | URL | Request, options?: RequestInit) =>
        Promise.resolve(
          options?.method === 'POST'
            ? new Response(
                JSON.stringify({
                  statusCode: 'VALIDATION_ERROR',
                  message: '入力内容に誤りがあります。',
                   errors: {
                     'equipment-7::変更日': '変更日を確認してください。',
                     'equipment-7::pcsUnitCount': '導入台数を確認してください。',
                   },
                }),
                { status: 400, headers: { 'Content-Type': 'application/json' } },
              )
            : new Response(JSON.stringify(equipmentResponse), {
                status: 200,
                headers: { 'Content-Type': 'application/json' },
              }),
        ),
    );
    const { container } = renderPage(fetchMock);

    await screen.findByRole('heading', { name: '設備変更' });
    const countInput = container.querySelector<HTMLInputElement>(
      '[name="powerCondUnitCount"]',
    );
    fireEvent.change(countInput!, { target: { value: '3' } });
    fireEvent.click(screen.getByRole('button', { name: '変更内容を確認' }));

    expect(
      await screen.findByText('導入台数を確認してください。'),
    ).toBeInTheDocument();
    expect(screen.getByText('変更日を確認してください。')).toBeInTheDocument();
    expect(screen.queryByText('設備変更の確認')).not.toBeInTheDocument();
    expect(
      fetchMock.mock.calls.some(([url]) =>
        String(url).endsWith('/equipment/7/change'),
      ),
    ).toBe(false);
  });
});
