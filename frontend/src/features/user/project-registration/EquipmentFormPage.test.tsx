import { fireEvent, render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import EquipmentFormPage from './EquipmentFormPage';

function renderEquipmentForm() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter
        initialEntries={[
          '/projects/SITE4f83ac050b20d755/PROJ0002/equipment?type=solar',
        ]}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Routes>
          <Route
            path="/projects/:siteId/:projectId/equipment"
            element={<EquipmentFormPage />}
          />
          <Route
            path="/projects/:siteId/:projectId/equipment/confirmation"
            element={<div>Equipment confirmation</div>}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('EquipmentFormPage file handling', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('user', JSON.stringify({ companyId: 'PC00001' }));
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((url: string | URL | Request) => {
        const requestUrl = String(url);
        let responseBody: Record<string, unknown> = { statusCode: '200' };
        if (requestUrl.startsWith('/api/site?companyId=')) {
          responseBody = {
              statusCode: '200',
              siteList: [
                {
                  siteId: 'SITE4f83ac050b20d755',
                  siteName: '東京拠点',
                },
                { siteId: 'SITE-2', siteName: '大阪拠点' },
              ],
            };
        } else if (requestUrl.includes('/files/upload')) {
          const fileType = new URL(
            requestUrl,
            'http://localhost',
          ).searchParams.get('fileType');
          responseBody = {
            statusCode: '200',
            s3Key: `temp/${fileType}`,
            fileId: `file-${fileType}`,
            originalName: `${fileType}.pdf`,
          };
        }

        return Promise.resolve(
          new Response(JSON.stringify(responseBody), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          }),
        );
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('validates selected file metadata without uploading the files', async () => {
    const { container } = renderEquipmentForm();
    expect((await screen.findAllByText('東京拠点')).length).toBeGreaterThan(0);
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
      await screen.findByText('Equipment confirmation'),
    ).toBeInTheDocument();
    expect(fetch).toHaveBeenCalledWith(
      '/api/site/project/equipment/validate',
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('"siteId":"SITE4f83ac050b20d755"'),
      }),
    );
    const validationRequest = vi.mocked(fetch).mock.calls.find(([url]) =>
      String(url).includes('/site/project/equipment/validate'),
    );
    const validationBody = JSON.parse(
      String((validationRequest?.[1] as RequestInit | undefined)?.body),
    ) as { files: Array<{ fileName: string }> };
    expect(validationBody.files).toHaveLength(requiredFileInputIds.length);
    const uploadCalls = vi.mocked(fetch).mock.calls.filter(([url]) =>
      String(url).includes('/files/upload'),
    );
    expect(uploadCalls).toHaveLength(0);
  });

  it('shows a warning for every missing required file', async () => {
    renderEquipmentForm();

    fireEvent.click(screen.getByRole('button', { name: '確認' }));

    const warnings = await screen.findAllByText(
      '確認資料をアップロードしてください。',
    );
    expect(warnings.length).toBeGreaterThanOrEqual(5);
    expect(
      vi.mocked(fetch).mock.calls.some(([url]) =>
        String(url).includes('/site/project/equipment/validate'),
      ),
    ).toBe(false);
    expect(screen.queryByText('Equipment confirmation')).not.toBeInTheDocument();
  });
});
