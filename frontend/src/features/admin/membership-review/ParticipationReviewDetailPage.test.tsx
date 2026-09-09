import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/_shared/errors';
import { mapParticipationReviewDetail } from '@/api/admin/participationReviewMapper';
import ParticipationReviewDetailPage from './ParticipationReviewDetailPage';

const {
  approveParticipationReview,
  getAdminProjectDetail,
  getMyParticipationReviews,
  getParticipationReviewDetail,
  rejectParticipationReview,
  downloadProjectMembershipPdf,
  downloadFromUrl,
  toast,
} = vi.hoisted(() => ({
  approveParticipationReview: vi.fn(),
  getAdminProjectDetail: vi.fn(),
  getMyParticipationReviews: vi.fn(),
  getParticipationReviewDetail: vi.fn(),
  rejectParticipationReview: vi.fn(),
  downloadProjectMembershipPdf: vi.fn(),
  downloadFromUrl: vi.fn(),
  toast: vi.fn(),
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

vi.mock('@/api/projects', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/projects')>();
  return {
    ...actual,
    downloadProjectMembershipPdf,
  };
});

vi.mock('@/utils/download', () => ({ downloadFromUrl }));

vi.mock('@/hooks/use-toast', () => ({
  useToast: () => ({ toast }),
}));

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

const approvedReviewDetail = {
  ...reviewDetail,
  review: {
    ...reviewDetail.review,
    primaryApplReviewerName: '一次確認者',
    secondaryApplReviewerName: '二次確認者',
  },
};

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
    downloadProjectMembershipPdf.mockReset();
    downloadFromUrl.mockReset();
    toast.mockReset();
    getAdminProjectDetail.mockResolvedValue({
      id: '7',
      projectName: 'プロジェクトA',
    });
    getParticipationReviewDetail.mockResolvedValue(reviewDetail);
    getMyParticipationReviews.mockResolvedValue([reviewDetail]);
    approveParticipationReview.mockResolvedValue(undefined);
    rejectParticipationReview.mockResolvedValue(undefined);
    downloadProjectMembershipPdf.mockResolvedValue({
      pdfUrl: 'https://example.com/membership.pdf',
      expiresIn: 300,
    });
  });

  it('opens the approval form and enables submission only after every required field is set', async () => {
    const user = userEvent.setup();
    renderPage();

    expect(
      await screen.findByRole('heading', { level: 1, name: '入会届の詳細' }),
    ).toBeInTheDocument();

    const approveButton = screen.getByRole('button', { name: '承認' });
    expect(approveButton).toBeEnabled();
    expect(
      screen.queryByRole('heading', { name: 'レビューチェックリスト' }),
    ).not.toBeInTheDocument();

    await user.click(approveButton);

    const confirmButton = screen.getByRole('button', { name: '承認する' });
    expect(confirmButton).toBeDisabled();

    await user.type(
      screen.getByRole('textbox', { name: /レビュー担当者印/ }),
      '山田',
    );
    for (const checkbox of screen.getAllByRole('checkbox')) {
      await user.click(checkbox);
    }

    expect(confirmButton).toBeEnabled();
    await user.click(confirmButton);

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

  it('starts every decision dialog with an unchecked checklist', async () => {
    const user = userEvent.setup();
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
    expect(screen.queryAllByRole('checkbox')).toHaveLength(0);

    await user.click(screen.getByRole('button', { name: '承認' }));

    expect(
      screen
        .getAllByRole('checkbox')
        .every(
          (checkbox) =>
            !checkbox.hasAttribute('data-state') ||
            checkbox.getAttribute('data-state') === 'unchecked',
        ),
    ).toBe(true);
  });

  it('marks every checklist item as required', async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByRole('heading', { level: 1, name: '入会届の詳細' });
    await user.click(screen.getByRole('button', { name: '承認' }));

    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes).toHaveLength(12);
    for (const checkbox of checkboxes) {
      expect(checkbox).toHaveAttribute('aria-required', 'true');
      const checkboxId = checkbox.getAttribute('id');
      const label = document.querySelector(`label[for="${checkboxId}"]`);
      expect(label).toHaveTextContent('*');
    }
  });

  it('gets the membership PDF and opens its temporary URL', async () => {
    const user = userEvent.setup();
    getParticipationReviewDetail.mockResolvedValue(approvedReviewDetail);
    getMyParticipationReviews.mockResolvedValue([]);
    renderPage();

    await screen.findByRole('heading', { level: 1, name: '入会届の詳細' });
    await user.click(screen.getByRole('button', { name: 'PDFを表示' }));

    expect(downloadProjectMembershipPdf).toHaveBeenCalledWith('7');
    expect(downloadFromUrl).toHaveBeenCalledWith(
      'https://example.com/membership.pdf',
    );
  });

  it('does not show the PDF after only the primary approval', async () => {
    getParticipationReviewDetail.mockResolvedValue({
      ...reviewDetail,
      review: {
        ...reviewDetail.review,
        primaryApplReviewerName: '一次確認者',
        secondaryApplReviewerName: '',
      },
    });
    renderPage();

    await screen.findByRole('heading', { level: 1, name: '入会届の詳細' });

    expect(
      screen.queryByRole('button', { name: 'PDFを表示' }),
    ).not.toBeInTheDocument();
  });

  it('keeps the page available and shows a toast when the PDF cannot be opened', async () => {
    const user = userEvent.setup();
    getParticipationReviewDetail.mockResolvedValue(approvedReviewDetail);
    getMyParticipationReviews.mockResolvedValue([]);
    downloadProjectMembershipPdf.mockRejectedValue(
      new Error('PDFがまだ作成されていません。'),
    );
    renderPage();

    await screen.findByRole('heading', { level: 1, name: '入会届の詳細' });
    await user.click(screen.getByRole('button', { name: 'PDFを表示' }));

    expect(toast).toHaveBeenCalledWith({
      title: 'PDFを表示できませんでした',
      description: 'PDFがまだ作成されていません。',
      variant: 'destructive',
    });
    expect(
      screen.getByRole('heading', { level: 1, name: '入会届の詳細' }),
    ).toBeInTheDocument();
  });

  it('requires a complete rejection form and preserves its draft after closing', async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByRole('heading', { level: 1, name: '入会届の詳細' });
    await user.click(screen.getByRole('button', { name: '却下' }));

    const rejectButton = screen.getByRole('button', { name: '却下する' });
    expect(rejectButton).toBeDisabled();

    await user.type(
      screen.getByRole('textbox', { name: /レビュー担当者印/ }),
      '山田',
    );
    await user.type(
      screen.getByRole('textbox', { name: /却下理由/ }),
      '資料に不備があります。',
    );
    for (const checkbox of screen.getAllByRole('checkbox')) {
      await user.click(checkbox);
    }

    expect(rejectButton).toBeEnabled();
    await user.click(screen.getByRole('button', { name: 'キャンセル' }));

    await waitFor(() => {
      expect(
        screen.queryByRole('button', { name: '却下する' }),
      ).not.toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: '却下' }));
    expect(
      screen.getByRole('textbox', { name: /レビュー担当者印/ }),
    ).toHaveValue('山田');
    expect(screen.getByRole('textbox', { name: /却下理由/ })).toHaveValue(
      '資料に不備があります。',
    );
    expect(
      screen
        .getAllByRole('checkbox')
        .every((checkbox) => checkbox.getAttribute('data-state') === 'checked'),
    ).toBe(true);

    await user.click(screen.getByRole('button', { name: '却下する' }));

    expect(rejectParticipationReview).toHaveBeenCalledWith(
      12,
      expect.objectContaining({
        reviewerInkan: '山田',
        rejectReason: '資料に不備があります。',
        isSolarContractDoc: true,
        isSubsidyCheck: true,
      }),
    );
  });

  it('keeps the decision dialog open and displays backend field errors', async () => {
    const user = userEvent.setup();
    approveParticipationReview.mockRejectedValue(
      new ApiError('入力内容に不備があります。', {
        status: 400,
        payload: {
          statusCode: 'VALIDATION_ERROR',
          message: '入力内容に不備があります。',
          errors: {
            reviewerInkan: 'レビュー担当者印を確認してください。',
          },
        },
      }),
    );
    renderPage();

    await screen.findByRole('heading', { level: 1, name: '入会届の詳細' });
    await user.click(screen.getByRole('button', { name: '承認' }));
    await user.type(
      screen.getByRole('textbox', { name: /レビュー担当者印/ }),
      '山田',
    );
    for (const checkbox of screen.getAllByRole('checkbox')) {
      await user.click(checkbox);
    }
    await user.click(screen.getByRole('button', { name: '承認する' }));

    expect(
      await screen.findByText('レビュー担当者印を確認してください。'),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: '入会届を承認しますか？' }),
    ).toBeInTheDocument();
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
    expect(screen.queryAllByRole('checkbox')).toHaveLength(0);
    expect(
      screen.queryByRole('button', { name: 'PDFを表示' }),
    ).not.toBeInTheDocument();
  });
});
