import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import EquipmentFormPage from './EquipmentFormPage';

function renderEquipmentForm() {
  return render(
    <MemoryRouter
      initialEntries={['/projects/SITE4f83ac050b20d755/2/equipment?type=solar']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <Routes>
        <Route
          path="/projects/:siteId/:projectId/equipment"
          element={<EquipmentFormPage />}
        />
        <Route
          path="/projects/:siteId/:projectId/register"
          element={<div>Project registration</div>}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('EquipmentFormPage file handling', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ statusCode: '200' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('validates without uploading a selected file', async () => {
    const { container } = renderEquipmentForm();
    const requiredFileInputIds = [
      'equipment-op-date-file',
      'nominal-max-output-file',
      'pcs-file',
      'total-investment-file',
      'pre-project-unit-price-file',
    ];

    requiredFileInputIds.forEach((inputId) => {
      const fileInput = container.querySelector<HTMLInputElement>(`#${inputId}`);
      expect(fileInput).not.toBeNull();
      fireEvent.change(fileInput!, {
        target: {
          files: [
            new File(['document'], `${inputId}.pdf`, {
              type: 'application/pdf',
            }),
          ],
        },
      });
    });
    fireEvent.click(screen.getByRole('button', { name: '確認' }));

    expect(
      await screen.findByText('Project registration'),
    ).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      '/api/site/project/equipment/validate',
      expect.objectContaining({ method: 'POST' }),
    );
    expect(
      vi.mocked(fetch).mock.calls.some(([url]) =>
        String(url).includes('/files/upload'),
      ),
    ).toBe(false);
  });

  it('shows a warning for every missing required file', async () => {
    renderEquipmentForm();

    fireEvent.click(screen.getByRole('button', { name: '確認' }));

    const warnings = await screen.findAllByText(
      '確認資料をアップロードしてください。',
    );
    expect(warnings.length).toBeGreaterThanOrEqual(5);
    expect(fetch).toHaveBeenCalledWith(
      '/api/site/project/equipment/validate',
      expect.objectContaining({ method: 'POST' }),
    );
    expect(screen.queryByText('Project registration')).not.toBeInTheDocument();
  });
});
