// Verifies that the five-step flow submits only after the final review action.
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/_shared/errors';
import type { SolarEquipment } from './components/types';
import type { ContractStepFormValues } from './components/ContractStep';
import ProjectRegisterPage from './ProjectRegisterPage';

const apiMocks = vi.hoisted(() => ({
  joinSiteProject: vi.fn(),
  uploadSiteProjectEquipmentFile: vi.fn(),
  validateSiteProjectEquipment: vi.fn(),
  validateSiteProjectConfirmationItems: vi.fn(),
  validateSiteProjectSustainabilityCommitment: vi.fn(),
  validateSiteProjectFinalAgreement: vi.fn(),
}));

const projectApiMocks = vi.hoisted(() => ({
  getProjectMemberTermsInfo: vi.fn(),
}));

const draftStorageMocks = vi.hoisted(() => ({
  loadProjectRegistrationDraft: vi.fn(),
  saveProjectRegistrationDraft: vi.fn(),
  deleteProjectRegistrationDraft: vi.fn(),
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
    // powerCondUnitCount: '1',
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

vi.mock('@/api/projects', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/projects')>()),
  ...projectApiMocks,
}));

vi.mock(
  '@/features/user/project-registration/projectRegistrationDraftStorage',
  async (importOriginal) => ({
    ...(await importOriginal<
      typeof import('@/features/user/project-registration/projectRegistrationDraftStorage')
    >()),
    ...draftStorageMocks,
  }),
);

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 'user-1', companyId: 'company-1', role: 'master' },
  }),
  useOptionalAuth: () => ({
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
    EquipmentRegistrationStep: forwardRef<
      unknown,
      { onBack: () => void; onNext: () => void }
    >((props, ref) => {
      useImperativeHandle(ref, () => ({
        validate: async () => flowEquipment,
      }));
      return (
        <div>
          <h2>設備登録フォーム</h2>
          <button type="button" onClick={props.onBack}>戻る</button>
          <button type="button" onClick={props.onNext}>次へ</button>
        </div>
      );
    }),
  };
});

vi.mock('./components/DynamicAgreementsStep', async () => {
  const { forwardRef, useImperativeHandle } = await import('react');
  return {
    DynamicAgreementsStep: forwardRef<
      unknown,
      {
        errors?: Record<string, string>;
        onBack: () => void;
        onNext: () => void;
      }
    >((props, ref) => {
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
      return (
        <div>
          <h2>同意事項フォーム</h2>
          <span data-testid="specified-emitter-code-error">
            {props.errors?.specifiedEmitterCode ?? ''}
          </span>
          <span data-testid="emission-addition-agreement-error">
            {props.errors?.emissionAdditionAgreement ?? ''}
          </span>
          <span data-testid="use-registered-info-error">
            {props.errors?.useRegisteredInfo ?? ''}
          </span>
          <span data-testid="provide-additional-info-error">
            {props.errors?.provideAdditionalInfo ?? ''}
          </span>
          <span data-testid="transfer-environmental-value-error">
            {props.errors?.transferEnvironmentalValue ?? ''}
          </span>
          <span data-testid="not-registered-elsewhere-error">
            {props.errors?.notRegisteredElsewhere ?? ''}
          </span>
          <span data-testid="keidanren-participation-error">
            {props.errors?.keidanrenParticipation ?? ''}
          </span>
          <span data-testid="energy-law-type-error">
            {props.errors?.energyLawType ?? ''}
          </span>
          <span data-testid="energy-law-business-number-error">
            {props.errors?.energyLawBusinessNumber ?? ''}
          </span>
          <span data-testid="energy-law-factory-number-error">
            {props.errors?.energyLawFactoryNumber ?? ''}
          </span>
          <button type="button" onClick={props.onBack}>戻る</button>
          <button type="button" onClick={props.onNext}>次へ</button>
        </div>
      );
    }),
  };
});

