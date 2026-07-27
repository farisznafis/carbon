import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import type { SolarEquipment } from '@/features/user/project-registration/components/types';
import ProjectRegisterPage from './ProjectRegisterPage';

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
    additionalEquipment: 'no',
    ictEquipmentPowerConsumption: '',
    additionalEquipmentDate: '',
    additionalEquipmentType: '',
    additionalUnitCount: '',
    additionalDeviceUniqueNo: '',
    totalInvestment: '1000',
    hasSubsidy: 'no',
    subsidyName: '',
    subsidyAmount: '',
    preProjectUnitPrice: '20',
  },
  files: [],
};

describe('ProjectRegisterPage', () => {
  it('opens equipment registration as the first admission step', async () => {
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
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText('設備登録')).toBeInTheDocument();
    expect(screen.getByText('太陽光発電設備の情報')).toBeInTheDocument();
    expect(screen.queryByText('入力内容の確認')).not.toBeInTheDocument();
    expect(screen.getByText('ステップ 1 / 4')).toBeInTheDocument();
  });
});
