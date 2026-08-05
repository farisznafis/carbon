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
  it('loads editable data, hides registration-only fields, confirms, and patches', async () => {
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
    expect(screen.queryByText(/確認資料：/)).not.toBeInTheDocument();
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
