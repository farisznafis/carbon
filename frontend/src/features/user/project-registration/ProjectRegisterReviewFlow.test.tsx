// Verifies that the five-step flow submits only after the final review action.
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { SolarEquipment } from './components/types';
import ProjectRegisterPage from './ProjectRegisterPage';

const apiMocks = vi.hoisted(() => ({
  joinSiteProject: vi.fn(),
  uploadSiteProjectEquipmentFile: vi.fn(),
  validateSiteProjectEquipment: vi.fn(),
  validateSiteProjectConfirmationItems: vi.fn(),
  validateSiteProjectSustainabilityCommitment: vi.fn(),
  validateSiteProjectFinalAgreement: vi.fn(),
}));

const flowEquipment: SolarEquipment = {
  id: 'equipment-review',
  formType: 'solar',
  solarData: {
    siteId: 'SITE-SELECTED',
    equipmentOpDate: '2026-08-17',
    operationStartDateFile: new File(['contract'], 'contract.pdf'),
    nominalMaxOutput: '49.5',
    pcsManufacturer: 'Maker',
    pcsModel: 'Model',
    powerCondUnitCount: '1',
    deviceNo: 'DEVICE-1',
    powerCondStandby: '0.05',
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

vi.mock('@/api/site', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/site')>()),
  ...apiMocks,
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 'user-1', companyId: 'company-1', role: 'master' },
  }),
}));

vi.mock('@/hooks/use-toast', () => ({
  useToast: () => ({ toast: vi.fn() }),
}));

vi.mock('@/hooks/sites/useSitesQuery', () => ({
  useSitesQuery: () => ({
    data: {
      sites: [{ siteId: 'SITE-SELECTED', siteName: '東京工場' }],
    },
  }),
}));

vi.mock('@/stores/dashboardStore', () => ({
  useDashboardStore: (
    selector: (state: {
      currentProject: Record<string, unknown>;
      setCurrentProject: ReturnType<typeof vi.fn>;
    }) => unknown,
  ) =>
    selector({
      currentProject: {
        id: 'PROJ-1',
        projectName: '太陽光発電プロジェクト',
        projectStatus: 'APPROVED',
        methodologyVersion: 'Ver.2.0',
        methodCategory: '',
        methodNumber: '003',
        methodName: '方法論',
        methodMainEmission: 'CO2',
        methodCombiFeasibility: false,
      },
      setCurrentProject: vi.fn(),
    }),
}));

vi.mock('./components/EquipmentRegistrationStep', async () => {
  const { forwardRef, useImperativeHandle } = await import('react');
  return {
    EquipmentRegistrationStep: forwardRef((_props, ref) => {
      useImperativeHandle(ref, () => ({
        validate: async () => flowEquipment,
      }));
      return <h2>設備登録フォーム</h2>;
    }),
  };
});

vi.mock('./components/DynamicAgreementsStep', async () => {
  const { forwardRef, useImperativeHandle } = await import('react');
  return {
    DynamicAgreementsStep: forwardRef((_props, ref) => {
      useImperativeHandle(ref, () => ({
        getValues: () => ({
          useRegisteredInfo: true,
          provideAdditionalInfo: true,
          transferEnvironmentalValue: true,
          notRegisteredElsewhere: true,
          keidanrenParticipation: 'notParticipating',
          specifiedEmitterStatus: 'notSpecifiedEmitter',
          energyRationalizationCompliance: 'notApplicable',
        }),
      }));
      return <h2>同意事項フォーム</h2>;
    }),
  };
});

vi.mock('./components/ContractStep', async () => {
  const { forwardRef, useImperativeHandle } = await import('react');
  return {
    ContractStep: forwardRef((_props, ref) => {
      useImperativeHandle(ref, () => ({
        getValues: () => ({
          businessName: 'ABC株式会社',
          representativeName: '山田太郎',
          representativePosition: '代表取締役',
          contractAgreed: true,
        }),
      }));
      return <h2>持続可能性フォーム</h2>;
    }),
  };
});

