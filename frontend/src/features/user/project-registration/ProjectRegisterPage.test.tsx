import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/_shared/errors';
import type { SolarEquipment } from '@/features/user/project-registration/components/types';
import ProjectRegisterPage from './ProjectRegisterPage';

const { validateSiteProjectEquipment } = vi.hoisted(() => ({
  validateSiteProjectEquipment: vi.fn(),
}));
const setCurrentProject = vi.fn();
const currentProject = {
  id: 'PROJ0002',
  projectName: 'Solar Project',
};

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: { role: 'master' } }),
}));

vi.mock('@/hooks/use-toast', () => ({
  useToast: () => ({ toast: vi.fn() }),
}));

vi.mock('@/hooks/sites/useSitesQuery', () => ({
  useSitesQuery: () => ({
    data: {
      sites: [{ siteId: 'SITE-1', siteName: 'Tokyo Site' }],
    },
    isLoading: false,
    isError: false,
    error: null,
  }),
}));

vi.mock('@/api/site', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/site')>();
  return {
    ...actual,
    validateSiteProjectEquipment,
  };
});

vi.mock('@/stores/dashboardStore', () => ({
  useDashboardStore: (
    selector: (state: {
      currentProject: typeof currentProject;
      setCurrentProject: typeof setCurrentProject;
    }) => unknown,
  ) => selector({ currentProject, setCurrentProject }),
}));

const pendingEquipment: SolarEquipment = {
  id: 'equipment-1',
  formType: 'solar',
  solarData: {
    siteId: 'SITE-1',
    equipmentOpDate: '2026-07-22',
    nominalMaxOutput: '10',
    pcsManufacturer: 'Maker',
    pcsModel: 'Model',
    powerCondUnitCount: '1',
    deviceNo: 'DEVICE-1',
    powerCondStandby: '1',
    hasBattery: 'no',
    batteryMaker: '',
    batteryModel: '',
    batteryCapacity: '',
    totalInvestment: '1000',
    hasSubsidy: 'no',
    subsidyName: '',
    subsidyAmount: '',
    preProjectUnitPrice: '20',
  },
  files: [],
};

function SiteRegisterDestination() {
  const location = useLocation();
  const returnTarget = (
    location.state as {
      returnTarget?: { state?: { pendingEquipment?: SolarEquipment } };
    } | null
  )?.returnTarget;

  return (
    <div>
      拠点追加ページ
      <span>{returnTarget?.state?.pendingEquipment?.solarData.siteId}</span>
    </div>
  );
}