vi.mock('./components/ContractStep', async () => {
  const { forwardRef, useImperativeHandle } = await import('react');
  return {
    ContractStep: forwardRef<
      unknown,
      {
        operatorName: string;
        formValues: Partial<ContractStepFormValues>;
        onValueChange: (values: Partial<ContractStepFormValues>) => void;
        errors?: Record<string, string>;
        onBack: () => void;
        onNext: () => void;
      }
    >((props, ref) => {
      useImperativeHandle(ref, () => ({
        getValues: () => ({
          businessName: 'ABC株式会社',
          representativeName: '山田太郎',
          representativePosition: '代表取締役',
          contractAgreed: true,
        }),
      }));
      return (
        <div>
          <h2>持続可能性フォーム</h2>
          <span data-testid="contract-operator-name">{props.operatorName}</span>
          <span data-testid="contract-business-name">
            {props.formValues.businessName ?? ''}
          </span>
          <span data-testid="contract-representative-name">
            {props.formValues.representativeName ?? ''}
          </span>
          <span data-testid="contract-representative-position">
            {props.formValues.representativePosition ?? ''}
          </span>
          <span data-testid="contract-agreed">
            {String(props.formValues.contractAgreed === true)}
          </span>
          <span data-testid="contract-business-name-error">
            {props.errors?.businessName ?? ''}
          </span>
          <span data-testid="contract-representative-name-error">
            {props.errors?.representativeName ?? ''}
          </span>
          <span data-testid="contract-representative-position-error">
            {props.errors?.representativePosition ?? ''}
          </span>
          <span data-testid="contract-agreed-error">
            {props.errors?.contractAgreed ?? ''}
          </span>
          <button
            type="button"
            onClick={() =>
              props.onValueChange({
                businessName: '',
                representativeName: '',
                representativePosition: '',
                contractAgreed: false,
              })
            }
          >
            契約情報を空にする
          </button>
          <button type="button" onClick={props.onBack}>戻る</button>
          <button type="button" onClick={props.onNext}>次へ</button>
        </div>
      );
    }),
  };
});

vi.mock('./components/FinalTermsStep', async () => {
  const { forwardRef, useImperativeHandle } = await import('react');
  return {
    FinalTermsStep: forwardRef<
      unknown,
      {
        projectName: string;
        operatorName: string;
        projectOverview: string;
        usePurpose: string;
        startedAt: string;
        errors?: Record<string, string>;
        onBack: () => void;
        onNext: () => void;
      }
    >((props, ref) => {
      useImperativeHandle(ref, () => ({
        getValues: () => ({ finalTermsAgreed: true }),
      }));
      return (
        <div>
          <h2>入会規約フォーム</h2>
          <span data-testid="terms-project-name">{props.projectName}</span>
          <span data-testid="terms-operator-name">{props.operatorName}</span>
          <span data-testid="terms-project-overview">{props.projectOverview}</span>
          <span data-testid="terms-use-purpose">{props.usePurpose}</span>
          <span data-testid="terms-started-at">{props.startedAt}</span>
          <span data-testid="final-terms-agreed-error">
            {props.errors?.finalTermsAgreed ?? ''}
          </span>
          <button type="button" onClick={props.onBack}>戻る</button>
          <button type="button" onClick={props.onNext}>確認へ</button>
        </div>
      );
    }),
  };
});