vi.mock('./components/FinalTermsStep', async () => {
  const { forwardRef, useImperativeHandle } = await import('react');
  return {
    FinalTermsStep: forwardRef((_props, ref) => {
      useImperativeHandle(ref, () => ({
        getValues: () => ({ finalTermsAgreed: true }),
      }));
      return <h2>入会規約フォーム</h2>;
    }),
  };
});

// Renders the project registration route with deterministic step data.
function renderFlow() {
  return render(
    <MemoryRouter
      initialEntries={['/projects/SITE-ROUTE/PROJ-1/register']}
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
}

// Advances the mocked four input steps to the final review screen.
async function advanceToReview(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('button', { name: '次へ' }));
  await screen.findByText('同意事項フォーム');
  await user.click(screen.getByRole('button', { name: '次へ' }));
  await screen.findByText('持続可能性フォーム');
  await user.click(screen.getByRole('button', { name: '次へ' }));
  await screen.findByText('入会規約フォーム');
  await user.click(screen.getByRole('button', { name: '確認へ' }));
  await screen.findByRole('heading', { name: '申請内容の確認' });
}

describe('ProjectRegisterPage review flow', () => {
  beforeEach(() => {
    localStorage.clear();
    window.scrollTo = vi.fn();
    Object.values(apiMocks).forEach((apiMock) => apiMock.mockReset());
    apiMocks.validateSiteProjectEquipment.mockResolvedValue({ statusCode: 'OK' });
    apiMocks.validateSiteProjectConfirmationItems.mockResolvedValue({ statusCode: 'OK' });
    apiMocks.validateSiteProjectSustainabilityCommitment.mockResolvedValue({ statusCode: 'OK' });
    apiMocks.validateSiteProjectFinalAgreement.mockResolvedValue({ statusCode: 'OK' });
    apiMocks.uploadSiteProjectEquipmentFile.mockResolvedValue({
      fileType: 'solar_contract',
      fileId: 'file-1',
      fileName: 'contract.pdf',
      s3Key: 'tmp/contract.pdf',
    });
    apiMocks.joinSiteProject.mockResolvedValue({ statusCode: 'OK' });
  });

  it('reviews all steps before uploading and submitting', async () => {
    const user = userEvent.setup();
    renderFlow();

    expect(screen.getByRole('navigation', { name: 'Progress' })).toHaveTextContent(
      '設備登録同意事項持続可能性入会規約確認',
    );

    await advanceToReview(user);

    expect(
      await screen.findByRole('heading', { name: '申請内容の確認' }),
    ).toBeVisible();
    expect(screen.getByText('東京工場')).toBeVisible();
    expect(screen.getByText('contract.pdf')).toBeVisible();
    expect(apiMocks.uploadSiteProjectEquipmentFile).not.toHaveBeenCalled();
    expect(apiMocks.joinSiteProject).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: '戻る' }));
    expect(await screen.findByText('入会規約フォーム')).toBeVisible();
    await user.click(screen.getByRole('button', { name: '確認へ' }));
    await user.click(await screen.findByRole('button', { name: '申請する' }));

    await waitFor(() => {
      expect(apiMocks.uploadSiteProjectEquipmentFile).toHaveBeenCalledWith(
        'SITE-SELECTED',
        'solar_contract',
        expect.any(File),
      );
      expect(apiMocks.joinSiteProject).toHaveBeenCalledTimes(1);
    });
  });

  it('keeps the review and draft visible when final submission fails', async () => {
    const user = userEvent.setup();
    apiMocks.joinSiteProject.mockRejectedValueOnce(new Error('送信失敗'));
    renderFlow();
    await advanceToReview(user);

    await user.click(screen.getByRole('button', { name: '申請する' }));

    await waitFor(() => {
      expect(apiMocks.joinSiteProject).toHaveBeenCalledTimes(1);
      expect(screen.getByRole('heading', { name: '申請内容の確認' })).toBeVisible();
      expect(screen.getByRole('button', { name: '申請する' })).toBeEnabled();
      expect(screen.getByText('contract.pdf')).toBeVisible();
    });
  });
});