describe('ProjectRegisterPage', () => {
  beforeEach(() => {
    localStorage.clear();
    Element.prototype.hasPointerCapture = vi.fn();
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
    Element.prototype.scrollIntoView = vi.fn();
    validateSiteProjectEquipment.mockReset();
    validateSiteProjectEquipment.mockResolvedValue({ statusCode: 'OK' });
  });

  it('asks the user to select a site instead of selecting the route site', async () => {
    render(
      <MemoryRouter
        initialEntries={['/projects/SITE-1/PROJ0002/register']}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Routes>
          <Route
            path="/projects/:siteId/:projectId/register"
            element={<ProjectRegisterPage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    expect((await screen.findAllByRole('combobox'))[0]).toHaveTextContent(
      '拠点を選択してください',
    );
    expect(screen.queryByText('Tokyo Site')).not.toBeInTheDocument();
  });

  it('opens equipment registration as the first admission step', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/projects/SITE-1/PROJ0002/register',
            state: { pendingEquipment },
          },
        ]}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Routes>
          <Route
            path="/projects/:siteId/:projectId/register"
            element={<ProjectRegisterPage />}
          />
          <Route
            path="/master/site-register"
            element={<SiteRegisterDestination />}
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole('heading', { name: '設備登録' }),
    ).toBeInTheDocument();
    expect(screen.getByText('太陽光発電設備の情報')).toBeInTheDocument();
    expect(screen.queryByText('入力内容の確認')).not.toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Progress' })).toHaveTextContent(
      '設備登録同意事項持続可能性入会規約確認',
    );
    expect(screen.queryByText('ステップ 1 / 5')).not.toBeInTheDocument();
    expect(screen.getByText('対象拠点').parentElement).toHaveTextContent('*');
    expect(
      screen.getByText('プロジェクトへの参加を申請する拠点を選択してください。'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('確認資料：太陽光発電電力受給契約確認書')
        .parentElement,
    ).toHaveTextContent('*');

    await user.click(screen.getAllByRole('combobox')[0]);
    await user.click(screen.getByRole('option', { name: '拠点を追加' }));

    expect(screen.getByText('拠点追加ページ')).toBeInTheDocument();
    expect(screen.getByText('SITE-1')).toBeInTheDocument();
  });

  it('keeps the first selected file when another file input is opened', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/projects/SITE-1/PROJ0002/register',
            state: { pendingEquipment },
          },
        ]}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Routes>
          <Route
            path="/projects/:siteId/:projectId/register"
            element={<ProjectRegisterPage />}
          />
        </Routes>
      </MemoryRouter>,
    );
    const fileInputs = container.querySelectorAll<HTMLInputElement>(
      'input[type="file"]',
    );
    const firstFile = new File(['contract'], 'contract.pdf', {
      type: 'application/pdf',
    });
    const secondFile = new File(['catalog'], 'catalog.pdf', {
      type: 'application/pdf',
    });

    await user.upload(fileInputs[0], firstFile);
    window.dispatchEvent(new Event('focus'));
    await user.upload(fileInputs[1], secondFile);

    expect(screen.getByText('contract.pdf')).toBeInTheDocument();
    expect(screen.getByText('catalog.pdf')).toBeInTheDocument();
  });

  it('calls backend validation when required fields and files are empty', async () => {
    const user = userEvent.setup();
    validateSiteProjectEquipment.mockRejectedValue(
      new ApiError('入力内容に誤りがあります。', {
        status: 400,
        payload: {
          statusCode: 'VALIDATION_ERROR',
          message: '入力内容に誤りがあります。',
          errors: {
            equipmentOpDate: '稼働開始日を入力してください。',
            nominalMaxOutput: '公称最大出力を入力してください。',
            pcsManufacturer: 'メーカーを入力してください。',
            files: '確認資料をアップロードしてください。',
          },
        },
      }),
    );

    render(
      <MemoryRouter
        initialEntries={['/projects/SITE-1/PROJ0002/register']}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Routes>
          <Route
            path="/projects/:siteId/:projectId/register"
            element={<ProjectRegisterPage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    await user.click(await screen.findByRole('button', { name: '次へ' }));

    expect(validateSiteProjectEquipment).toHaveBeenCalledTimes(1);
    expect(validateSiteProjectEquipment).toHaveBeenCalledWith(
      expect.objectContaining({
        equipmentOpDate: null,
        nominalMaxOutput: null,
        pcsManufacturer: null,
        files: [],
      }),
    );
    expect(
      await screen.findByText('稼働開始日を入力してください。'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('公称最大出力を入力してください。'),
    ).toBeInTheDocument();
    expect(screen.getByText('メーカーを入力してください。')).toBeInTheDocument();
    expect(
      screen.getAllByText('確認資料をアップロードしてください。'),
    ).toHaveLength(5);
  });

  it('shows the warning for the only missing required file after API validation', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <MemoryRouter
        initialEntries={['/projects/SITE-1/PROJ0002/register']}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Routes>
          <Route
            path="/projects/:siteId/:projectId/register"
            element={<ProjectRegisterPage />}
          />
        </Routes>
      </MemoryRouter>,
    );
    const fileInputs = container.querySelectorAll<HTMLInputElement>(
      'input[type="file"]',
    );

    for (const index of [0, 1, 2, 4]) {
      await user.upload(
        fileInputs[index],
        new File([`file-${index}`], `file-${index}.pdf`, {
          type: 'application/pdf',
        }),
      );
    }

    await user.click(await screen.findByRole('button', { name: '次へ' }));

    expect(validateSiteProjectEquipment).toHaveBeenCalledTimes(1);
    expect(
      await screen.findAllByText('確認資料をアップロードしてください。'),
    ).toHaveLength(1);
  });
});
