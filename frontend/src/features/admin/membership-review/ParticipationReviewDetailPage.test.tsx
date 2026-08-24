import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mapParticipationReviewDetail } from '@/api/admin/participationReviewMapper';
import ParticipationReviewDetailPage from './ParticipationReviewDetailPage';

const {
  approveParticipationReview,
  getAdminProjectDetail,
  getMyParticipationReviews,
  getParticipationReviewDetail,
  rejectParticipationReview,
} = vi.hoisted(() => ({
  approveParticipationReview: vi.fn(),
  getAdminProjectDetail: vi.fn(),
  getMyParticipationReviews: vi.fn(),
  getParticipationReviewDetail: vi.fn(),
  rejectParticipationReview: vi.fn(),
}));

vi.mock('@/api/admin', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/admin')>();
  return {
    ...actual,
    approveParticipationReview,
    getAdminProjectDetail,
    getMyParticipationReviews,
    getParticipationReviewDetail,
    rejectParticipationReview,
  };
});

const reviewDetail = mapParticipationReviewDetail({
  participationId: 12,
  siteId: 3,
  projectId: 7,
  projectName: 'プロジェクトA',
  siteName: '拠点A',
  companyName: '会社A',
  submittedAt: '2026-07-30T06:50:55.732Z',
  equipment: {
    id: 9,
    siteId: '3',
    equipmentOpDate: '2026-07-01',
    nominalMaxOutput: 50,
    pcsManufacturer: 'PCSメーカー',
    pcsModel: 'PCS-01',
    pcsUnitCount: 2,
    deviceNo: 'DEVICE-1',
    pcsStandby: 1,
    hasBattery: false,
    totalInvestment: 1000000,
    hasSubsidy: false,
    preProjectUnitPrice: 20,
    evidenceDocuments: [],
    files: [],
  },
  confirmationItems: {},
  sustainabilityCommitment: {},
  review: {
    isSolarContractDoc: false,
    isPcsSpecDoc: false,
    isBatterySpecDoc: false,
    isOperationStartDoc: false,
    isGenerationDataDoc: false,
    isReversePowerDoc: false,
    isInvestmentProofDoc: false,
    isApplicationCompleteCheck: false,
    isFormsAttachCheck: false,
    isEmissionsReductionActivityListCheck: false,
    isOriginalSpecCheck: false,
    isSubsidyCheck: false,
    primaryApplReviewerName: '',
    secondaryApplReviewerName: '',
  },
});

function renderPage() {
  return render(
    <MemoryRouter
      initialEntries={['/admin/projects/7/memberships/12']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <Routes>
        <Route
          path="/admin/projects/:projectId/memberships/:participationId"
          element={<ParticipationReviewDetailPage />}
        />
        <Route
          path="/admin/projects/:projectId/memberships"
          element={<div>list page</div>}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ParticipationReviewDetailPage', () => {
  beforeEach(() => {
    approveParticipationReview.mockReset();
    getAdminProjectDetail.mockReset();
    getMyParticipationReviews.mockReset();
    getParticipationReviewDetail.mockReset();
    rejectParticipationReview.mockReset();
    getAdminProjectDetail.mockResolvedValue({
      id: '7',
      projectName: 'プロジェクトA',
    });
    getParticipationReviewDetail.mockResolvedValue(reviewDetail);
    getMyParticipationReviews.mockResolvedValue([reviewDetail]);
    approveParticipationReview.mockResolvedValue(undefined);
    rejectParticipationReview.mockResolvedValue(undefined);
  });

  it('enables approval only after reviewer inkan and every checklist item are set', async () => {
    const user = userEvent.setup();
    renderPage();

    expect(
      await screen.findByRole('heading', { level: 1, name: '入会届の詳細' }),
    ).toBeInTheDocument();

    const approveButton = screen.getByRole('button', { name: '承認' });
    expect(approveButton).toBeDisabled();

    await user.type(
      screen.getByRole('textbox', { name: /レビュー担当者印/ }),
      '山田',
    );
    for (const checkbox of screen.getAllByRole('checkbox')) {
      await user.click(checkbox);
    }

    expect(approveButton).toBeEnabled();
    await user.click(approveButton);
    await user.click(screen.getByRole('button', { name: '承認する' }));

    expect(approveParticipationReview).toHaveBeenCalledWith(
      12,
      expect.objectContaining({
        reviewerInkan: '山田',
        isSolarContractDoc: true,
        isSubsidyCheck: true,
      }),
    );
    expect(
      approveParticipationReview.mock.calls[0]?.[1],
    ).not.toHaveProperty('rejectReason');
  });

  it('starts every reviewable item with an unchecked checklist', async () => {
    getParticipationReviewDetail.mockResolvedValue({
      ...reviewDetail,
      review: {
        ...reviewDetail.review,
        isSolarContractDoc: true,
        isPcsSpecDoc: true,
        isBatterySpecDoc: true,
        isOperationStartDoc: true,
        isGenerationDataDoc: true,
        isReversePowerDoc: true,
        isInvestmentProofDoc: true,
        isApplicationCompleteCheck: true,
        isFormsAttachCheck: true,
        isEmissionsReductionActivityListCheck: true,
        isOriginalSpecCheck: true,
        isSubsidyCheck: true,
      },
    });

    renderPage();

    expect(
      await screen.findByRole('heading', { level: 1, name: '入会届の詳細' }),
    ).toBeInTheDocument();
    expect(
      screen.getAllByRole('checkbox').every((checkbox) => !checkbox.hasAttribute('data-state') || checkbox.getAttribute('data-state') === 'unchecked'),
    ).toBe(true);
  });

  it('renders a non-reviewable pending item as read-only', async () => {
    getMyParticipationReviews.mockResolvedValue([]);
    renderPage();

    expect(
      await screen.findByText(
        'この入会届は現在あなたのレビュー対象ではないため、閲覧のみ可能です。',
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('textbox', { name: /レビュー担当者印/ }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '承認' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '却下' })).not.toBeInTheDocument();
    expect(screen.getAllByRole('checkbox').every((item) => item.hasAttribute('disabled'))).toBe(
      true,
    );
  });
});