// Renders the project registration route with deterministic step data.
function renderFlow(
  initialEntry:
    | string
    | {
        pathname: string;
        state?: Record<string, unknown>;
      } = '/projects/SITE-ROUTE/PROJ-1/register',
) {
  return render(
    <MemoryRouter
      initialEntries={[initialEntry]}
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
async function advanceToReview(
  user: ReturnType<typeof userEvent.setup>,
  onFinalTermsVisible?: () => void,
) {
  await user.click(await screen.findByRole('button', { name: '次へ' }));
  await screen.findByText('同意事項フォーム');
  await user.click(screen.getByRole('button', { name: '次へ' }));
  await screen.findByText('持続可能性フォーム');
  await user.click(screen.getByRole('button', { name: '次へ' }));
  await screen.findByText('入会規約フォーム');
  onFinalTermsVisible?.();
  await user.click(screen.getByRole('button', { name: '確認へ' }));
  await screen.findByRole('heading', { name: '申請内容の確認' });
}

describe('ProjectRegisterPage review flow', () => {
  beforeEach(() => {
    localStorage.clear();
    window.scrollTo = vi.fn();
    Object.values(apiMocks).forEach((apiMock) => apiMock.mockReset());
    projectApiMocks.getProjectMemberTermsInfo.mockReset();
    projectApiMocks.getProjectMemberTermsInfo.mockResolvedValue({
      statusCode: 'OK',
      message: '',
      projectName: '動的プロジェクト名',
      operatorNameJa: '株式会社ABC',
      adminRepName: '山田太郎',
      projectOverview: '動的プロジェクト概要',
      usePurpose: '地域脱炭素施策',
      startedAt: '2026-09-03T01:05:12.040Z',
    });
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
    draftStorageMocks.loadProjectRegistrationDraft.mockReset();
    draftStorageMocks.saveProjectRegistrationDraft.mockReset();
    draftStorageMocks.deleteProjectRegistrationDraft.mockReset();
    draftStorageMocks.loadProjectRegistrationDraft.mockResolvedValue(null);
    draftStorageMocks.saveProjectRegistrationDraft.mockResolvedValue(undefined);
    draftStorageMocks.deleteProjectRegistrationDraft.mockResolvedValue(undefined);
  });

  it('reviews all steps before uploading and submitting', async () => {
    const user = userEvent.setup();
    renderFlow();

    expect(await screen.findByRole('navigation', { name: 'Progress' })).toHaveTextContent(
      '設備登録同意事項持続可能性入会規約確認',
    );

    await advanceToReview(user, () => {
      expect(projectApiMocks.getProjectMemberTermsInfo).toHaveBeenCalledTimes(1);
      expect(projectApiMocks.getProjectMemberTermsInfo).toHaveBeenCalledWith(
        'PROJ-1',
      );
      expect(screen.getByTestId('terms-project-name')).toHaveTextContent(
        '動的プロジェクト名',
      );
      expect(screen.getByTestId('terms-operator-name')).toHaveTextContent(
        '株式会社ABC・山田太郎',
      );
      expect(screen.getByTestId('terms-project-overview')).toHaveTextContent(
        '動的プロジェクト概要',
      );
      expect(screen.getByTestId('terms-use-purpose')).toHaveTextContent(
        '地域脱炭素施策',
      );
      expect(screen.getByTestId('terms-started-at')).toHaveTextContent(
        '2026-09-03T01:05:12.040Z',
      );
    });

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
      expect(draftStorageMocks.deleteProjectRegistrationDraft).toHaveBeenCalledTimes(1);
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

  it('shows both Japanese backend errors for specified-emitter fields', async () => {
    const user = userEvent.setup();
    apiMocks.validateSiteProjectConfirmationItems.mockRejectedValueOnce(
      new ApiError('入力内容に不備があります。', {
        status: 400,
        payload: {
          statusCode: 'VALIDATION_ERROR',
          message: '入力内容に不備があります。',
          errors: {
            'request::調整後排出量への加算': '同意が必要です。',
            'request::事業者コード（9桁）': '9桁の数字を入力してください。',
          },
        },
      }),
    );
    renderFlow();

    await user.click(await screen.findByRole('button', { name: '次へ' }));
    await screen.findByText('同意事項フォーム');
    await user.click(screen.getByRole('button', { name: '次へ' }));

    expect(
      await screen.findByTestId('specified-emitter-code-error'),
    ).toHaveTextContent('9桁の数字を入力してください。');
    expect(
      screen.getByTestId('emission-addition-agreement-error'),
    ).toHaveTextContent('同意が必要です。');
    expect(screen.getByText('同意事項フォーム')).toBeVisible();
  });

  it('maps every returned empty-form error to its agreement field', async () => {
    const user = userEvent.setup();
    apiMocks.validateSiteProjectConfirmationItems.mockRejectedValueOnce(
      new ApiError('入力内容に不備があります。', {
        status: 400,
        payload: {
          statusCode: 'VALIDATION_ERROR',
          message: '入力内容に不備があります。',
          errors: {
            'request::データ利用への同意':
              'データ利用への同意が必要です。',
            'request::本入会届に記載された以外の情報の提供への同意':
              '本入会届に記載された以外の情報の提供への同意が必要です。',
            'request::経団連カーボンニュートラル行動計画への参加状況':
              '選択してください。',
            'request::環境価値（J-クレジット）の譲渡への同意':
              '環境価値（J-クレジット）の譲渡への同意が必要です。',
            'request::他のJ-クレジット制度への未登録確認':
              '他のJ-クレジット制度への未登録確認が必要です。',
          },
        },
      }),
    );
    renderFlow();

    await user.click(await screen.findByRole('button', { name: '次へ' }));
    await screen.findByText('同意事項フォーム');
    await user.click(screen.getByRole('button', { name: '次へ' }));

    expect(await screen.findByTestId('use-registered-info-error')).toHaveTextContent(
      'データ利用への同意が必要です。',
    );
    expect(screen.getByTestId('provide-additional-info-error')).toHaveTextContent(
      '本入会届に記載された以外の情報の提供への同意が必要です。',
    );
    expect(screen.getByTestId('keidanren-participation-error')).toHaveTextContent(
      '選択してください。',
    );
    expect(
      screen.getByTestId('transfer-environmental-value-error'),
    ).toHaveTextContent(
      '環境価値（J-クレジット）の譲渡への同意が必要です。',
    );
    expect(screen.getByTestId('not-registered-elsewhere-error')).toHaveTextContent(
      '他のJ-クレジット制度への未登録確認が必要です。',
    );
  });

  it('maps every energy-law backend error to its conditional field', async () => {
    const user = userEvent.setup();
    apiMocks.validateSiteProjectConfirmationItems.mockRejectedValueOnce(
      new ApiError('入力内容に不備があります。', {
        status: 400,
        payload: {
          statusCode: 'VALIDATION_ERROR',
          message: '入力内容に不備があります。',
          errors: {
            'request::事業者番号': '7桁の数字を入力してください。',
            'request::工場等番号': '7桁の数字を入力してください。',
            'request::第一種・第二種区分': '選択してください。',
          },
        },
      }),
    );
    renderFlow();

    await user.click(await screen.findByRole('button', { name: '次へ' }));
    await screen.findByText('同意事項フォーム');
    await user.click(screen.getByRole('button', { name: '次へ' }));

    expect(await screen.findByTestId('energy-law-type-error')).toHaveTextContent(
      '選択してください。',
    );
    expect(
      screen.getByTestId('energy-law-business-number-error'),
    ).toHaveTextContent('7桁の数字を入力してください。');
    expect(
      screen.getByTestId('energy-law-factory-number-error'),
    ).toHaveTextContent('7桁の数字を入力してください。');
  });

  it('maps every Japanese sustainability error to its contract field', async () => {
    const user = userEvent.setup();
    apiMocks.validateSiteProjectSustainabilityCommitment.mockRejectedValueOnce(
      new ApiError('入力内容に不備があります。', {
        status: 400,
        payload: {
          statusCode: 'VALIDATION_ERROR',
          message: '入力内容に不備があります。',
          errors: {
            'request::代表者役職': '必須項目です。',
            'request::事業者名': '必須項目です。',
            'request::代表者名': '必須項目です。',
            'request::同意する': '同意してください。',
          },
        },
      }),
    );
    renderFlow();

    await user.click(await screen.findByRole('button', { name: '次へ' }));
    await screen.findByText('同意事項フォーム');
    await user.click(screen.getByRole('button', { name: '次へ' }));
    await screen.findByText('持続可能性フォーム');
    await user.click(screen.getByRole('button', { name: '次へ' }));

    expect(
      await screen.findByTestId('contract-business-name-error'),
    ).toHaveTextContent('必須項目です。');
    expect(
      screen.getByTestId('contract-representative-name-error'),
    ).toHaveTextContent('必須項目です。');
    expect(
      screen.getByTestId('contract-representative-position-error'),
    ).toHaveTextContent('必須項目です。');
    expect(screen.getByTestId('contract-agreed-error')).toHaveTextContent(
      '同意してください。',
    );
    expect(screen.getByText('持続可能性フォーム')).toBeVisible();
  });

  it('maps the Japanese final-agreement error to the agreement checkbox', async () => {
    const user = userEvent.setup();
    apiMocks.validateSiteProjectFinalAgreement.mockRejectedValueOnce(
      new ApiError('入力内容に不備があります。', {
        status: 400,
        payload: {
          statusCode: 'VALIDATION_ERROR',
          message: '入力内容に不備があります。',
          errors: {
            'request::同意します': '同意してください。',
          },
        },
      }),
    );
    renderFlow();

    await user.click(await screen.findByRole('button', { name: '次へ' }));
    await screen.findByText('同意事項フォーム');
    await user.click(screen.getByRole('button', { name: '次へ' }));
    await screen.findByText('持続可能性フォーム');
    await user.click(screen.getByRole('button', { name: '次へ' }));
    await screen.findByText('入会規約フォーム');
    await user.click(screen.getByRole('button', { name: '確認へ' }));

    expect(
      await screen.findByTestId('final-terms-agreed-error'),
    ).toHaveTextContent('同意してください。');
    expect(screen.getByText('入会規約フォーム')).toBeVisible();
  });

  it('restores the saved step, equipment values, and selected files after refresh', async () => {
    draftStorageMocks.loadProjectRegistrationDraft.mockResolvedValueOnce({
      version: 1,
      currentStep: 5,
      equipment: [flowEquipment],
      agreementFormValues: {},
      contractFormValues: {},
      finalTermsValues: {},
      updatedAt: '2026-08-17T17:00:00.000Z',
    });

    renderFlow();

    expect(
      await screen.findByRole('heading', { name: '申請内容の確認' }),
    ).toBeVisible();
    expect(screen.getByText('東京工場')).toBeVisible();
    expect(screen.getByText('2026-08-17')).toBeVisible();
    expect(screen.getByText('contract.pdf')).toBeVisible();
  });

  it('restores manually entered Contract Step values from the saved draft', async () => {
    draftStorageMocks.loadProjectRegistrationDraft.mockResolvedValueOnce({
      version: 1,
      currentStep: 3,
      equipment: [flowEquipment],
      agreementFormValues: {},
      contractFormValues: {
        businessName: '入力済み株式会社',
        representativeName: '入力済み代表者',
        representativePosition: '入力済み役職',
        contractAgreed: true,
      },
      finalTermsValues: {},
      updatedAt: '2026-08-18T02:00:00.000Z',
    });

    renderFlow();

    await screen.findByText('持続可能性フォーム');
    expect(screen.getByTestId('contract-business-name')).toHaveTextContent(
      '入力済み株式会社',
    );
    expect(screen.getByTestId('contract-representative-name')).toHaveTextContent(
      '入力済み代表者',
    );
    expect(
      screen.getByTestId('contract-representative-position'),
    ).toHaveTextContent('入力済み役職');
    expect(screen.getByTestId('contract-agreed')).toHaveTextContent('true');
  });

  it('shows a loading state instead of static terms placeholders', async () => {
    projectApiMocks.getProjectMemberTermsInfo.mockReturnValueOnce(
      new Promise(() => undefined),
    );
    draftStorageMocks.loadProjectRegistrationDraft.mockResolvedValueOnce({
      version: 1,
      currentStep: 3,
      equipment: [flowEquipment],
      agreementFormValues: {},
      contractFormValues: {},
      finalTermsValues: {},
      updatedAt: '2026-09-03T02:00:00.000Z',
    });

    renderFlow();

    expect(
      await screen.findByRole('status', {
        name: '入会規約情報を読み込んでいます',
      }),
    ).toBeVisible();
    expect(document.body).not.toHaveTextContent('プログラム型運営・管理者名');
    expect(document.body).not.toHaveTextContent('プロジェクト概要');
    expect(document.body).not.toHaveTextContent('YYYY年MM月DD');
  });

  it('blocks only a dependent step and retries member terms loading', async () => {
    const user = userEvent.setup();
    projectApiMocks.getProjectMemberTermsInfo.mockRejectedValueOnce(
      new Error('入会規約情報の取得に失敗しました'),
    );
    draftStorageMocks.loadProjectRegistrationDraft.mockResolvedValueOnce({
      version: 1,
      currentStep: 3,
      equipment: [flowEquipment],
      agreementFormValues: {},
      contractFormValues: {},
      finalTermsValues: {},
      updatedAt: '2026-09-03T02:00:00.000Z',
    });

    renderFlow();

    expect(
      await screen.findByText('入会規約情報の取得に失敗しました'),
    ).toBeVisible();
    expect(screen.queryByText('持続可能性フォーム')).not.toBeInTheDocument();

    projectApiMocks.getProjectMemberTermsInfo.mockResolvedValueOnce({
      statusCode: 'OK',
      message: '',
      projectName: '再取得プロジェクト',
      operatorNameJa: '再取得運用者',
      adminRepName: '再取得代表者',
      projectOverview: '再取得概要',
      usePurpose: '再取得利用目的',
      startedAt: '2026-09-04T00:00:00.000Z',
    });
    await user.click(screen.getByRole('button', { name: '再読み込み' }));

    expect(await screen.findByText('持続可能性フォーム')).toBeVisible();
    expect(screen.getByTestId('contract-operator-name')).toHaveTextContent(
      '再取得運用者・再取得代表者',
    );
    expect(projectApiMocks.getProjectMemberTermsInfo).toHaveBeenCalledTimes(2);
  });

  it('persists intentionally cleared Contract Step fields in the draft', async () => {
    const user = userEvent.setup();
    renderFlow();

    await user.click(await screen.findByRole('button', { name: '次へ' }));
    await user.click(screen.getByRole('button', { name: '次へ' }));
    await user.click(
      await screen.findByRole('button', { name: '契約情報を空にする' }),
    );

    await waitFor(() => {
      expect(draftStorageMocks.saveProjectRegistrationDraft).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          contractFormValues: {
            businessName: '',
            representativeName: '',
            representativePosition: '',
            contractAgreed: false,
          },
        }),
      );
    });
  });
});
