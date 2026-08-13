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
  it('loads editable data, hides site selection, shows replacement files, and patches', async () => {
    const fetchMock = vi.fn().mockImplementation(
      (_url: string | URL | Request, options?: RequestInit) =>
        Promise.resolve(
          new Response(
            JSON.stringify(
              options?.method === 'PATCH'
                ? { ...equipmentResponse, nominalMaxOutput: 30 }
                : equipmentResponse,
            ),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          ),
        ),
    );
    const { container } = renderPage(fetchMock);

    expect(await screen.findByText('設備ID：7')).toBeInTheDocument();
    expect(screen.queryByText('対象拠点')).not.toBeInTheDocument();
    expect(
      screen.getByText('確認資料：太陽光発電電力受給契約確認書'),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'existing-pcs.pdfを表示' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: '変更内容を確認' }),
    ).toBeDisabled();

    const outputInput = container.querySelector<HTMLInputElement>(
      '[name="nominalMaxOutput"]',
    );
    expect(outputInput).not.toBeNull();
    fireEvent.change(outputInput!, { target: { value: '30' } });
    fireEvent.click(screen.getByRole('button', { name: '変更内容を確認' }));

    expect(await screen.findByText('設備変更の確認')).toBeInTheDocument();
    expect(screen.getByText('変更あり')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '変更する' }));

    expect(await screen.findByText('Equipment list returned')).toBeInTheDocument();
    const patchCall = fetchMock.mock.calls.find(
      ([, options]) => (options as RequestInit | undefined)?.method === 'PATCH',
    );
    expect(patchCall?.[0]).toBe('/api/site/SITE-1/equipment/7');
    expect(JSON.parse(String((patchCall?.[1] as RequestInit).body))).toEqual({
      nominalMaxOutput: 30,
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

  it('uploads a replacement document and includes merged file metadata in PATCH', async () => {
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

    await screen.findByText('設備ID：7');
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
    const patchCall = fetchMock.mock.calls.find(
      ([, options]) => (options as RequestInit | undefined)?.method === 'PATCH',
    );
    const patchBody = JSON.parse(String((patchCall?.[1] as RequestInit).body));
    expect(patchBody).toEqual({
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

  it('returns backend field errors to the editable field', async () => {
    const fetchMock = vi.fn().mockImplementation(
      (_url: string | URL | Request, options?: RequestInit) =>
        Promise.resolve(
          options?.method === 'PATCH'
            ? new Response(
                JSON.stringify({
                  statusCode: 'VALIDATION_ERROR',
                  message: '入力内容に誤りがあります。',
                  errors: { pcsUnitCount: '導入台数を確認してください。' },
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

    await screen.findByText('設備ID：7');
    const countInput = container.querySelector<HTMLInputElement>(
      '[name="powerCondUnitCount"]',
    );
    fireEvent.change(countInput!, { target: { value: '3' } });
    fireEvent.click(screen.getByRole('button', { name: '変更内容を確認' }));
    fireEvent.click(await screen.findByRole('button', { name: '変更する' }));

    expect(
      await screen.findByText('導入台数を確認してください。'),
    ).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText('設備変更')).toBeInTheDocument();
    });
  });
});
